import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FaultTimerTask extends TimerTask {

    private final ElevatorStateMachine elevator;
    public FaultTimerTask(ElevatorStateMachine elevator){
        this.elevator = elevator;
    }

    @Override
    public void run() {
        try{
            TimeUnit.SECONDS.sleep(Configuration.ELEVATOR_TIMEOUT);
            if(elevator.trips.isEmpty()){return;}
            //System.out.println("TimeOut - Fault detected");
            elevator.handleFault();

        }catch(InterruptedException ignored){

        }
    }

}
