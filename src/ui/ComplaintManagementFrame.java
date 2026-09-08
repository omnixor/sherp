package ui;

import dao.ComplaintDAO;
import dao.DummyComplaintDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Complaint;

public class ComplaintManagementFrame extends JFrame {
    private final Integer studentId;
    private final boolean adminView;
    private final JTextField descriptionField = new JTextField();
    private final JTextField complaintIdField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>(new String[] {"Pending", "Resolved"});
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Complaint ID", "Student ID", "Description", "Status"}, 0);
    private final ComplaintDAO complaintDAO = new DummyComplaintDAO();

    public ComplaintManagementFrame(Integer studentId, boolean adminView) {
        this.studentId = studentId;
        this.adminView = adminView;
        setTitle(adminView ? "Complaint Management - Admin" : "My Complaints");
        setSize(750, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(1, 4, 8, 8));
        form.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createTitledBorder(adminView ? "Update Complaint" : "Submit Complaint"),
            javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        if (adminView) {
            form.add(new JLabel("Complaint ID"));
            form.add(complaintIdField);
            form.add(new JLabel("New Status"));
            form.add(statusBox);
        } else {
            form.add(new JLabel("Complaint Description"));
            form.add(descriptionField);
            JButton submitButton = new JButton("Submit Complaint");
            form.add(submitButton);
            form.add(new JLabel());
            submitButton.addActionListener(event -> submitComplaint());
        }
        add(form, BorderLayout.NORTH);
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bottom.setBorder(javax.swing.BorderFactory.createTitledBorder("Actions"));
        JButton refreshButton = new JButton("Refresh");
        bottom.add(refreshButton);
        if (adminView) {
            JButton updateButton = new JButton("Update Status");
            bottom.add(updateButton);
            updateButton.addActionListener(event -> updateStatus());
        }
        add(bottom, BorderLayout.SOUTH);
        refreshButton.addActionListener(event -> loadComplaints());
        loadComplaints();
    }

    private void submitComplaint() {
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            showMessage("Enter a complaint description.");
            return;
        }
        try {
            complaintDAO.addComplaint(new Complaint(0, studentId, description, "Pending"));
            descriptionField.setText("");
            showMessage("Complaint submitted.");
            loadComplaints();
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void updateStatus() {
        try {
            int complaintId = Integer.parseInt(complaintIdField.getText().trim());
            complaintDAO.updateStatus(complaintId, (String) statusBox.getSelectedItem());
            showMessage("Complaint status updated.");
            loadComplaints();
        } catch (NumberFormatException exception) {
            showMessage("Complaint ID must be a number.");
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void loadComplaints() {
        try {
            tableModel.setRowCount(0);
            java.util.List<Complaint> complaints = adminView
                    ? complaintDAO.getComplaints()
                    : complaintDAO.getComplaintsByStudent(studentId);
            for (Complaint complaint : complaints) {
                tableModel.addRow(new Object[] {complaint.getComplaintId(), complaint.getStudentId(),
                    complaint.getDescription(), complaint.getStatus()});
            }
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
