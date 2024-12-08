import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

public class Scheduler implements Runnable {
    private DatagramSocket socket;
    private DatagramSocket sendSocket;
    private  SystemMsg msg;

    public Scheduler() throws Exception {
        this.socket = new DatagramSocket(Configuration.FLOOR_SUBSYSTEM_SCHEDULER_PORT);
        this.sendSocket = new DatagramSocket();  // Socket for sending messages
        this.msg = null;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024]; // Adjust the buffer size if necessary

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);

                // Determine the source of the message
                if (packet.getPort() == Configuration.FLOOR_SUBSYSTEM_SEND_PORT) {
                    // Message received from FloorSubsystem
                    processFloorMessage(packet);
                } else if (packet.getPort() == Configuration.ELEVATOR_SUBSYSTEM_SCHEDULER_PORT) {
                    // Message received from ElevatorClient
                    processElevatorMessage(packet);
                } else {
                    // Received from an unknown source
                    System.out.println("Received message from unknown source.");
                }
            } catch (Exception e) {
                System.out.println("Exception in Scheduler run method.");
                e.printStackTrace();
            }
        }
    }

    public void processFloorMessage(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte messageType = data[0];

        try {
            // Determine the message type based on the first byte
            if (messageType == 0) {
                // It's an elevator request
                System.out.println("Elevator request received from floor subsystem.");

                // Deserialize the SystemMsg object, skipping the first byte
                ByteArrayInputStream byteStream = new ByteArrayInputStream(data, 1, packet.getLength() - 1);
                ObjectInputStream is = new ObjectInputStream(byteStream);
                SystemMsg receivedMsg = (SystemMsg) is.readObject();
                System.out.println(receivedMsg.time + ", elevator needed at floor " + receivedMsg.currentFloor + ", sending msg to elevator.");
                msg = receivedMsg;

                // After processing, send the message to the elevator subsystem
                sendSystemMsgToElevator(receivedMsg, (byte) 0);

                is.close();
                byteStream.close();
            } else if (messageType == 1) {
                // It's a destination message
                System.out.println("Destination message received from floor subsystem.");

                // Deserialize the SystemMsg object, skipping the first byte
                ByteArrayInputStream byteStream = new ByteArrayInputStream(data, 1, packet.getLength() - 1);
                ObjectInputStream is = new ObjectInputStream(byteStream);
                SystemMsg receivedMsg = (SystemMsg) is.readObject();
                System.out.println("Destination message details: " + receivedMsg.carButton);

                sendSystemMsgToElevator(receivedMsg, (byte) 1);

                is.close();
                byteStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception while processing floor message.");
            e.printStackTrace();
        }
    }

    public void processElevatorMessage(DatagramPacket packet) {


        byte[] data = packet.getData();
        int length = packet.getLength();

        String check = new String(data, 0, 1, StandardCharsets.UTF_8);
        System.out.println("HERE: "+check);
        if (check.equals("1")){
            System.out.println("Destination reached, stopping.");
            return;
        }
        try {
            String receivedMessage = new String(data, 2, length, StandardCharsets.UTF_8);
            System.out.println("Message received from ElevatorClient: " + receivedMessage);

            // Process the received string message

        } catch (Exception e) {
            System.out.println("Exception while processing elevator message.");
            e.printStackTrace();
        }
        System.out.println("Requesting destination to floor ");
        System.out.println(msg);
        //sendConfirmationToFloor(msg);
    }



    public void sendSystemMsgToElevator(SystemMsg msg, byte id) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byteStream.write(id);
            ObjectOutputStream os = new ObjectOutputStream(byteStream);
            os.writeObject(msg);
            os.flush();

            byte[] sendData = byteStream.toByteArray();

            // Create and send the packet to the elevator subsystem
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, InetAddress.getByName(Configuration.ELEVATOR_SUBSYSTEM_IP), Configuration.ELEVATOR_SUBSYSTEM_SCHEDULER_PORT
            );
            sendSocket.send(sendPacket);

            os.close();
            byteStream.close();

        } catch (Exception e) {
            System.out.println("Exception in sending SystemMsg to the elevator subsystem.");
            e.printStackTrace();
        }
    }

    public void sendConfirmationToFloor(SystemMsg message) {
        try {
            // Serialize the SystemMsg object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();

            byte[] messageBytes = baos.toByteArray();
            InetAddress floorAddress = InetAddress.getByName(Configuration.FLOOR_SUBSYSTEM_IP);
            int floorPort = Configuration.FLOOR_SUBSYSTEM_RECEIVE_PORT; // Ensure this port is set correctly in Configuration

            DatagramPacket confirmationPacket = new DatagramPacket(messageBytes, messageBytes.length, floorAddress, floorPort);
            sendSocket.send(confirmationPacket);  // Use sendSocket for sending messages

            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try {
            Scheduler scheduler = new Scheduler();
            new Thread(scheduler).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
