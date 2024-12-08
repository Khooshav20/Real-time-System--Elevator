**SYSC3303 Project - Real Time Concurrent System **

-> Elevator Control System

Group # ,

Members:

David Sedarous,
Ralph Joseph,
Emmanuel Adekoya,
Renya,
Khooshav Bundhoo,

------------------------------------------------------------------------------------------------------------------------------------------------------------------
Iteration 4:

Responsibilities:

David:
- Fixed Iteration 3 Code:
    > Fixed the floor subsystem to not get stuck waiting for responses
    	- Changed sendReceiveSocket to send and receive sockets, receiveSocket has a timeout,
    	- Changed the while loop to run indefinitely until the program is terminated.
    	- Added a flag to avoid excess printing of waiting messages.

    > Changed Input file and Time management so that elevator requests are relative to the current time.
    	- Changed java.Time in SystemMsg to LocalTime using LocalTime.now()
    	- added a totalDelay that is cumulative, and added delay in seconds to each SystemMsg.
    	- This is done so that the Floor checks if the current time is greater than the SystemMSg time before sending it (Better)
    	- Uses a delay variable in the Configuration to determine the delay between messages in the queue.

    > Fixed the ElevatorClient:
        - Now uses a DataType (Trip) to track ongoing trips, whether they're destinations or requests, and remove the trips after they're completed.
        - Altered the ElevatorClient code to manage concurrent trips, allowing for multiple elevators to complete trips
        -
- Iteration 4 Hard Fault
    > Created the Hard Fault Sequence Diagram
    > Implemented Hard Faults
        - Each Trip corresponding to a trip has a timer that's incremented every second
        - ElevatorClient increments each timer every second, compares to a Configuration variable for maximum trip time.
        - If any trip is over the maximum trip time, a hard fault is assumed and the elevator is shut down.
        - Elevators are sent the fault-code from the input, and are informed to not take the trip if a hard fault input is detected.
        - All trips from a shut-down elevator are removed.

Renua:
> Tests
Kooshav:
> Updated the FloorSubsystem to accept system faults within input.txt.
> Created a system-fault class to allow flexible usage of system-faults.

Emmanuel & Ralph:
> Soft-Faults







**Project Iteration 1: Create the elevator class, floor class, floor system class, scheduler class **

The purpose of Iteration 1 is to models the operation of an elevator, handling floor requests, moving between floors, and scheduling these tasks using the 3 Subsystems.

Files:

Elevator.java:

Serves as the representation of the Elevator subsystem.
Scheduler.java:

Oversees communication between the Elevator and Floor subsystems.
Floor.java:

Acts as the representation of the Floor subsystem, responsible for reading input data and facilitating communication with the Schedule
SystemMsg.java:

Represents messages exchanged between the Scheduler and the various subsystems.
Main.java:

Main class to initialize and start the Elevator, Floor, and Scheduler threads.
Diagrams (located in the "diagrams" subfolder):

UML class diagram for Elevator.java, Floor.java, Scheduler.java and SystemMsg.java UML sequence diagram

Responsibilities: (Iteration 1)
David Sedarous : Designed and Updated UML Class and UML Sequence diagrams.

Ralph Joseph: Implemented the Floor, Time and SystemMsg class

Emmanuel Adekoya: Implemented the Scheduler and SystemMsg class.

Khooshav Bundhoo (101132063) : Created Elevator Class and modifications in the main.java

Renua: Created the JUnit tests for Elevator, Scheduler, and Floor classes.

Notes:
Requires JUnit 5 in order to run tests and main. JUnit tests must be run individually as methods - NOT as a test suite.

Project Iteration 2: Adding the Scheduler and Elevator Subsystems

The purpose of Iteration 2 is to implement State Machines for the Scheduler and Elevator Subsystems and assume that only 1 elevator is present. In addition, the Elevator Subsystem will notify the scheduler when an elevator reaches the floor

Files:

Elevator.java:

Serves as the representation of the Elevator subsystem.
Scheduler.java:

Oversees communication between the Elevator and Floor subsystems.
Floor.java:

Acts as the representation of the Floor subsystem, responsible for reading input data and facilitating communication with the Schedule
SystemMsg.java:

Represents messages exchanged between the Scheduler and the various subsystems.
ElevatorStateMachine.java:

Time.Java:

Main.java:

Main class to initialize and start the Elevator, Floor, and Scheduler threads.
TESTING

To run the unit tests for the Test class: Add 'JUnit5.8.1' to class path

ElevatorTest.java

FloorSubsystemTest.java

SchedulerTest.java

SystemMsgTest.java

TimeTest.java

Diagrams (located in the "diagrams" subfolder):

UML state diagram UML class diagram for Elevator.java, Floor.java, Scheduler.java and SystemMsg.java UML sequence diagram

Responsibilities: (Iteration 2)
David & Renua: - Create the Elevator subclass state machine & implemented the state machine.

Ralph & Emmanuel: - Create the Scheduler subclass state machine.

Khooshav: Diagrams

UML class diagram
UML sequence diagram
State diagrams
Readme.txt
Overview This Java project simulates an elevator system with three main subsystems: Elevator, Floor, and Scheduler. The Elevator subsystem manages the movement of elevators within a building, the Floor subsystem reads input data from a file representing passenger floor requests, and the Scheduler facilitates communication between the Elevator and Floor subsystems.

Iteration 3 Enhancements

In Iteration 3, the project implements Remote Procedure Calls (RPCs) over UDP to enhance communication efficiency between the main threads of the Elevator and Floor subsystems. Specifically, the Elevator subsystem handles four separate elevators within a 22-floor building. It receives scheduled requests from the Scheduler and distributes them to the respective elevators for efficient passenger transportation.

Subsystems

Elevator
Files: Elevator.java, ElevatorClient.java, ElevatorStateMachine.java
Description: The Elevator subsystem manages the movement and operation of individual elevators. It listens for instructions from the Scheduler and executes them accordingly. Each elevator is represented by an instance of the Elevator class, which communicates with the main system via UDP.
Floor
Files: Floor.java, Direction.java, FloorSubsystem.java, FloorSubsystemTest.java
Description: The Floor subsystem represents the different floors in the building and handles passenger requests. It reads input data from a file, representing passenger floor requests, and sends them to the Scheduler for scheduling. Each floor is managed by an instance of the Floor class.
Scheduler
Files: Scheduler.java, SharedDataInterface.java
Description: The Scheduler acts as a mediator between the Elevator and Floor subsystems. It manages the communication between them and schedules passenger requests to optimize elevator usage. The Scheduler utilizes Remote Procedure Calls (RPCs) over UDP for efficient communication.
Testing

Files: SystemMsgTest.java, TimeTest.java
Description: Unit tests ensure the correctness and robustness of the system components. SystemMsgTest.java tests the functionality of the SystemMsg class, while TimeTest.java verifies the behavior of the Time class.
Configuration

Edit Configuration.java to adjust parameters such as elevator timing, number of floors, and IP addresses.
Responsibilities: (Iteration 3)
David:

ElevatorStateMachine
ElevatorClient (UDP)
Renua:

UDP connectivity
Ralph & Emmanuel:

Scheduler
Khooshav: Diagrams

implement configuration class
modified elevatorStateMachine to take the configuration class
implement FloorSubsystem
Readme.txt
SETUP:

Install IntelliJ IDEA
Open IntelliJ IDEA and choose Get from Version control
In the URL section, paste this link: https://github.com/SharpieRalph/sysc3303_project.git
Choose your directory and then click on clone
To run the unit tests for the Test class Iteration3Test: Add 'JUnit5.8.1' to class path
Once done, You are good to go!!
