import java.io.Serializable;
import java.time.LocalTime;

public class SystemMsg implements Serializable {
    public LocalTime time;
    public String direction;
    public int currentFloor;
    public int carButton;
    public SystemFault fault;

    public SystemMsg() {
    }

    public SystemMsg(LocalTime time, String direction, int currentFloor, int carButton, SystemFault fault){
        this.time = time;
        this.direction = direction;
        this.currentFloor = currentFloor;
        this.carButton = carButton;
        this.fault = fault;
    }

    public String toString(){
        return time + " " + direction + " " + currentFloor + " "  + carButton + " " + fault;
    }
}
