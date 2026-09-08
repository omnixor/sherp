package ui;

import dao.AdminDAO;
import dao.DummyAdminDAO;
import dao.DummyStudentDAO;
import dao.StudentDAO;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import model.Admin;
import model.Student;

public class LoginFrame extends JFrame {
    private final JComboBox<String> roleBox = new JComboBox<>(new String[] {"Admin", "Student"});
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AdminDAO adminDAO = new DummyAdminDAO();
    private final StudentDAO studentDAO = new DummyStudentDAO();

    public LoginFrame() {
        setTitle("Smart Hostel - Login");
        setSize(460, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(7, 2, 8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel titleLabel = new JLabel("Smart Hostel Management", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel);
        panel.add(new JLabel());
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);
        panel.add(new JLabel("Username / Student ID:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel());
        JButton loginButton = new JButton("Login");
        panel.add(loginButton);
        panel.add(new JLabel("Admin: admin / admin123"));
        panel.add(new JLabel("Student: use student ID"));
        JButton registerButton = new JButton("Register as Student");
        panel.add(new JLabel());
        panel.add(registerButton);
        add(panel);

        roleBox.addActionListener(event -> updatePasswordLabel());
        loginButton.addActionListener(event -> login());
        registerButton.addActionListener(event -> new StudentRegistrationFrame().setVisible(true));
        updatePasswordLabel();
    }

    private void updatePasswordLabel() {
        passwordField.setEnabled(roleBox.getSelectedItem().equals("Admin"));
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a username or student ID.");
            return;
        }

        try {
            if (roleBox.getSelectedItem().equals("Admin")) {
                Admin admin = adminDAO.login(username, password);
                if (admin == null) {
                    JOptionPane.showMessageDialog(this, "Invalid admin login.");
                    return;
                }
                new AdminDashboard(admin).setVisible(true);
            } else {
                int studentId = Integer.parseInt(username);
                Student student = studentDAO.searchStudent(studentId);
                if (student == null) {
                    JOptionPane.showMessageDialog(this, "Student ID was not found.");
                    return;
                }
                new ComplaintManagementFrame(studentId, false).setVisible(true);
            }
            dispose();
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Student ID must be a number.");
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Database error: " + exception.getMessage());
        }
    }
}
