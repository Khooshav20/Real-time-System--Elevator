public class ElevatorLamp{
    private boolean lit;
    public int floorNumber;

    public ElevatorLamp(int floorNumber){
        lit = false;
        this.floorNumber = floorNumber;
    }

    public void turnOn(){lit = true;}
    public void turnOff(){lit = false;}
    public boolean getLit(){return lit;}
}
