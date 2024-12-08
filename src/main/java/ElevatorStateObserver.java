public interface ElevatorStateObserver {
    void updateFloor(int elevatorId, int newFloor);
    void updateState(int elevatorId, String newState);
    void isFaulted(int elevatorId, int faultType);
    void updateElevatorLamp(int elevatorId, int floorId, boolean lit);
    void lightupFloorButtons(int floorId, int arrow, boolean lit);
}
