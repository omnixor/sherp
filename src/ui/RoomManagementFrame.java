package ui;

import dao.DAOFactory;
import dao.RoomDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import model.Room;
import model.RoomAllocation;

public class RoomManagementFrame extends JFrame {
    private final JTextField roomNumberField = HostelTheme.createTextField("e.g. 101");
    private final JTextField capacityField = HostelTheme.createTextField("e.g. 4");
    private final JTextField studentIdField = HostelTheme.createTextField("Student ID");
    private final JTextField searchField = HostelTheme.createTextField("Search room number");
    private final JComboBox<String> statusFilter =
        new JComboBox<>(new String[] {"All statuses", "Available", "Partially occupied", "Full"});
    private final JLabel pageLabel = new JLabel();
    private final List<Room> loadedRooms = new ArrayList<>();
    private int pageIndex;
    private int pageSize = 10;
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Room", "Capacity", "Occupied", "Available beds", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable();
    private final RoomDAO roomDAO = DAOFactory.roomDAO();

    public RoomManagementFrame() {
        this(null);
    }

    public RoomManagementFrame(Integer initialStudentId) {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HostelTheme.styleFrame(this, "Room Management", 1040, 720);
        setMinimumSize(new java.awt.Dimension(860, 600));
        setResizable(true);
        setLayout(new BorderLayout());
        JPanel header = HostelTheme.createHeaderPanel("Room Management",
            "Manage rooms, availability, allocation, and allocation history");
        HostelTheme.addBackToMenu(header, this);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(HostelTheme.BG_PRIMARY);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel formCard = HostelTheme.card();
        formCard.setLayout(new BorderLayout(0, 8));
        formCard.add(HostelTheme.sectionLabel("Room and allocation details"), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.gridy = 0;
        addField(form, gbc, 0, "Room number", roomNumberField);
        addField(form, gbc, 2, "Capacity", capacityField);
        gbc.gridy = 2;
        addField(form, gbc, 0, "Student ID", studentIdField, 3);
        formCard.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.GridLayout(2, 4, 8, 8));
        actions.setOpaque(false);
        JButton addButton = HostelTheme.successButton("Add room");
        JButton updateButton = HostelTheme.secondaryButton("Update capacity");
        JButton deleteButton = HostelTheme.dangerButton("Delete room");
        JButton allocateButton = HostelTheme.primaryButton("Allocate room");
        JButton vacateButton = HostelTheme.secondaryButton("Vacate room");
        JButton historyButton = HostelTheme.secondaryButton("Allocation history");
        JButton refreshButton = HostelTheme.secondaryButton("Refresh");
        actions.add(addButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        actions.add(allocateButton);
        actions.add(vacateButton);
        actions.add(historyButton);
        actions.add(refreshButton);
        formCard.add(actions, BorderLayout.SOUTH);
        addButton.addActionListener(event -> addRoom());
        updateButton.addActionListener(event -> updateRoom());
        deleteButton.addActionListener(event -> deleteRoom());
        allocateButton.addActionListener(event -> changeAllocation(true));
        vacateButton.addActionListener(event -> changeAllocation(false));
        historyButton.addActionListener(event -> showAllocationHistory());
        refreshButton.addActionListener(event -> loadRooms());
        body.add(formCard, BorderLayout.NORTH);

        JPanel tableCard = HostelTheme.card();
        tableCard.setLayout(new BorderLayout(0, 8));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchField);
        toolbar.add(statusFilter);
        tableCard.add(toolbar, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCard.add(HostelTheme.createStyledTable(table, tableModel), BorderLayout.CENTER);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        JComboBox<Integer> pageSizeBox = new JComboBox<>(new Integer[] {10, 25, 50});
        HostelTheme.styleComboBox(pageSizeBox);
        JButton previousButton = HostelTheme.secondaryButton("Previous");
        JButton nextButton = HostelTheme.secondaryButton("Next");
        paging.add(new JLabel("Rows"));
        paging.add(pageSizeBox);
        paging.add(previousButton);
        paging.add(pageLabel);
        paging.add(nextButton);
        tableCard.add(paging, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                roomNumberField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                capacityField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { resetAndRender(); }
            @Override public void removeUpdate(DocumentEvent event) { resetAndRender(); }
            @Override public void changedUpdate(DocumentEvent event) { resetAndRender(); }
        });
        statusFilter.addActionListener(event -> resetAndRender());
        pageSizeBox.addActionListener(event -> {
            pageSize = (Integer) pageSizeBox.getSelectedItem();
            resetAndRender();
        });
        previousButton.addActionListener(event -> {
            if (pageIndex > 0) {
                pageIndex--;
                renderRooms();
            }
        });
        nextButton.addActionListener(event -> {
            if ((pageIndex + 1) * pageSize < filteredRooms().size()) {
                pageIndex++;
                renderRooms();
            }
        });
        if (initialStudentId != null) {
            studentIdField.setText(String.valueOf(initialStudentId));
        }
        addWindowListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent event) { loadRooms(); }
        });
        loadRooms();
    }

    private void addField(JPanel form, GridBagConstraints gbc, int column,
            String title, java.awt.Component field) {
        addField(form, gbc, column, title, field, 1);
    }

    private void addField(JPanel form, GridBagConstraints gbc, int column,
            String title, java.awt.Component field, int span) {
        gbc.gridx = column;
        gbc.weightx = 0;
        form.add(HostelTheme.fieldLabel(title), gbc);
        gbc.gridx = column + 1;
        gbc.weightx = 1;
        gbc.gridwidth = span;
        form.add(field, gbc);
        gbc.gridwidth = 1;
    }

    private int readRoomNumber() {
        return Integer.parseInt(roomNumberField.getText().trim());
    }

    private int readCapacity() {
        return Integer.parseInt(capacityField.getText().trim());
    }

    private void addRoom() {
        try {
            roomDAO.addRoom(new Room(readRoomNumber(), readCapacity(), 0));
            showMessage("Room added successfully.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Room number and capacity must be numbers.");
        } catch (SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void updateRoom() {
        try {
            roomDAO.updateRoom(new Room(readRoomNumber(), readCapacity(), 0));
            showMessage("Room capacity updated.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Room number and capacity must be numbers.");
        } catch (SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void deleteRoom() {
        try {
            int roomNumber = readRoomNumber();
            int answer = JOptionPane.showConfirmDialog(this,
                "Delete room " + roomNumber + "? Rooms with allocation history cannot be deleted.",
                "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            roomDAO.deleteRoom(roomNumber);
            showMessage("Room deleted.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Enter a numeric room number.");
        } catch (SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void changeAllocation(boolean allocate) {
        try {
            int id = Integer.parseInt(studentIdField.getText().trim());
            if (allocate) {
                roomDAO.allocateRoom(id, readRoomNumber());
            } else {
                int answer = JOptionPane.showConfirmDialog(this,
                    "Vacate the current room for student " + id + "?",
                    "Confirm vacating", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
                roomDAO.vacateRoom(id);
            }
            showMessage(allocate ? "Room allocated." : "Room vacated.");
            loadRooms();
        } catch (NumberFormatException exception) {
            showMessage("Enter a valid student ID and room number.");
        } catch (SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void showAllocationHistory() {
        try {
            DefaultTableModel historyModel = new DefaultTableModel(
                new String[] {"Student ID", "Student", "Room", "Allocated", "Vacated", "Status"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (RoomAllocation allocation : roomDAO.getAllocationHistory(null)) {
                historyModel.addRow(new Object[] {allocation.getStudentId(), allocation.getStudentName(),
                    allocation.getRoomNumber(), allocation.getAllocatedAt(),
                    allocation.getVacatedAt() == null ? "—" : allocation.getVacatedAt(),
                    allocation.isActive() ? "Active" : "Historical"});
            }
            JTable historyTable = new JTable();
            JScrollPane historyScroll = HostelTheme.createStyledTable(historyTable, historyModel);
            historyScroll.setPreferredSize(new java.awt.Dimension(760, 340));
            JOptionPane.showMessageDialog(this, historyScroll, "Allocation history",
                JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException exception) {
            showMessage("Could not load allocation history: " + exception.getMessage());
        }
    }

    private void loadRooms() {
        try {
            loadedRooms.clear();
            loadedRooms.addAll(roomDAO.getRooms());
            resetAndRender();
        } catch (SQLException exception) {
            showMessage("Could not load rooms: " + exception.getMessage());
        }
    }

    private List<Room> filteredRooms() {
        String query = searchField.getText().trim().toLowerCase();
        String selectedStatus = String.valueOf(statusFilter.getSelectedItem());
        List<Room> results = new ArrayList<>();
        for (Room room : loadedRooms) {
            int freeBeds = room.getCapacity() - room.getOccupied();
            String status = room.getOccupied() == 0 ? "Available"
                : freeBeds == 0 ? "Full" : "Partially occupied";
            if ((!query.isEmpty() && !String.valueOf(room.getRoomNumber()).contains(query))
                    || (!"All statuses".equals(selectedStatus) && !selectedStatus.equals(status))) {
                continue;
            }
            results.add(room);
        }
        return results;
    }

    private void resetAndRender() {
        pageIndex = 0;
        renderRooms();
    }

    private void renderRooms() {
        List<Room> filtered = filteredRooms();
        int pageCount = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        pageIndex = Math.min(pageIndex, pageCount - 1);
        tableModel.setRowCount(0);
        int start = pageIndex * pageSize;
        for (int index = start; index < Math.min(start + pageSize, filtered.size()); index++) {
            Room room = filtered.get(index);
            int freeBeds = room.getCapacity() - room.getOccupied();
            String status = room.getOccupied() == 0 ? "Available"
                : freeBeds == 0 ? "Full" : "Partially occupied";
            tableModel.addRow(new Object[] {room.getRoomNumber(), room.getCapacity(),
                room.getOccupied(), freeBeds, status});
        }
        pageLabel.setText((pageIndex + 1) + " / " + pageCount + " (" + filtered.size() + ")");
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
