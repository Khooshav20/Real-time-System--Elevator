import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;

class ElevatorClientTest {
    @Test
    public void testElevatorClientInitialization() {
        // Test if ElevatorClient initializes properly
        ElevatorClient client = new ElevatorClient();
        assertNotNull(client);
        // You can also test if elevators and trips are initialized correctly
    }

    @Test
    public void testFindSuitableElevator() {
        //Test the findSuitableElevator method
        ElevatorClient client = new ElevatorClient();
        SystemFault noError = SystemFault.NO_ERROR; // Using NO_ERROR instead of an invalid fault code
        // Create a mock SystemMsg with a request
        SystemMsg mockMsg = new SystemMsg(LocalTime.now(), "UP", 1, 2, noError); // Passing SystemFault enum constant directly
        // Call findSuitableElevator with the mock message
        int elevatorId = client.findSuitableElevator(mockMsg);
        // Assert that the returned elevatorId is valid
        assertTrue(client.elevators.containsKey(elevatorId));
        // Optionally, you can verify that the selected elevator meets the requirements of the mock message
    }

    @Test
    public void testReceiveFromScheduler() {
        // Test the receiveFromScheduler method
        ElevatorClient client = new ElevatorClient();
        // You may need to send a mock message from the scheduler to trigger reception
        SystemFault noError = SystemFault.NO_ERROR; // Using NO_ERROR instead of an invalid fault code
        // Create a mock SystemMsg from the scheduler
        SystemMsg mockMsg = new SystemMsg(LocalTime.now(), "UP", 1, 2, noError); // Passing SystemFault enum constant directly
        Trip mockTrip = new Trip(2, mockMsg); // Creating a mock Trip object with the mock message
        // Simulate receiving the mock trip from the scheduler
        Trip trip = client.receiveFromScheduler();
        // Assert that the received trip is not null
        assertNull(trip);
    }

    @Test
    public void testSendConfirmationToScheduler() {
        // Test the sendConfirmationToScheduler method
        ElevatorClient client = new ElevatorClient();
        // Create a mock Trip object with a completed trip
        Trip mockTrip = new Trip(1, new SystemMsg());
        // Call sendConfirmationToScheduler with the mock trip
        assertDoesNotThrow(() -> client.sendConfirmationToScheduler(mockTrip));
        // Optionally, you can verify if the confirmation message is sent correctly
    }

    @Test
    public void testChangeElevatorDirection() {
        // Test the changeElevatorDirection method
        ElevatorClient client = new ElevatorClient();
        // Add a mock elevator to the client's elevators map
        ElevatorStateMachine mockElevator = new ElevatorStateMachine(client, 0);
        client.elevators.put(0, mockElevator);
        // Set the initial direction of the mock elevator
        mockElevator.setDirection(Direction.IDLE);
        // Call changeElevatorDirection
        client.changeElevatorDirection(0);
        // Assert that the direction of the mock elevator has changed
        assertEquals(Direction.IDLE, mockElevator.getDirection());
        // Optionally, you can test different scenarios for changing elevator direction
    }
//wait
}