package ui;

import dao.DummyRoomDAO;
import dao.RoomDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Room;

public class RoomManagementFrame extends JFrame {
    private final JTextField roomNumberField = new JTextField();
    private final JTextField capacityField = new JTextField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Room", "Capacity", "Occupied", "Status"}, 0);
    private final RoomDAO roomDAO = new DummyRoomDAO();

    public RoomManagementFrame() {
        setTitle("Room Management");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createTitledBorder("Room Details"),
            javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        form.add(new JLabel("Room Number"));
        form.add(roomNumberField);
        form.add(new JLabel("Capacity"));
        form.add(capacityField);
        add(form, BorderLayout.NORTH);
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.setBorder(javax.swing.BorderFactory.createTitledBorder("Actions"));
        JButton addButton = new JButton("Add Room");
        JButton allocateButton = new JButton("Allocate");
        JButton vacateButton = new JButton("Vacate");
        JButton viewButton = new JButton("View Rooms");
        buttons.add(addButton);
        buttons.add(allocateButton);
        buttons.add(vacateButton);
        buttons.add(viewButton);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addRoom());
        allocateButton.addActionListener(event -> changeOccupancy(true));
        vacateButton.addActionListener(event -> changeOccupancy(false));
        viewButton.addActionListener(event -> loadRooms());
        loadRooms();
    }

    private int readRoomNumber() {
        return Integer.parseInt(roomNumberField.getText().trim());
    }

    private void addRoom() {
        try {
            int roomNumber = readRoomNumber();
            int capacity = Integer.parseInt(capacityField.getText().trim());
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be greater than zero.");
            }
            roomDAO.addRoom(new Room(roomNumber, capacity, 0));
            showMessage("Room added.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Room number and capacity must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void changeOccupancy(boolean allocate) {
        try {
            if (allocate) {
                roomDAO.allocateRoom(readRoomNumber());
            } else {
                roomDAO.vacateRoom(readRoomNumber());
            }
            showMessage(allocate ? "Room allocated." : "Room vacated.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Enter a numeric room number.");
        } catch (SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void loadRooms() {
        try {
            tableModel.setRowCount(0);
            for (Room room : roomDAO.getRooms()) {
                tableModel.addRow(new Object[] {room.getRoomNumber(), room.getCapacity(), room.getOccupied(),
                    room.isAvailable() ? "Available" : "Occupied"});
            }
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
