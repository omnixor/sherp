package ui;

import dao.DAOFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import model.Admin;
import model.Complaint;
import model.Room;

public class AdminDashboard extends JFrame {
    private final JLabel studentCount = new JLabel("—");
    private final JLabel roomCount = new JLabel("—");
    private final JLabel availableBeds = new JLabel("—");
    private final JLabel pendingComplaints = new JLabel("—");
    private SwingWorker<int[], Void> overviewWorker;

    public AdminDashboard(Admin admin) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        HostelTheme.styleFrame(this, "Smart Hostel - Admin Dashboard", 980, 640);
        setMinimumSize(new Dimension(800, 560));
        setResizable(true);
        setLayout(new BorderLayout());

        JPanel header = HostelTheme.createHeaderPanel("Administrator Dashboard",
            "Signed in as " + admin.getUsername());
        HostelTheme.addAdminActions(header);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(HostelTheme.BG_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(28, 34, 30, 34));

        JPanel summary = new JPanel(new BorderLayout(0, 14));
        summary.setOpaque(false);
        JPanel summaryHeader = new JPanel(new BorderLayout());
        summaryHeader.setOpaque(false);
        JLabel summaryTitle = HostelTheme.sectionLabel("Hostel overview");
        summaryTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        summaryHeader.add(summaryTitle, BorderLayout.WEST);
        JButton refreshButton = HostelTheme.secondaryButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 34));
        summaryHeader.add(refreshButton, BorderLayout.EAST);
        summary.add(summaryHeader, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(createMetric("Registered students", studentCount));
        metrics.add(createMetric("Rooms", roomCount));
        metrics.add(createMetric("Available beds", availableBeds));
        metrics.add(createMetric("Pending complaints", pendingComplaints));
        summary.add(metrics, BorderLayout.CENTER);
        content.add(summary, BorderLayout.NORTH);

        JPanel actions = new JPanel(new BorderLayout(0, 14));
        actions.setOpaque(false);
        actions.add(HostelTheme.sectionLabel("Administration"), BorderLayout.NORTH);
        JPanel actionCards = new JPanel(new GridLayout(1, 3, 14, 0));
        actionCards.setOpaque(false);
        actionCards.add(createActionCard("Students",
            "View registered students, add or edit records, and manage room assignments.",
            "Open student records", () -> new StudentManagementFrame().setVisible(true)));
        actionCards.add(createActionCard("Rooms",
            "Add rooms, update capacity, allocate students, and review allocation history.",
            "Manage rooms", () -> new RoomManagementFrame().setVisible(true)));
        actionCards.add(createActionCard("Complaints",
            "Review submissions, update status, and record resolution notes.",
            "Manage complaints",
            () -> new ComplaintManagementFrame(null, true).setVisible(true)));
        actions.add(actionCards, BorderLayout.CENTER);
        content.add(actions, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        refreshButton.addActionListener(event -> refreshOverview());
        addWindowListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent event) { refreshOverview(); }
        });
        refreshOverview();
    }

    private JPanel createMetric(String title, JLabel value) {
        JPanel card = HostelTheme.card();
        card.setLayout(new BorderLayout(0, 10));
        value.setFont(HostelTheme.FONT_TITLE.deriveFont(26f));
        value.setForeground(HostelTheme.TEXT_PRIMARY);
        JLabel heading = HostelTheme.fieldLabel(title);
        heading.setForeground(HostelTheme.TEXT_MUTED);
        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createActionCard(String title, String description, String buttonText,
            Runnable action) {
        JPanel card = HostelTheme.card();
        card.setLayout(new BorderLayout(0, 14));
        JLabel heading = HostelTheme.sectionLabel(title);
        card.add(heading, BorderLayout.NORTH);
        JLabel details = new JLabel("<html><div style='width:180px'>" + description + "</div></html>");
        details.setFont(HostelTheme.FONT_BODY);
        details.setForeground(HostelTheme.TEXT_SECONDARY);
        details.setVerticalAlignment(JLabel.TOP);
        card.add(details, BorderLayout.CENTER);
        JButton button = HostelTheme.primaryButton(buttonText);
        button.setPreferredSize(new Dimension(210, 38));
        button.setMinimumSize(new Dimension(170, 38));
        button.addActionListener(event -> action.run());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(button);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void refreshOverview() {
        if (overviewWorker != null && !overviewWorker.isDone()) {
            return;
        }
        overviewWorker = new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws SQLException {
                int students = DAOFactory.studentDAO().getStudents().size();
                java.util.List<Room> rooms = DAOFactory.roomDAO().getRooms();
                int bedsAvailable = 0;
                for (Room room : rooms) {
                    bedsAvailable += Math.max(0, room.getCapacity() - room.getOccupied());
                }
                java.util.List<Complaint> complaints = DAOFactory.complaintDAO().getComplaints();
                int pending = (int) complaints.stream()
                    .filter(complaint -> "Pending".equals(complaint.getStatus())).count();
                return new int[] {students, rooms.size(), bedsAvailable, pending};
            }

            @Override
            protected void done() {
                try {
                    int[] totals = get();
                    studentCount.setText(String.valueOf(totals[0]));
                    roomCount.setText(String.valueOf(totals[1]));
                    availableBeds.setText(String.valueOf(totals[2]));
                    pendingComplaints.setText(String.valueOf(totals[3]));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                        "Overview refresh was interrupted.", "Refresh error",
                        JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                        "Could not refresh the hostel overview: " + cause.getMessage(),
                        "Refresh error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    overviewWorker = null;
                }
            }
        };
        overviewWorker.execute();
    }
}
