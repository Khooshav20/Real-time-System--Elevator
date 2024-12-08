public enum SystemFault {
    NO_ERROR(0),
    DOOR_STUCK(1),
    ELEVATOR_STUCK(2);

    private final int faultCode;

    SystemFault(final int faultCode) {
        this.faultCode = faultCode;
    }

    public int getFaultCode() {
        return this.faultCode;
    }

    public static SystemFault fromFaultCode(int faultCode) {
        for (SystemFault fault : SystemFault.values()) {
            if (fault.getFaultCode() == faultCode) {
                return fault;
            }
        }
        throw new IllegalArgumentException("Unknown fault code: " + faultCode);
    }
}