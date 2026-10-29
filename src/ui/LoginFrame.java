package ui;

import dao.AdminDAO;
import dao.DAOFactory;
import dao.StudentDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.JTextField;
import model.Admin;
import model.Student;
import model.User;
import security.SecuritySession;

public class LoginFrame extends JFrame {
    private final JComboBox<String> roleBox = new JComboBox<>(new String[] {"Admin", "Student"});
    private final JTextField usernameField = HostelTheme.textField();
    private final JPasswordField passwordField = HostelTheme.passwordField();
    private final AdminDAO adminDAO = DAOFactory.adminDAO();
    private final StudentDAO studentDAO = DAOFactory.studentDAO();
    private final JButton loginButton = HostelTheme.primaryButton("Login");
    private boolean loginInProgress;

    public LoginFrame() {
        HostelTheme.applyGlobalDefaults();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        HostelTheme.styleFrame(this, "Smart Hostel Management", 500, 620);
        setMinimumSize(new Dimension(440, 560));
        setResizable(true);
        setLayout(new BorderLayout());

        // Top bar
        add(HostelTheme.createTopBar("Smart Hostel", "Management System"), BorderLayout.NORTH);

        // Center
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(HostelTheme.BG_PRIMARY);
        center.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        JPanel card = HostelTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel section = HostelTheme.sectionLabel("Sign In");
        section.setAlignmentX(LEFT_ALIGNMENT);
        card.add(section);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(HostelTheme.fieldLabel("Role"), gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 12, 0);
        HostelTheme.styleComboBox(roleBox);
        form.add(roleBox, gbc);

        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(HostelTheme.fieldLabel("Username / Student ID"), gbc);
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(usernameField, gbc);

        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(HostelTheme.fieldLabel("Password"), gbc);
        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 20, 0);
        form.add(passwordField, gbc);

        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 8, 0);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        form.add(loginButton, gbc);

        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 0, 0);
        JButton registerButton = HostelTheme.secondaryButton("Register as Student");
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        form.add(registerButton, gbc);

        card.add(form);

        // Hints
        card.add(Box.createVerticalStrut(14));
        JLabel hint = new JLabel("Administrator setup uses HOSTEL_ADMIN_USERNAME and HOSTEL_ADMIN_PASSWORD.");
        hint.setText("Sign in as an administrator or register as a student.");
        hint.setFont(HostelTheme.FONT_SMALL);
        hint.setForeground(HostelTheme.TEXT_MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        card.add(hint);

        center.add(card, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(center,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(HostelTheme.BG_PRIMARY);
        add(scrollPane, BorderLayout.CENTER);

        // Events
        roleBox.addActionListener(event -> updatePasswordState());
        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
        registerButton.addActionListener(event -> new StudentRegistrationFrame().setVisible(true));
        updatePasswordState();
    }

    private void updatePasswordState() {
        passwordField.setText("");
        passwordField.setEnabled(true);
        passwordField.setBackground(HostelTheme.BG_SURFACE);
    }

    private void login() {
        if (loginInProgress) {
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean adminLogin = "Admin".equals(roleBox.getSelectedItem());
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a username or student ID.");
            return;
        }

        final int studentId;
        if (!adminLogin) {
            try {
                studentId = Integer.parseInt(username);
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "Student ID must be a number.");
                return;
            }
        } else {
            studentId = 0;
        }

        loginInProgress = true;
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        roleBox.setEnabled(false);
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws SQLException {
                return adminLogin
                    ? adminDAO.login(username, password)
                    : studentDAO.authenticate(studentId, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this, adminLogin
                            ? "Invalid admin login." : "Invalid student ID or password.");
                        return;
                    }
                    SecuritySession.start(user);
                    JFrame dashboard;
                    if (user instanceof Admin) {
                        dashboard = new AdminDashboard((Admin) user);
                    } else if (user instanceof Student) {
                        dashboard = new StudentDashboard((Student) user);
                    } else {
                        throw new IllegalStateException("Unsupported authenticated user type.");
                    }
                    HostelTheme.startSessionMonitor();
                    dashboard.setVisible(true);
                    dispose();
                } catch (InterruptedException exception) {
                    SecuritySession.clear();
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                        "Sign-in was interrupted. Please try again.");
                } catch (ExecutionException exception) {
                    SecuritySession.clear();
                    Throwable cause = exception.getCause();
                    if (cause instanceof SQLException) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                            "Database error: " + cause.getMessage());
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                            "Could not sign in: " + cause.getMessage());
                    }
                } catch (RuntimeException exception) {
                    SecuritySession.clear();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                        "Could not open your dashboard: " + exception.getMessage());
                } finally {
                    loginInProgress = false;
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    roleBox.setEnabled(true);
                    usernameField.setEnabled(true);
                    passwordField.setEnabled(true);
                }
            }
        }.execute();
    }
}
