interface ElevatorState{

    void nextState(ElevatorStateMachine context);
    /**
     * Displays the current state information
     */
    void displayState();

    void entry(ElevatorStateMachine context);

    void exit(ElevatorStateMachine context);

    String getClassName();

}

/**
 Classes for Doors Opening / Closing States. [COMPLETE]
 */

class DoorsClosed implements ElevatorState {

    public void displayState(){
        System.out.println("Current state: Doors Closed");
    }

    public void nextState(ElevatorStateMachine context) {

        if(!context.runFlag){return;}
        context.setState("WaitingForScheduler");
    }

    public void entry(ElevatorStateMachine context){
        System.out.println("Elevator "+context.getId()+": Doors closed on floor: " +context.getCurrentFloor());
        context.stateFlag = true;
    }
    public void exit(ElevatorStateMachine context){
        ;
    }
    public String getClassName(){return "DoorsClosed";}
}



class DoorsOpen implements ElevatorState{

    public void nextState(ElevatorStateMachine context){
        if(!context.runFlag){return;}
        context.setState(("DoorsClosed"));}

    public void displayState(){
        System.out.println("Current State: Doors Open");
    }

    public void entry(ElevatorStateMachine context){
        context.faultState = this;
        boolean flag = true;
        System.out.println("Elevator "+context.getId()+": Doors opened on floor: " +context.getCurrentFloor());
        //Turn floor lamp of the current floor off (since current floor has been set by now)
        //This function also calls a message to client that confirms the elevator reached the floor.
        Trip currentTrip;
        for(int i = 0; i < context.trips.size(); i++){
            currentTrip = context.trips.get(i);
            //Conditions for soft-fault: We're at the destination & the fault code is 1.
            if (currentTrip.tripType == 1 && (currentTrip.getDestinationFloor() == context.getCurrentFloor()) && (currentTrip.msg.fault.getFaultCode() == 1)){
                flag = false;
                //System.out.println("Soft fault code activated");
            }
        }
        //If the flag for a fault is set, the trip never finishes because the floor is stuck.
        if(flag){
            context.removeDestination(context.getCurrentFloor());
            context.stateFlag = true;
        }

    }
    public void exit(ElevatorStateMachine context){
        //System.out.println("Elevator "+context.getId()+": Closing doors on floor"+context.getCurrentFloor());
        try{
            Thread.sleep(Configuration.ELEVATOR_DOORS_OPENING_CLOSING);
        }catch(InterruptedException e){};
    }
    public String getClassName(){return "DoorsOpen";}


}


/**
 * Classes for Communication with Scheduler.
 */

//Initial State
class WaitingForScheduler implements ElevatorState{


    public void nextState(ElevatorStateMachine context) {
        if(!context.runFlag){return;}
        context.setState("ElevatorMoving");
    }

    //may require a function to get context from the scheduler
    public void entry(ElevatorStateMachine context){
        boolean printFlag = true;
        while(true){
            if(!context.trips.isEmpty()){
                //If there is a trip, then we should move on. No more actions should be taken here, they're all managed elsewhere.
                context.stateFlag = true;
                break;
            }
            //If there's no trips, just sit and wait. Sleep is to prevent busy-waiting.
            context.setDirection(Direction.IDLE);
            try{
                Thread.sleep(1000);
            }
            catch(InterruptedException e){};
        }
    }
    public void exit(ElevatorStateMachine context){
        //Update floor lamp(s) with desired floors found in instructions.
    }
    public void displayState(){System.out.println("Current State: Waiting For Scheduler");}

    public String getClassName(){return "WaitingForScheduler";}
}


/**
 * Classes for Elevator Moving
 */

class ElevatorMoving implements ElevatorState{

    //Timer required here to demonstrate time before elevator reaches destination
    public void nextState(ElevatorStateMachine context){
        if(!context.runFlag){return;}
        context.restartTimer();
        context.setState("ElevatorStopped");
    }

    public void displayState(){System.out.println("Current State: Elevator Moving");}

    /**
     * Increments the floor in its set direction until the end is reached, or a serviceable trip is found.
     * Assumes trips aren't empty that is checked within the 'WaitingForScheduler' State.
     * Sleeps between floors moving.
     * @param context
     */
    public void entry(ElevatorStateMachine context){
        context.faultState = this;
        if (!context.runFlag) {
            return;
        }
        System.out.println("Elevator "+context.getId() +": Started moving on floor: "+context.getCurrentFloor() + " ("+context.getDirection()+")");
        System.out.println("Elevator " +context.getId()+" servicing trips: ");
        for(int i = 0; i < context.trips.size(); i++){
            System.out.print("("+context.trips.get(i).msg.currentFloor+", "+ context.trips.get(i).msg.direction+", "+context.trips.get(i).msg.carButton+", Type: "+context.trips.get(i).tripType+"), ");
        }
        System.out.println();
        Trip currentTrip;
        synchronized (context) {
            while (true) {
                if(!context.runFlag){
                    return;
                }
                for (int i = 0; i < context.trips.size(); i++) {
                    if(!context.runFlag){
                        return;
                    }


                    currentTrip = context.trips.get(i);
                    //If the current floor is a destination, we'll always stop there.
                    if (currentTrip.tripType == 1 && currentTrip.msg.carButton == context.getCurrentFloor()) {
                        //Hard fault: The elevator doesn't detect the arrival.
                        if (currentTrip.msg.fault.getFaultCode() == 2){
                            //System.out.println("Hard fault code triggered HERE");
                            return;
                        }
                        System.out.println("Stopped for trip " + currentTrip.toString());
                        System.out.println("Elevator " + context.getId() + ": Stopping at floor " + context.getCurrentFloor() + ": Destination reached.");
                        context.stateFlag = true;
                        return;
                    }
                    //If the current floor is a request, we should only go to it if the elevator is moving the same direction as the trip
                    // OR if it's the furthest trip in the current direction.
                    else if (context.getCurrentFloor() == currentTrip.getDestinationFloor()) {
                        //System.out.println(currentTrip.getDestinationFloor() + " " + currentTrip.msg.currentFloor);
                        if (currentTrip.msg.direction.equals(context.getDirection().name())) {
                            System.out.println("Elevator " + context.getId() + ": Stopping at floor " + context.getCurrentFloor());
                            context.stateFlag = true;
                            return;
                        }
                        else if (context.getDirection().equals(Direction.IDLE)){
                            System.out.println("Elevator "+context.getId()+": Already on the requested floor, picking up.");
                            context.stateFlag = true;
                            return;
                        }
                        else {
                            //checks whether the current floor is the furthest trip in the current direction.
                            int compare = 0;
                            for (int k = 0; k < context.trips.size(); k++) {
                                if (context.getDirection().equals(Direction.UP) && (context.trips.get(k).getDestinationFloor() > compare)) {
                                    compare = context.trips.get(k).getDestinationFloor();
                                } else if (context.getDirection().equals(Direction.DOWN) && (context.trips.get(k).getDestinationFloor() < compare)) {
                                    compare = context.trips.get(k).getDestinationFloor();
                                }

                                if (compare == currentTrip.getDestinationFloor()) {
                                    System.out.println("Elevator " + context.getId() + ": Stopping at floor " + context.getCurrentFloor());
                                    context.stateFlag = true;
                                    return;
                                }


                            }
                        }

                    }


                }

            //Otherwise, we pass by the floor
            context.incrementFloor();
            //Sleeping for # of seconds = elevator_movement config variable
            try {
                Thread.sleep(Configuration.ELEVATOR_MOVEMENT * 1000);
            } catch (InterruptedException e) {}

            }
        }
//

    }
    public void exit(ElevatorStateMachine context){
        ; //none
    }

    public String getClassName(){return "ElevatorMoving";}
}


class ElevatorStopped implements ElevatorState{

    public void nextState(ElevatorStateMachine context){
        //We'd set a timer here if we wanted to implement doors opening/closing timers.
        context.setState("DoorsOpen");}
    public void displayState(){System.out.println("Current State: Elevator Stopped");}

    public void entry(ElevatorStateMachine context){
        //System.out.println("Elevator "+context.getId() + ": Stopped on floor "+context.getCurrentFloor());
        context.stateFlag = true;
    }
    public void exit(ElevatorStateMachine context){
        //System.out.println("Elevator "+context.getId()+": Doors opening on floor"+context.getCurrentFloor());
        try{
            Thread.sleep(Configuration.ELEVATOR_DOORS_OPENING_CLOSING);
        }catch(InterruptedException e){};

    }
    public String getClassName(){return "ElevatorStopped";}
}

class Faulted implements ElevatorState{

    public void nextState(ElevatorStateMachine context){

        if(!context.runFlag){return;}
        context.setState("ElevatorMoving");
    }

    public void entry(ElevatorStateMachine context){
        switch(context.faultState.getClassName()){
            case "DoorsOpen":
                System.out.println("Elevator " + context.getId() + ": Fault Detected [Door Stuck]. Resolving.");
                context.restartTimer();
                context.removeDestination(context.getCurrentFloor());
                context.setState("DoorsOpen");
                context.stateFlag = true;
                context.nextState();
                context.restartTimer();
                return;
            case "ElevatorMoving":
                System.out.println("Elevator "+context.getId()+" Fault Detected [ArrivalSensor Issue] Shutting Down");
                context.shutOffElevator();
                return;
            default:
                System.out.println("ERROR: No explicit fault detected");
        }

    }
    public void exit(ElevatorStateMachine context){
        System.out.println("Elevator + "+context.getId()+" : FAULT HANDLED");
    }

    public String getClassName(){return "Faulted";}

    public void displayState(){System.out.println("Current State: Faulted");}
}
