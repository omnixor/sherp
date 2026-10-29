package ui;

import dao.DAOFactory;
import dao.StudentDAO;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import model.Student;

public class StudentRegistrationFrame extends JFrame {
    private final JTextField idField = HostelTheme.createTextField("e.g. 1002");
    private final JTextField nameField = HostelTheme.createTextField("Full name");
    private final JTextField courseField = HostelTheme.createTextField("e.g. S3 OOP");
    private final JTextField phoneField = HostelTheme.createTextField("Phone number");
    private final JTextField emailField = HostelTheme.createTextField("name@example.com");
    private final JPasswordField passwordField = HostelTheme.passwordField();
    private final JPasswordField confirmPasswordField = HostelTheme.passwordField();
    private final StudentDAO studentDAO = DAOFactory.studentDAO();

    public StudentRegistrationFrame() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HostelTheme.styleFrame(this, "Student Registration", 500, 720);
        setMinimumSize(new java.awt.Dimension(440, 600));
        setResizable(true);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────
        JPanel header = HostelTheme.createHeaderPanel("Student Registration", "Create a new student account");
        HostelTheme.addBackToMenu(header, this);
        add(header, BorderLayout.NORTH);

        // ── Body ────────────────────────────────────────────────────────
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(HostelTheme.BG_DARK);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JPanel card = HostelTheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel sectionLabel = HostelTheme.createSectionLabel("Student Details");
        sectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        card.add(sectionLabel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        addFormRow(form, gbc, 0, "Student ID", idField);
        addFormRow(form, gbc, 2, "Name", nameField);
        addFormRow(form, gbc, 4, "Course", courseField);
        addFormRow(form, gbc, 6, "Phone", phoneField);
        addFormRow(form, gbc, 8, "Email", emailField);
        addPasswordRow(form, gbc, 10, "Password", passwordField);
        addPasswordRow(form, gbc, 12, "Confirm Password", confirmPasswordField);

        gbc.gridy = 14;
        gbc.insets = new Insets(16, 0, 0, 0);
        JButton registerButton = HostelTheme.createSuccessButton("Save Student Data");
        registerButton.setToolTipText("Validate the form and save the new student record.");
        form.add(registerButton, gbc);

        card.add(form);
        outer.add(card, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(outer,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(HostelTheme.BG_DARK);
        add(scrollPane, BorderLayout.CENTER);

        // ── Events ──────────────────────────────────────────────────────
        registerButton.addActionListener(event -> registerStudent());
        confirmPasswordField.addActionListener(event -> registerStudent());
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int startRow, String label, JTextField field) {
        gbc.gridy = startRow;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(HostelTheme.createLabel(label), gbc);
        gbc.gridy = startRow + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(field, gbc);
    }

    private void addPasswordRow(JPanel form, GridBagConstraints gbc, int startRow,
            String label, JPasswordField field) {
        gbc.gridy = startRow;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(HostelTheme.createLabel(label), gbc);
        gbc.gridy = startRow + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(field, gbc);
    }

    private void registerStudent() {
        try {
            int studentId = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            String course = courseField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmation = new String(confirmPasswordField.getPassword());
            if (studentId <= 0) {
                throw new IllegalArgumentException("Student ID must be positive.");
            }
            if (name.isEmpty() || course.isEmpty() || phone.isEmpty()
                    || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("All fields are required.");
            }
            if (password.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters.");
            }
            if (!password.equals(confirmation)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            studentDAO.addStudent(new Student(studentId, name, course, phone, email, 0), password);
            JOptionPane.showMessageDialog(this, "Registration successful!\nYou can now log in with your Student ID.");
            dispose();
        } catch (NumberFormatException exception) {
            showMessage("Student ID must be a number.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
