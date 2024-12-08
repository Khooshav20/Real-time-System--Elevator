public class Configuration {
    public static final double ELEVATOR_TIMING = 1.0;
    public static final int NUM_FLOORS = 22;
    public static final int NUM_ELEVATORS = 4;
    public static final String FLOOR_INPUT_FILE = "src/main/input/input.txt";
    public static final String SCHEDULER_IP = "127.0.0.1";
    public static final String FLOOR_SUBSYSTEM_IP = "127.0.0.1";
    public static final String ELEVATOR_SUBSYSTEM_IP = "127.0.0.1";
    public static final int FLOOR_SUBSYSTEM_SCHEDULER_PORT = 23;
    public static final int ELEVATOR_SUBSYSTEM_SCHEDULER_PORT = 69;
    public static final int FLOOR_SUBSYSTEM_SEND_PORT = 25;

    public static final int FLOOR_SUBSYSTEM_RECEIVE_PORT = 80;
    public static final int ELEVATOR_DOORS_OPENING_CLOSING = 2000;
    public static final int ELEVATOR_MOVEMENT = 2;
    public static final int FLOOR_SOCKET_TIMEOUT = 500;

    public static final int FLOOR_INPUT_RANDOM_DELAY = 10;

    public static final int CAPACITY_LIMIT = 3;

    //For 22 floors and 2 seconds movement, 44seconds + 10 for safety.
    public static final int ELEVATOR_TIMEOUT = (ELEVATOR_MOVEMENT * NUM_FLOORS) + (5 * ELEVATOR_MOVEMENT);

}
