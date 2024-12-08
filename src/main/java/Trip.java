public class Trip {
    public int tripType;
    public SystemMsg msg;

    public int elevatorID;

    public Trip(int tripType, SystemMsg msg){
        this.tripType = tripType;
        this.msg = msg;
    }

    public int getDestinationFloor(){
        if(tripType == 1){return msg.carButton;}
        return msg.currentFloor;
    }

    public String toString(){
        return "(Elevator: "+elevatorID+", TT: "+tripType+", CF: "+msg.currentFloor+", DE: "+msg.carButton+")";
    }

}
