/**
 * The SimpleEchoClient class represents a simple client application that sends
 * and receives messages using UDP and interacts with shared data through Java RMI.
 * Each client runs as a separate thread and communicates with a shared server.
 *
 * @author Dr. Rami Sabouni,
 * Systems and Computer Engineering,
 * Carleton University
 * @version 1.0, March 11, 2024
 */

import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ElevatorClient {
    public Map<Integer, ElevatorStateMachine> elevators = new HashMap<>();
    LinkedList<Trip> trips;
    private DatagramPacket sendPacket;
    private GUI gui;

    private DatagramPacket receivePacket;
    private DatagramSocket sendReceiveSocket;
    public ElevatorClient() {
        trips = new LinkedList<>();
        try{
            sendReceiveSocket = new DatagramSocket(Configuration.ELEVATOR_SUBSYSTEM_SCHEDULER_PORT);
            sendReceiveSocket.setSoTimeout(1000);

        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        gui = new GUI();

       for (int i = 0; i < Configuration.NUM_ELEVATORS; i++) {
            //Elevator elevator = new Elevator(i);
            ElevatorStateMachine elevator = new ElevatorStateMachine(this, i);
           elevator.addObserver(gui);
            elevators.put(i, elevator);
            new Thread(elevator, "ELEVATOR_"+i).start();
        }
    }



    /**
     * Finds the elevator closest to the requested floor, that is moving in the requested direction.
     * This should only be called for request trips (tripType 0 in Trips class), as for deliveries the same elevator must be used.
     * @param msg is the SystemMsg containing trip information
     * @return
     */
    public int findSuitableElevator(SystemMsg msg){
        String direction = msg.direction;
        int floorNumber = msg.currentFloor;
        int destinationNumber = msg.carButton;
        //serviceDirection is the direction the elevator's trips are moving (Not the way the elevator is moving)
        String serviceDirection;
        //current is a short-cut for getting the elevator at each phase in the loop
        ElevatorStateMachine current;
        Trip firstTrip;
        boolean printflag = true;
        while(true){

            //Determine most suitable elevator.
            for (int i = 0; i < elevators.size(); i++){
                current = elevators.get(i);
                if (current == null){
                    continue;
                }

                if(current.trips.size() >= Configuration.CAPACITY_LIMIT){
                    System.out.println("ElevatorClient: Capacity limit for Elevator "+current.getId()+" reached. Finding another elevator if available");
                    continue;
                }

                //If they're servicing the same direction
                //Move to the next elevator if this one is idle/has no trips
                try{
                    firstTrip = current.trips.getFirst();
                    serviceDirection = current.trips.getFirst().msg.direction;
                } catch(NoSuchElementException e){continue;}

                if(serviceDirection.equals(direction)){
                    //If the elevator hasn't passed the requested floor in the direction of travel already, it's suitable:
                    //Not doing == because it'll probably pass by the req. by the time it gets it, since it's moving.
                    if(current.getDirection().name().toUpperCase().equals("UP") && (firstTrip.getDestinationFloor() > floorNumber) && (current.getCurrentFloor() <= floorNumber)){return i;}
                    if(current.getDirection().name().toUpperCase().equals("DOWN") && (firstTrip.getDestinationFloor() < floorNumber) && (current.getCurrentFloor() >= floorNumber)){return i;}
                    //if it has, this elevator isn't suitable.
                }
                //If the elevator direction is opposite the service direction, it's moving towards the first trip.
                //If this trip is moving in that direction and will finish before the direction changes, it can be added.
                if (direction.equals(current.getDirection().name().toUpperCase())){
                    //Conditions: Direction | firstTrip goes further or equal the pickup floor | firstTrip goes further than this trip

                    if(direction.toUpperCase().equals("UP") && (current.getCurrentFloor() <= floorNumber) && (firstTrip.msg.carButton >= destinationNumber)){
                        return i;
                    }

                    else if (direction.toUpperCase().equals("DOWN") &&  (current.getCurrentFloor() >= floorNumber) && (firstTrip.msg.carButton <= destinationNumber)){
                    return i;
                    }
                    //Debugging message:
                    // System.out.println("Failed pickup : "+firstTrip.getDestinationFloor()+" >= "+floorNumber+" , "+destinationNumber);
                }



            }
            //No suitable moving elevators - find a new, idle elevator.
            for (int i = 0; i < elevators.size(); i++){
                current = elevators.get(i);
                if(current == null){continue;}
                if(current.getDirection() == Direction.IDLE){
                    return i;
                }
            }
            //If there isn't a suitable elevator, wait half a second before trying again.

            try{
                if(printflag){
                    System.out.println("ElevatorClient: Currently no suitable elevators for this call, waiting");
                    printflag = false;
                }
                TimeUnit.MILLISECONDS.sleep(500);
            }catch(InterruptedException e){};

            //here
        }

    }

    public Trip receiveFromScheduler() {
        byte data[] = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        //System.out.println("ElevatorClient: Waiting for Packet.\n");

        try {
            sendReceiveSocket.receive(receivePacket);
        }catch(SocketTimeoutException e){
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // Extract the ID
        int id = data[0];


        SystemMsg msg = null;
        try {
            // Deserialize the SystemMsg, skipping the first byte
            ByteArrayInputStream in = new ByteArrayInputStream(data, 1, receivePacket.getLength() - 1);
            ObjectInputStream is = new ObjectInputStream(in);
            msg = (SystemMsg) is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("ElevatorClient: Received info from Scheduler, msg ID: "+id+", MSG: " + msg);

        return new Trip(id, msg);
    }


    public void sendConfirmationToScheduler(Trip trip) {
        // Construct the confirmation message
        int floor = trip.msg.currentFloor;
        if(trip.tripType == 1){floor = trip.msg.carButton;}
        String message = trip.tripType +  " Elevator " + trip.elevatorID + " has reached floor " + floor;

        try {
            byte[] messageBytes = message.getBytes();
            InetAddress schedulerAddress = InetAddress.getByName(Configuration.SCHEDULER_IP);
            int schedulerPort = Configuration.FLOOR_SUBSYSTEM_SCHEDULER_PORT; // Assuming this is the port Scheduler listens on

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, schedulerAddress, schedulerPort);
            sendReceiveSocket.send(packet);

        }catch(SocketTimeoutException e){
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Determines the direction of travel for an elevator after a new trip is added
     * Determined by current direction & distance to the furthest trip
     * @param elevatorID is the elevator ID for client's array of elevators.
     */
    public void changeElevatorDirection(int elevatorID){
        //System.out.println("ElevatorClient: Changing direction of Elevator "+elevatorID);
        ElevatorStateMachine elevator = elevators.get(elevatorID);
        switch(elevator.getDirection()){
            case UP:
                for(int i = 0; i < elevator.trips.size(); i++){
                    if((elevator.trips.get(i).tripType == 1 && elevator.getCurrentFloor() < elevator.trips.get(i).msg.carButton) || ( elevator.trips.get(i).tripType == 0 && elevator.getCurrentFloor() < elevator.trips.get(i).msg.currentFloor)){
                        return;
                    }
                }
                elevator.setDirection(Direction.DOWN);
                break;

            case DOWN:
                for(int i = 0; i < elevator.trips.size(); i++){
                    if((elevator.trips.get(i).tripType == 1 && elevator.getCurrentFloor() > elevator.trips.get(i).msg.carButton) || ( elevator.trips.get(i).tripType == 0 && elevator.getCurrentFloor() > elevator.trips.get(i).msg.currentFloor)){
                        return;
                    }
                }
                elevator.setDirection(Direction.UP);
                break;

                //If idle, need to determine the necessary direction using relative position
                //Idle should only occur when the elevator had no trips previously, so we can compare to the type and floor of the first trip
            case IDLE:
                Trip trip;
                try{
                    trip = elevator.trips.get(0);
                }catch(IndexOutOfBoundsException e){System.out.println("ElevatorClient: Left Elevator "+elevatorID+" idle, no trips"); return;}

                int floor;
                if(trip.tripType == 0){
                    floor = trip.msg.currentFloor;
                }
                else{
                    floor = trip.msg.carButton;
                }
                //The elevatorMoving state accounts for cases where they're on the same floor - resolves before moving.
                if (elevator.getCurrentFloor() < floor){
                    elevator.setDirection(Direction.UP);
                }
                else if (elevator.getCurrentFloor() > floor){
                    elevator.setDirection(Direction.DOWN);
                }

                //Case for when elevator is 'sent' to a floor it's already on: remain idle.
                //The moving state should figure out what to do.

                break;
        }
        System.out.println("ElevatorClient: Changed direction of Elevator " + elevatorID + " to "+elevators.get(elevatorID).getDirection().name());
    }

    public static void main(String[] args) {
        ElevatorClient client = new ElevatorClient();
        System.out.println("Elevators Starting at: "+ LocalTime.now());
        while (true) {
            // Step 1: Receive a message from the scheduler (requesting elevator to move to a floor)

            //System.out.println("SIZE : "+client.elevators.size());
            Trip trip = client.receiveFromScheduler();
            if (trip == null){

                continue;
            }
            System.out.println("Floor button pressed at floor " +trip.msg.currentFloor + " going " + trip.msg.direction);


            SystemMsg msg = trip.msg;
            int msgid = trip.elevatorID;
            // Step 2: Find the suitable elevator and send it to the requested floor
            int id = client.findSuitableElevator(msg);
            ElevatorStateMachine selectedElevator = client.elevators.get(id);
            int arrowDirection = trip.msg.direction.equals("up") ? 0 : 1;
            client.elevators.get(id).notifyFloorButton(trip.msg.currentFloor, arrowDirection  , true);

            trip.elevatorID = id;
            client.trips.add(trip);
            if(msgid == 1){
                //System.out.println("Floor button on floor " + trip.msg.currentFloor + " turned off");
                System.out.println("ElevatorClient: Sending Elevator "+selectedElevator.getId()+" to floor "+msg.carButton+" from floor "+selectedElevator.getCurrentFloor());
                selectedElevator.addDestination(trip);


            }
            else{
                selectedElevator.addDestination(trip);
                System.out.println("ElevatorClient: Sending Elevator "+selectedElevator.getId()+" to floor "+msg.currentFloor+" from floor "+selectedElevator.getCurrentFloor());
            }
            client.changeElevatorDirection(id);
            try{
                TimeUnit.SECONDS.sleep(1);
                //client.incrementTimer();
            }catch(InterruptedException e){

            }

        }
    }



}
