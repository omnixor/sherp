package ui;

import dao.DAOFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import model.Complaint;
import model.HostelNotification;
import model.Student;

public class StudentDashboard extends JFrame {
    private final Student student;
    private final DefaultListModel<HostelNotification> notificationModel = new DefaultListModel<>();
    private final JList<HostelNotification> notificationList = new JList<>(notificationModel);
    private final DefaultListModel<String> recentComplaintModel = new DefaultListModel<>();

    public StudentDashboard(Student student) {
        this.student = student;
        HostelTheme.styleFrame(this, "Student Dashboard", 820, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel header = HostelTheme.createHeaderPanel("Welcome, " + student.getName(),
            "Student dashboard · " + student.getCourse());
        HostelTheme.addSignOut(header);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(HostelTheme.BG_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel summary = HostelTheme.card();
        summary.setLayout(new GridLayout(0, 2, 14, 10));
        summary.add(HostelTheme.fieldLabel("Student ID"));
        summary.add(HostelTheme.fieldLabel(String.valueOf(student.getUserId())));
        summary.add(HostelTheme.fieldLabel("Department / course"));
        summary.add(HostelTheme.fieldLabel(student.getCourse()));
        summary.add(HostelTheme.fieldLabel("Phone"));
        summary.add(HostelTheme.fieldLabel(student.getPhone()));
        summary.add(HostelTheme.fieldLabel("Email"));
        summary.add(HostelTheme.fieldLabel(student.getEmail() == null ? "Not provided" : student.getEmail()));
        summary.add(HostelTheme.fieldLabel("Room allocation"));
        summary.add(HostelTheme.fieldLabel(student.getRoomNumber() == 0
            ? "Not currently allocated" : "Room " + student.getRoomNumber()));
        content.add(summary, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.setOpaque(false);
        JPanel recent = HostelTheme.card();
        recent.setLayout(new BorderLayout(0, 10));
        recent.add(HostelTheme.sectionLabel("Recent complaints"), BorderLayout.NORTH);
        JList<String> complaintList = new JList<>(recentComplaintModel);
        recent.add(new JScrollPane(complaintList), BorderLayout.CENTER);
        lower.add(recent);

        JPanel notifications = HostelTheme.card();
        notifications.setLayout(new BorderLayout(0, 10));
        notifications.add(HostelTheme.sectionLabel("Notifications"), BorderLayout.NORTH);
        notificationList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel((value.isRead() ? "" : "• ")
                + value.getMessage() + "  (" + value.getCreatedAt() + ")");
            label.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
            label.setOpaque(true);
            label.setBackground(selected ? HostelTheme.TABLE_SELECT
                : index % 2 == 0 ? HostelTheme.BG_SECONDARY : HostelTheme.TABLE_ALT);
            label.setForeground(HostelTheme.TEXT_PRIMARY);
            return label;
        });
        notifications.add(new JScrollPane(notificationList), BorderLayout.CENTER);
        JButton markReadButton = HostelTheme.secondaryButton("Mark selected as read");
        notifications.add(markReadButton, BorderLayout.SOUTH);
        markReadButton.addActionListener(event -> markSelectedRead());
        lower.add(notifications);
        content.add(lower, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton submitComplaintButton = HostelTheme.successButton("Submit complaint");
        submitComplaintButton.setPreferredSize(new Dimension(170, 38));
        JButton complaintsButton = HostelTheme.primaryButton("My complaints");
        complaintsButton.setPreferredSize(new Dimension(150, 38));
        JButton allocationHistoryButton = HostelTheme.secondaryButton("Allocation history");
        allocationHistoryButton.setPreferredSize(new Dimension(170, 38));
        footer.add(submitComplaintButton);
        footer.add(complaintsButton);
        footer.add(allocationHistoryButton);
        content.add(footer, BorderLayout.SOUTH);
        submitComplaintButton.addActionListener(event ->
            new ComplaintManagementFrame(student.getUserId(), false).setVisible(true));
        complaintsButton.addActionListener(event ->
            new ComplaintManagementFrame(student.getUserId(), false).setVisible(true));
        allocationHistoryButton.addActionListener(event -> showAllocationHistory());
        add(content, BorderLayout.CENTER);
        loadNotifications();
        loadRecentComplaints();
    }

    private void loadNotifications() {
        new SwingWorker<List<HostelNotification>, Void>() {
            @Override
            protected List<HostelNotification> doInBackground() throws SQLException {
                return DAOFactory.notificationDAO().getForStudent(student.getUserId());
            }

            @Override
            protected void done() {
                try {
                    notificationModel.clear();
                    for (HostelNotification notification : get()) {
                        notificationModel.addElement(notification);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Loading notifications was interrupted.", "Notification error",
                        JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Could not load notifications: " + exception.getCause().getMessage(),
                        "Notification error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadRecentComplaints() {
        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws SQLException {
                return DAOFactory.complaintDAO().getComplaintsByStudent(student.getUserId());
            }

            @Override
            protected void done() {
                try {
                    recentComplaintModel.clear();
                    for (Complaint complaint : get().stream().limit(8).toList()) {
                        recentComplaintModel.addElement("#" + complaint.getComplaintId() + " · "
                            + complaint.getTitle() + " · " + complaint.getStatus()
                            + (complaint.getResolutionNote() == null
                                || complaint.getResolutionNote().isBlank()
                                    ? "" : " · Response: " + complaint.getResolutionNote()));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Loading complaints was interrupted.", "Complaint error",
                        JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Could not load recent complaints: " + exception.getCause().getMessage(),
                        "Complaint error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void markSelectedRead() {
        HostelNotification selected = notificationList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a notification first.");
            return;
        }
        try {
            DAOFactory.notificationDAO().markRead(selected.getNotificationId(), student.getUserId());
            loadNotifications();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Could not update notification: " + exception.getMessage(),
                "Notification error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAllocationHistory() {
        try {
            StringBuilder text = new StringBuilder();
            for (model.RoomAllocation allocation :
                    DAOFactory.roomDAO().getAllocationHistory(student.getUserId())) {
                text.append("Room ").append(allocation.getRoomNumber())
                    .append(" · allocated ").append(allocation.getAllocatedAt())
                    .append(" · ").append(allocation.isActive() ? "Active"
                        : "vacated " + allocation.getVacatedAt()).append('\n');
            }
            JOptionPane.showMessageDialog(this,
                text.length() == 0 ? "No allocation history." : text.toString(),
                "My allocation history", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Could not load allocation history: " + exception.getMessage(),
                "Allocation history error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
