import java.io.*;
import java.net.*;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class FloorSubsystem {

    private ArrayList<Floor> floors;

    private Queue<SystemMsg> eventsQueue;

    //private DatagramSocket sendReceiveSocket;

    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;


    //printFlag used to prevent loop-printing of waiting messages in receive(): true will print, false won't.
    private boolean printFlag;

    private int totalDelay;




    public FloorSubsystem() {
        totalDelay = 0;
        printFlag = true;
        eventsQueue = new LinkedList<>();
        floors = new ArrayList<>();
        for (int i = 0; i < Configuration.NUM_FLOORS; i++) {
            floors.add(new Floor(i, i == 1, i == Configuration.NUM_FLOORS -1));
        }

        try {

            sendSocket = new DatagramSocket(Configuration.FLOOR_SUBSYSTEM_SEND_PORT);
            receiveSocket = new DatagramSocket(Configuration.FLOOR_SUBSYSTEM_RECEIVE_PORT);
            //sendReceiveSocket = new DatagramSocket(Configuration.FLOOR_SUBSYSTEM_PORT);
            //Split into 2 sockets: sendSocket should not time out or else we might skip requests.
            //receiveSocket must time out, or else we'll get stuck waiting for the elevator to arrive, not sending any more messages.
            receiveSocket.setSoTimeout(Configuration.FLOOR_SOCKET_TIMEOUT);
        } catch (SocketException e) {
            System.out.println("Socket creation failed due to an error: ");
            e.printStackTrace();
            System.exit(1);
        }

        readFromInputFile();
        //While will run until program is stopped, needs to constantly be checking for messages from the scheduler.
        while(true) {
            if(!eventsQueue.isEmpty()){
                //First check if the SystemMsg time has occurred.
                if(LocalTime.now().isAfter(eventsQueue.peek().time)){
                    System.out.println("Sending a message to the scheduler:");
                    sendMessage(eventsQueue.poll());
                }


            }
            //Here, we only send more events to the scheduler after we have received a result.
            //We need a timeout for receive, where we'll check for another input. - David
            //receive();
        }
    }

    private void readFromInputFile() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Configuration.FLOOR_INPUT_FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                eventsQueue.add(getSystemMsgFromInputLine(line));
            }
            reader.close();
        } catch (IOException e) {
        }
    }

    public SystemMsg getSystemMsgFromInputLine(String line) {
        String[] input = line.split(" ");
        String[] timeStr = input[0].split(":");

//        int hours = Integer.parseInt(timeStr[0]);
//        int minutes = Integer.parseInt(timeStr[1]);
//        float seconds = Float.parseFloat(timeStr[2]);
        //Time time = new Time(hours, minutes, seconds);

        //Updated time so that it gets the current time,and adds some delay to it (a few seconds) - David
        //Gets the current time, and adds a delay of a few seconds.
        int delay = (int)(Math.random() * Configuration.FLOOR_INPUT_RANDOM_DELAY) + totalDelay;
        totalDelay += delay; //Ensures that messages are processed in order
        LocalTime lTime = LocalTime.now().plusSeconds(delay);
        int pickupFloor = Integer.parseInt(input[1]);
        int destinationFloor = Integer.parseInt(input[3]);
        String direction = input[2];
        SystemFault fault = SystemFault.fromFaultCode(Integer.parseInt(input[4]));

        return new SystemMsg(lTime, direction, pickupFloor, destinationFloor, fault);
    }

    public void sendMessage(SystemMsg event) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(event);
            oos.flush();
            oos.close();
            baos.close();
            byte[] data = baos.toByteArray();

            // Prepend a 0 byte for request messages
            byte[] requestData = new byte[data.length + 1];
            requestData[0] = 0; // 0 indicating a request
            System.arraycopy(data, 0, requestData, 1, data.length);

            send(requestData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void process(DatagramPacket packet) {
        if (packet.getLength() == 0) {
            return;
        }

        // Assuming packet contains the serialized SystemMsg object
        byte[] data = packet.getData();



        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            // Deserialize the object
            SystemMsg receivedMsg = (SystemMsg) ois.readObject();

            // Print the deserialized SystemMsg
            System.out.println("Received SystemMsg: " + receivedMsg.toString());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("An error occurred while deserializing SystemMsg:");
            e.printStackTrace();
        }

        // Prepend a 1 byte for response messages
        byte[] responseData = new byte[packet.getData().length + 1];
        responseData[0] = 1; // 1 indicating that we are sending a destination
        System.arraycopy(packet.getData(), 0, responseData, 1, packet.getData().length);

        try {
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(Configuration.SCHEDULER_IP), Configuration.FLOOR_SUBSYSTEM_SCHEDULER_PORT);
            sendSocket.send(responsePacket);

        } catch (IOException e) {
            System.out.println("Error sending the destination ");
            e.printStackTrace();
            // Handle the exception
        }

    }


    private void send(byte[] data) {
        byte[] buffer = new byte[1024];
        DatagramPacket sendPacket = null;
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

        try {
            sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName(Configuration.SCHEDULER_IP), Configuration.FLOOR_SUBSYSTEM_SCHEDULER_PORT);
            sendSocket.send(sendPacket);
        } catch (IOException e) {
            System.out.println("Socket timed out: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void receive() {
        byte[] buffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);


        try {
            if(printFlag){
                System.out.println("Requesting an elevator from scheduler");
            }

            receiveSocket.receive(receivePacket);
            process(receivePacket);
        }catch (SocketTimeoutException e){
            if(printFlag){
                System.out.println("Timeout before receiving message, checking for further requests.");
                printFlag = false;
            }
            return;
        } catch (IOException e) {
            System.out.println("Error receiving packet: ");
            e.printStackTrace();
        }

        System.out.println("Passenger enters the elevator");
        printFlag = true;
    }







    public Queue<SystemMsg> getEventsQueue() {
        return eventsQueue;
    }

    public static void main(String[] args) {
        new FloorSubsystem();
    }
}