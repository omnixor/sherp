package ui;

import dao.ComplaintDAO;
import dao.DAOFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import model.Complaint;
import model.ComplaintStatusChange;

public class ComplaintManagementFrame extends JFrame {
    private static final String[] CATEGORIES =
        {"Maintenance", "Plumbing", "Electricity", "Cleanliness", "Security", "Other"};
    private final Integer studentId;
    private final boolean adminView;
    private final JTextField titleField = HostelTheme.createTextField("Short complaint title");
    private final JTextField descriptionField = HostelTheme.createTextField("Describe your complaint (max 500 characters)");
    private final JTextField complaintIdField = HostelTheme.createTextField("Complaint ID");
    private final JTextField noteField = HostelTheme.createTextField("Administrative response / resolution note");
    private final JTextField searchField = HostelTheme.createTextField("Search ID, title, student, or details");
    private final JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[] {"Pending", "In Progress", "Resolved"});
    private final JComboBox<String> categoryFilter = new JComboBox<>(
        new String[] {"All categories", "Maintenance", "Plumbing", "Electricity", "Cleanliness", "Security", "Other"});
    private final JComboBox<String> statusFilter = new JComboBox<>(
        new String[] {"All statuses", "Pending", "In Progress", "Resolved"});
    private final JLabel pageLabel = new JLabel();
    private final List<Complaint> loadedComplaints = new ArrayList<>();
    private int pageIndex;
    private int pageSize = 10;
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"ID", "Student ID", "Title", "Category", "Details", "Status", "Submitted",
            "Administrative response / resolution"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable();
    private final ComplaintDAO complaintDAO = DAOFactory.complaintDAO();

    public ComplaintManagementFrame(Integer studentId, boolean adminView) {
        this.studentId = studentId;
        this.adminView = adminView;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HostelTheme.styleFrame(this, adminView ? "Complaint Management" : "My Complaints", 1100, 700);
        setLayout(new BorderLayout());
        JPanel header = HostelTheme.createHeaderPanel(adminView ? "Complaint Management" : "My Complaints",
            adminView ? "Review complaints, record responses, and track status history"
                : "Submit and follow your hostel complaints");
        HostelTheme.addBackToMenu(header, this);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(HostelTheme.BG_PRIMARY);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel formCard = HostelTheme.card();
        formCard.setLayout(new BorderLayout(0, 8));
        formCard.add(HostelTheme.sectionLabel(adminView ? "Update complaint" : "Submit a complaint"),
            BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(3, 5, 3, 5);
        constraints.weightx = 1;
        if (adminView) {
            addField(form, constraints, 0, 0, "Complaint ID", complaintIdField);
            HostelTheme.styleComboBox(statusBox);
            addField(form, constraints, 0, 2, "New status", statusBox);
            addField(form, constraints, 1, 0, "Response note", noteField, 3);
        } else {
            HostelTheme.styleComboBox(categoryBox);
            addField(form, constraints, 0, 0, "Title", titleField);
            addField(form, constraints, 0, 2, "Category", categoryBox);
            addField(form, constraints, 1, 0, "Description", descriptionField, 3);
        }
        formCard.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton primaryButton = adminView
            ? HostelTheme.primaryButton("Update complaint status")
                : HostelTheme.successButton("Submit complaint");
        primaryButton.setPreferredSize(new Dimension(adminView ? 220 : 180, 38));
        if (adminView) {
            primaryButton.setToolTipText(
                "Apply the selected status to the complaint shown in the Complaint ID field.");
        }
        JButton historyButton = HostelTheme.secondaryButton("View status history");
        historyButton.setPreferredSize(new Dimension(180, 38));
        JButton refreshButton = HostelTheme.secondaryButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(110, 38));
        actions.add(primaryButton);
        if (adminView) {
            actions.add(historyButton);
        }
        actions.add(refreshButton);
        formCard.add(actions, BorderLayout.SOUTH);
        primaryButton.addActionListener(event -> {
            if (adminView) {
                updateStatus();
            } else {
                submitComplaint();
            }
        });
        historyButton.addActionListener(event -> showStatusHistory());
        refreshButton.addActionListener(event -> loadComplaints());

        JPanel tableCard = HostelTheme.card();
        tableCard.setLayout(new BorderLayout(0, 8));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchField);
        toolbar.add(categoryFilter);
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
        body.add(formCard, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0 && adminView) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                complaintIdField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                statusBox.setSelectedItem(tableModel.getValueAt(row, 5));
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showComplaintDetails();
                }
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { resetAndRender(); }
            @Override public void removeUpdate(DocumentEvent event) { resetAndRender(); }
            @Override public void changedUpdate(DocumentEvent event) { resetAndRender(); }
        });
        categoryFilter.addActionListener(event -> resetAndRender());
        statusFilter.addActionListener(event -> resetAndRender());
        pageSizeBox.addActionListener(event -> {
            pageSize = (Integer) pageSizeBox.getSelectedItem();
            resetAndRender();
        });
        previousButton.addActionListener(event -> {
            if (pageIndex > 0) {
                pageIndex--;
                renderComplaints();
            }
        });
        nextButton.addActionListener(event -> {
            if ((pageIndex + 1) * pageSize < filteredComplaints().size()) {
                pageIndex++;
                renderComplaints();
            }
        });
        loadComplaints();
    }

    private void addField(JPanel form, GridBagConstraints constraints,
            int row, int column, String label, java.awt.Component field) {
        addField(form, constraints, row, column, label, field, 1);
    }

    private void addField(JPanel form, GridBagConstraints constraints, int row, int column,
            String label, java.awt.Component field, int span) {
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = span;
        form.add(HostelTheme.fieldLabel(label), constraints);
        constraints.gridy = row + 1;
        form.add(field, constraints);
        constraints.gridwidth = 1;
    }

    private void submitComplaint() {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String category = String.valueOf(categoryBox.getSelectedItem());
        if (studentId == null || title.isEmpty() || description.isEmpty()
                || title.length() > 120 || description.length() > 500) {
            showMessage("Enter a title (max 120 characters) and description (max 500 characters).");
            return;
        }
        try {
            Complaint complaint = new Complaint(0, studentId, category, title, description,
                "Pending", null, null);
            complaintDAO.addComplaint(complaint);
            titleField.setText("");
            descriptionField.setText("");
            showMessage("Complaint #" + complaint.getComplaintId() + " submitted.");
            loadComplaints();
        } catch (SQLException exception) {
            showMessage("Could not submit complaint: " + exception.getMessage());
        }
    }

    private void updateStatus() {
        try {
            int id = Integer.parseInt(complaintIdField.getText().trim());
            String status = String.valueOf(statusBox.getSelectedItem());
            String note = noteField.getText().trim();
            if ("Resolved".equals(status) && note.isEmpty()) {
                showMessage("Add a resolution note before resolving a complaint.");
                return;
            }
            complaintDAO.updateStatus(id, status, note);
            noteField.setText("");
            showMessage("Complaint status updated.");
            loadComplaints();
        } catch (NumberFormatException exception) {
            showMessage("Select a complaint or enter a numeric complaint ID.");
        } catch (SQLException exception) {
            showMessage("Could not update complaint: " + exception.getMessage());
        }
    }

    private void showStatusHistory() {
        try {
            int id = Integer.parseInt(complaintIdField.getText().trim());
            List<ComplaintStatusChange> changes = complaintDAO.getStatusHistory(id);
            if (changes.isEmpty()) {
                showMessage("No status history is available for this complaint.");
                return;
            }
            StringBuilder text = new StringBuilder();
            for (ComplaintStatusChange change : changes) {
                text.append(change.getChangedAt()).append(" · ").append(change.getStatus());
                if (change.getNote() != null && !change.getNote().trim().isEmpty()) {
                    text.append(" — ").append(change.getNote());
                }
                text.append('\n');
            }
            JOptionPane.showMessageDialog(this, text.toString(), "Complaint status history",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException exception) {
            showMessage("Select a complaint first.");
        } catch (SQLException exception) {
            showMessage("Could not load status history: " + exception.getMessage());
        }
    }

    private void showComplaintDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        row = table.convertRowIndexToModel(row);
        JOptionPane.showMessageDialog(this,
            "Complaint #" + tableModel.getValueAt(row, 0) + "\n"
                + tableModel.getValueAt(row, 2) + " · " + tableModel.getValueAt(row, 3) + "\n\n"
                + tableModel.getValueAt(row, 4) + "\n\nStatus: " + tableModel.getValueAt(row, 5)
                + "\nSubmitted: " + tableModel.getValueAt(row, 6)
                + "\n\nAdministrative response / resolution: "
                + (tableModel.getValueAt(row, 7) == null
                    || String.valueOf(tableModel.getValueAt(row, 7)).isBlank()
                        ? "No response recorded."
                        : tableModel.getValueAt(row, 7)),
            "Complaint details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadComplaints() {
        try {
            loadedComplaints.clear();
            loadedComplaints.addAll(adminView
                ? complaintDAO.getComplaints()
                : complaintDAO.getComplaintsByStudent(studentId));
            resetAndRender();
        } catch (SQLException exception) {
            showMessage("Could not load complaints: " + exception.getMessage());
        }
    }

    private List<Complaint> filteredComplaints() {
        String query = searchField.getText().trim().toLowerCase();
        String category = String.valueOf(categoryFilter.getSelectedItem());
        String status = String.valueOf(statusFilter.getSelectedItem());
        List<Complaint> matches = new ArrayList<>();
        for (Complaint complaint : loadedComplaints) {
            boolean textMatches = query.isEmpty()
                || String.valueOf(complaint.getComplaintId()).contains(query)
                || String.valueOf(complaint.getStudentId()).contains(query)
                || complaint.getTitle().toLowerCase().contains(query)
                || complaint.getDescription().toLowerCase().contains(query)
                || (complaint.getResolutionNote() != null
                    && complaint.getResolutionNote().toLowerCase().contains(query));
            if (textMatches && ("All categories".equals(category) || category.equals(complaint.getCategory()))
                    && ("All statuses".equals(status) || status.equals(complaint.getStatus()))) {
                matches.add(complaint);
            }
        }
        return matches;
    }

    private void resetAndRender() {
        pageIndex = 0;
        renderComplaints();
    }

    private void renderComplaints() {
        List<Complaint> filtered = filteredComplaints();
        int pageCount = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        pageIndex = Math.min(pageIndex, pageCount - 1);
        tableModel.setRowCount(0);
        int start = pageIndex * pageSize;
        for (int index = start; index < Math.min(start + pageSize, filtered.size()); index++) {
            Complaint complaint = filtered.get(index);
            tableModel.addRow(new Object[] {complaint.getComplaintId(), complaint.getStudentId(),
                complaint.getTitle(), complaint.getCategory(), complaint.getDescription(),
                complaint.getStatus(), complaint.getSubmittedAt(), complaint.getResolutionNote()});
        }
        pageLabel.setText((pageIndex + 1) + " / " + pageCount + " (" + filtered.size() + ")");
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
