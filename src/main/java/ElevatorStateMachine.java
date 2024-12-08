
import java.time.LocalTime;
import java.util.*;


/**
 * State Machine Class & Main Methods
 */
public class ElevatorStateMachine implements Runnable{
    //Hashmap containing the elevator's potential states.
    private final Map<String, ElevatorState> states;
    private ElevatorState currentState;
    public ElevatorLamp[] lamps;
    private Direction direction;

    private int currentFloor;
    //client: The ElevatorClient that manages this elevator.
    private final ElevatorClient client;
    //id: The elevator's ID
    private final int id;

    //stateFlag: determines whether the state is ready to progress in run()
    public boolean stateFlag;

    LinkedList<Trip> trips;
    public final Timer timer;

    public ElevatorState faultState;
    public boolean runFlag;

    private List<ElevatorStateObserver> observers = new ArrayList<>();






    public ElevatorStateMachine(ElevatorClient client, int id){
        //System.out.println("--------------------\nTime per floor: "+Configuration.ELEVATOR_TIMING+" seconds\n# of floors: "+Configuration.NUM_FLOORS+"\n--------------------");
        runFlag = true;
        timer = new Timer(true);
        this.client = client;
        this.stateFlag = true;
        this.id = id;
        lamps = new ElevatorLamp[Configuration.NUM_FLOORS];
        //Setting a lamp for each floor.
        for(int i = 0; i < lamps.length; i++){
            lamps[i] = new ElevatorLamp(i);
        }
        //Attributes
        states = new HashMap<>();
        direction = Direction.IDLE;
        //currentFloor set to 1 by default, maybe change this after.
        currentFloor = 1;

        //Create a Thread-safe collection of lists.
        trips = new LinkedList<>();

        //All the states to be added to the hashmap (5)
        addState("DoorsClosed", new DoorsClosed());
        addState("DoorsOpen", new DoorsOpen());

        addState("ElevatorMoving", new ElevatorMoving());
        addState("ElevatorStopped", new ElevatorStopped());

        addState("WaitingForScheduler", new WaitingForScheduler());
        addState("Faulted", new Faulted());

        //Initial State:
        this.setState("DoorsClosed");

    }

    public void addObserver(ElevatorStateObserver observer) {
        observers.add(observer);
    }
    public void notifyFloorButton(int floor, int dir, boolean lit){
        for (ElevatorStateObserver observer : observers) {
            observer.lightupFloorButtons(floor, dir, lit );
        }
    }

    private void notifyFloorChange() {
        for (ElevatorStateObserver observer : observers) {
            observer.updateFloor(this.id, this.currentFloor);
        }
    }

    private void notifyStateChange() {
        for (ElevatorStateObserver observer : observers) {
            observer.updateState(this.id, this.currentState.getClassName());
        }
    }

    private void notifyFault(int fault){
        for (ElevatorStateObserver observer : observers) {
            observer.isFaulted(this.id , fault);
        }
    }




    private void notifyElevatorLampChange(int floorId, boolean lit) {
        for (ElevatorStateObserver observer : observers) {
            observer.updateElevatorLamp(this.id, floorId, lit);
        }
    }

    /**
     * Transitions the Elevator to the next state.
     * First checks if the elevator is set to shut down.
     * Activates the exit/entry actions for the current/next state accordingly.
     */
    public void nextState(){
        if(Thread.currentThread().isInterrupted()){return;}
        this.stateFlag = false;
        currentState.exit(this);
        //System.out.println("-------------------------------------------------------");
        //System.out.println("\nState before: "+currentState.getClassName());
        currentState.nextState(this);
        //System.out.println("Elevator "+this.id+": Changed state to "+currentState.getClassName());
        currentState.entry(this);
        //System.out.println("State after: "+currentState.getClassName());
        //System.out.println("-------------------------------------------------------");
        notifyStateChange();
    }

    /**
     * Adds a trip to the Elevator's list of trips.
     * Lights the corresponding lamp if the trip is passenger's destination (Type 1).
     * @param trip the Trip type to be added.
     */
    public void addDestination(Trip trip){
        //currently doesn't change the direction at all, direction managed by scheduler.
        synchronized (trips){
            this.trips.add(trip);
            //If the trip is a destination (not a request), turn on the lamp
            if(trip.elevatorID == 1){
                lamps[trip.msg.carButton].turnOn();
            }
            timer.schedule(new FaultTimerTask(this), 0);
        }
    }

    /**
     * Purges the timer's current time-out, and starts a new one.
     * Used to re-start the timer whenever a destination is removed.
     * Checks whether there are any trips before re-starting timer, otherwise remains dormant.
     */
    public void restartTimer(){
        //System.out.println("Elevator "+id+": Restarting timer ");
        timer.purge();
        if(!trips.isEmpty()){
            timer.schedule(new FaultTimerTask(this), 0);
        }
    }
    public void shutOffElevator(){
        System.out.println("Elevator " + id + ": Experienced [HARD FAULT]");
        notifyFault(0);
        Thread.currentThread().interrupt();
        //System.out.println("ELEVATOR "+id + " " +currentState.getClassName());
        stateFlag = false;
        runFlag = false;
        System.out.println("Elevator "+id+" Finished Running at time:"+ LocalTime.now());
        timer.purge();
        client.elevators.remove(this.id);
        Thread.currentThread().interrupt();

    }

    public void handleFault(){
        this.currentState = getState("Faulted");
        notifyFault(1);
        currentState.entry(this);

    }


    /**
     * Checks for trips that end on the floor we stopped, and sends confirmation to the scheduler that the trip has completed
     * Checks all trips in case of multiple completed trips on the same floor.
     * @param floor is the floor on which the elevator stopped.
     */
    public void removeDestination(int floor){
        lamps[floor].turnOff();
        notifyElevatorLampChange(floor, false);
        Trip trip;
        synchronized (this){
            int index = 0;
            while(true){
                if(trips.isEmpty()){
                    setDirection(Direction.IDLE);
                    break;
                }
                trip = trips.get(index);
                if(trip.tripType == 0 && trip.msg.currentFloor == floor){
                    restartTimer();
                    System.out.println("Floor button on floor " + currentFloor + " turned off.");
                    int arrowDirection = trip.msg.direction.equals("up") ? 0 : 1;
                    client.elevators.get(id).notifyFloorButton(currentFloor,  arrowDirection, false);
                    //remove the trip, send an update to the scheduler.
                    client.sendConfirmationToScheduler(trip);
                    //Change the trip type to 'destination'
                    trips.get(index).tripType = 1;
                    lamps[trip.msg.carButton].turnOn();
                    notifyElevatorLampChange(trip.msg.carButton , true);
                }
                else if (trip.tripType == 1 && trip.msg.carButton == floor){
                    restartTimer();
                    notifyElevatorLampChange(trip.msg.carButton , false);
                    client.sendConfirmationToScheduler(trip);
                    System.out.println("Elevator "+id+": Finished trip: "+trip.toString()+" at time: "+ LocalTime.now());
                    trips.remove(trip);
                    index = 0;
                    continue;
                }
                index++;
                if (index == trips.size()){break;}
            }


        }
        client.changeElevatorDirection(id);

    }

    /**
     * Increments the Elevator's currentFloor variable in its given direction.
     * Prints an error message if the end of the floor is reached.
     * This should not occur as the scheduler manages the direction of the elevators so that the floor limit isn't passed.
     * Only instance in which this error should occur is if the input.txt file specifies a floor greater or less than the floor range set in Configuration.
     */
    public void incrementFloor(){
        //System.out.println("ELEVATOR "+id+": MOVING IN DIRECTION: " +direction.name());
        if(direction.equals(Direction.UP) && (currentFloor < Configuration.NUM_FLOORS)){
            this.currentFloor++;
            notifyFloorChange();
            return;
        }
        else if(direction.equals(Direction.DOWN) && (currentFloor > 1)){
            this.currentFloor--;
            notifyFloorChange();
            return;
        }
        System.out.println("Elevator "+id+": End of floor reached" + currentFloor + ", becoming IDLE (Shouldn't occur)");
        direction = Direction.IDLE;
    }

    public int getCurrentFloor(){return this.currentFloor;}
    public int getId(){return this.id;}
    public void setCurrentFloor(int newFloor){this.currentFloor = newFloor;}
    public void printCurrentFloor(){System.out.println("Elevator: "+id+": On floor " + currentFloor); }

    public void addState(String stateName, ElevatorState state){this.states.put(stateName, state);}

    public void setState(String stateName){this.currentState = getState(stateName);}

    public ElevatorState getState(String stateName){return states.get(stateName);}

    public Direction getDirection(){return this.direction;}
    public void setDirection(Direction dir){this.direction = dir;}

    public ElevatorState getCurrentState() {
        return currentState;
    }



    /**
     * Run method for the thread, checks whether the next state should be activated using stateFlag.
     * Sleeps if there are no trips to avoid excessive busy-waiting in loop.
     */
    public void run(){

        System.out.println("Elevator "+this.id+": Running");
        try{
            while(runFlag) {
                System.out.print("");
                if (this.stateFlag && runFlag) {
                    //System.out.println("Next state");
                    this.nextState();
                }
                //If the elevator has nothing to do, it should go to sleep to avoid busy-waiting. Losing a second on servicing an elevator is fine.
                //We want to skip this if the elevator is trying to transition between states.
                else if (this.trips.isEmpty() && runFlag) {
                    Thread.sleep(1000);

                }
            }
        }catch(InterruptedException ignored){}


    }
        //Currently only occurs during hard-fault.


}
