package ui;

import dao.DummyStudentDAO;
import dao.StudentDAO;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.Student;

public class StudentRegistrationFrame extends JFrame {
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField courseField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField roomField = new JTextField();
    private final StudentDAO studentDAO = new DummyStudentDAO();

    public StudentRegistrationFrame() {
        setTitle("Student Registration");
        setSize(420, 330);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(7, 2, 8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel titleLabel = new JLabel("Create Student Account", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel);
        panel.add(new JLabel());
        addField(panel, "Student ID", idField);
        addField(panel, "Name", nameField);
        addField(panel, "Course", courseField);
        addField(panel, "Phone", phoneField);
        addField(panel, "Room Number", roomField);
        JButton registerButton = new JButton("Register");
        panel.add(new JLabel());
        panel.add(registerButton);
        add(panel);

        registerButton.addActionListener(event -> registerStudent());
    }

    private void addField(JPanel panel, String label, JTextField field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void registerStudent() {
        try {
            int studentId = Integer.parseInt(idField.getText().trim());
            int roomNumber = Integer.parseInt(roomField.getText().trim());
            String name = nameField.getText().trim();
            String course = courseField.getText().trim();
            String phone = phoneField.getText().trim();
            if (studentId <= 0 || roomNumber <= 0) {
                throw new IllegalArgumentException("Student ID and room number must be positive.");
            }
            if (name.isEmpty() || course.isEmpty() || phone.isEmpty()) {
                throw new IllegalArgumentException("All fields are required.");
            }

            studentDAO.addStudent(new Student(studentId, name, course, phone, roomNumber));
            JOptionPane.showMessageDialog(this, "Registration successful. You can now log in with your Student ID.");
            dispose();
        } catch (NumberFormatException exception) {
            showMessage("Student ID and room number must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
