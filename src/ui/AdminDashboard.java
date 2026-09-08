package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.Admin;

public class AdminDashboard extends JFrame {
    public AdminDashboard(Admin admin) {
        setTitle("Smart Hostel - Admin Dashboard");
        setSize(500, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 70, 20, 70));
        JLabel welcomeLabel = new JLabel("Welcome, " + admin.getUsername(), JLabel.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(welcomeLabel, BorderLayout.NORTH);

        JButton studentButton = new JButton("Student Management");
        JButton roomButton = new JButton("Room Management");
        JButton complaintButton = new JButton("Complaint Management");
        JButton logoutButton = new JButton("Logout");
        panel.add(studentButton);
        panel.add(roomButton);
        panel.add(complaintButton);
        panel.add(logoutButton);
        add(panel, BorderLayout.CENTER);

        studentButton.addActionListener(event -> new StudentManagementFrame().setVisible(true));
        roomButton.addActionListener(event -> new RoomManagementFrame().setVisible(true));
        complaintButton.addActionListener(event -> new ComplaintManagementFrame(null, true).setVisible(true));
        logoutButton.addActionListener(event -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
    }
}
