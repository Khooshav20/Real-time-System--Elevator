import java.util.HashMap;
import java.util.Map;

public class Floor {

    private final int floorNumber;
    private final Map<Direction, Boolean> floorLamps;
    private final Map<Direction, Boolean>[] directionLamps;

    public Floor(int floorNumber, boolean isFirst, boolean isLast) {
        this.floorNumber = floorNumber;
        floorLamps = new HashMap<>();
        directionLamps = new HashMap[Configuration.NUM_ELEVATORS];

        // Initialize all lamps to OFF
        if (!isLast) {
            floorLamps.put(Direction.UP, Boolean.FALSE);
        }

        if (!isFirst) {
            floorLamps.put(Direction.DOWN, Boolean.FALSE);
        }

        for (int i = 0; i < Configuration.NUM_ELEVATORS; i++) {
            directionLamps[i] = new HashMap<>();

            if (!isLast) {
                directionLamps[i].put(Direction.UP, false);
            }

            if (!isFirst) {
                directionLamps[i].put(Direction.DOWN, false);
            }
        }
    }

}