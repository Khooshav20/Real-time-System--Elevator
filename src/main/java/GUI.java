import javax.swing.*;
import java.awt.*;

public class GUI implements ElevatorStateObserver {
    private JPanel panel;
    private JFrame frame;
//    private JMenuBar menuBar;
    private JPanel floorPanel;
    private JTextArea console;

    private JPanel[] elevatorPanels;
    private JLabel[][] elevatorLabels;
    private JLabel[][] elevatorLampPanels;
    // Initialize your arrow tracking array
    JLabel[][] upArrows;
    JLabel[][] downArrows;


    public GUI() {
        frame = new JFrame("Elevator System");
        panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        elevatorPanels = new JPanel[Configuration.NUM_ELEVATORS];
        elevatorLabels = new JLabel[Configuration.NUM_ELEVATORS][2];
        elevatorLampPanels = new JLabel[Configuration.NUM_FLOORS][Configuration.NUM_ELEVATORS];
        upArrows = new JLabel[Configuration.NUM_FLOORS][1];
        downArrows = new JLabel[Configuration.NUM_FLOORS][1];


        createElevatorDisplay();
        createLightDisplay();
        createMessageDisplay();
//        createMenu();

        // Configure frame
        frame.add(panel);
//        frame.setJMenuBar(menuBar);
        frame.setSize(1500, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

//    public void createMenu() {
//        menuBar = new JMenuBar();
//        JMenu simulation = new JMenu("Simulation");
//        JMenuItem addIssue = new JMenuItem("Add Issue");
//        JMenuItem addRequest = new JMenuItem("Add Request");
//        simulation.add(addIssue);
//        simulation.add(addRequest);
//        menuBar.add(simulation);
//    }

    private void createElevatorDisplay() {
        Font font = new Font("Roboto", Font.PLAIN, 16);
        for (int i = 0; i < Configuration.NUM_ELEVATORS; i++) {
            elevatorPanels[i] = new JPanel();
            elevatorPanels[i].setName("Elevator " + i);
            elevatorPanels[i].setLayout(new GridLayout(Configuration.NUM_ELEVATORS, 1));
            elevatorPanels[i].setBackground(Color.LIGHT_GRAY);
            elevatorPanels[i].setOpaque(true);
            elevatorPanels[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel nameLabel = new JLabel("Elevator " + i, SwingConstants.CENTER);
            nameLabel.setFont(font);
            elevatorPanels[i].add(nameLabel);

            // Label for current floor
            JLabel floorLabel = new JLabel("Current Floor: " + 0, SwingConstants.CENTER);
            floorLabel.setFont(font);
            elevatorPanels[i].add(floorLabel);
            elevatorLabels[i][0] = floorLabel; // Store the reference for later updates

            // Label for elevator state
            JLabel stateLabel = new JLabel("Elevator State: Idle", SwingConstants.CENTER);
            stateLabel.setFont(font);
            elevatorPanels[i].add(stateLabel);
            elevatorLabels[i][1] = stateLabel; // Store the reference for later updates

            panel.add(elevatorPanels[i]);
        }
    }

    public void createLightDisplay() {
        floorPanel = new JPanel();
        // Adjust the grid layout: Add 2 instead of 1 for the two arrow columns (up and down)
        floorPanel.setLayout(new GridLayout(Configuration.NUM_FLOORS + 1, Configuration.NUM_ELEVATORS + 4));

        // Header for floors
        floorPanel.add(new JLabel("Floor", SwingConstants.CENTER));
        // Headers for the arrow columns
        floorPanel.add(new JLabel("Up", SwingConstants.CENTER));
        floorPanel.add(new JLabel("Down", SwingConstants.CENTER));

        // Headers for elevators
        for (int i = 0; i < Configuration.NUM_ELEVATORS; i++) {
            floorPanel.add(new JLabel("Elevator " + i + " Lamp", SwingConstants.CENTER));
        }



        // Add floor numbers, arrows, and elevator lamps
        for (int i = 0; i < Configuration.NUM_FLOORS; i++) {
            // Floor number
            floorPanel.add(new JLabel(String.valueOf(i), SwingConstants.CENTER));

            // Up arrow
            JLabel upArrow = new JLabel("<html><font size='5'>↑</font></html>", SwingConstants.CENTER);
            floorPanel.add(upArrow);
            upArrows[i][0] = upArrow;

            // Down arrow
            JLabel downArrow = new JLabel("<html><font size='5'>↓</font></html>", SwingConstants.CENTER);
            floorPanel.add(downArrow);
            downArrows[i][0] = downArrow;

            // Elevator lamp status
            for (int j = 0; j < Configuration.NUM_ELEVATORS; j++) {
                JLabel label = new JLabel("OFF", SwingConstants.CENTER);
                label.setBackground(Color.LIGHT_GRAY);
                label.setOpaque(true);
                floorPanel.add(label);
                elevatorLampPanels[i][j] = label;
            }
        }

        // Add the floor panel to the main panel
        panel.add(floorPanel);
    }


    public void createMessageDisplay() {
        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(console);
        panel.add(scrollPane);
    }

    @Override
    public void updateFloor(int elevatorId, int newFloor) {
        SwingUtilities.invokeLater(() -> {
            // Update the GUI component showing the elevator's floor
            elevatorLabels[elevatorId][0].setText("Current Floor: " + newFloor);
        });
    }

    @Override
    public void updateState(int elevatorId, String newState) {
        SwingUtilities.invokeLater(() -> {
            // Update the GUI component showing the elevator's state
            elevatorLabels[elevatorId][1].setText("Elevator State: " + newState);
            updateLabels(Color.LIGHT_GRAY, elevatorId);
        });
    }

    private void updateLabels(Color color, int elevatorId){
        elevatorLabels[elevatorId][0].setBackground(color);
        elevatorLabels[elevatorId][1].setBackground(color);
        elevatorPanels[elevatorId].setBackground(color);
    }


    @Override
    public void updateElevatorLamp(int elevatorId, int floorId, boolean lit) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Updating lamp for elevator " + elevatorId + " on floor " + floorId + " to " + (lit ? "ON" : "OFF"));
            elevatorLampPanels[floorId][elevatorId].setText(lit ? "ON" : "OFF");
            elevatorLampPanels[floorId][elevatorId].setBackground(lit ? Color.GREEN : Color.LIGHT_GRAY);
        });
    }


    public void lightupFloorButtons(int floorId, int arrow, boolean lit) {
        // Check if the floorId is within the valid range
        if (floorId < 0 || floorId >= Configuration.NUM_FLOORS) {
            return;
        }

        // Determine the arrow to light up based on the 'arrow' parameter
        JLabel arrowLabel = null;
        if (arrow == 0) { // 0 for up arrow
            arrowLabel = upArrows[floorId][0];
        } else if (arrow == 1) { // 1 for down arrow
            arrowLabel = downArrows[floorId][0];
        }

        // Update the arrow's color based on the 'lit' parameter
        if (arrowLabel != null) {
            if (lit) {
                arrowLabel.setBackground(Color.YELLOW);
            } else {
                arrowLabel.setBackground(Color.LIGHT_GRAY);
            }
            arrowLabel.setOpaque(true);
        }
    }



    public void isFaulted(int elevatorId, int faultType){
        SwingUtilities.invokeLater(() -> {



            switch (faultType) {
                case 0: //Hard Fault

                    elevatorLabels[elevatorId][1].setText("Elevator State: Faulted/ Offline" );
                    elevatorLabels[elevatorId][0].setText("Current Floor: OFFLINE");

                    //Update the GUI component to show that elevator is offline
                    updateLabels(Color.RED, elevatorId);
            
                    for (int i = 0; i < Configuration.NUM_FLOORS; i++) {
                        elevatorLampPanels[i][elevatorId].setText("OFF");
                        elevatorLampPanels[i][elevatorId].setBackground(Color.RED);
                    }

                    break;
                case 1:
                    // Transient Fault
                    elevatorLabels[elevatorId][1].setText("Elevator State: Faulted/ Door Stucked" );
                    //Update the GUI component to show that elevator is offline
                    updateLabels(Color.YELLOW, elevatorId);

                    break;
                default:

                    break;
            }
        });
    }

}
