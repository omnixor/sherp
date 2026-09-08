package ui;

import dao.DummyStudentDAO;
import dao.StudentDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Student;

public class StudentManagementFrame extends JFrame {
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField courseField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField roomField = new JTextField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"ID", "Name", "Course", "Phone", "Room"}, 0);
    private final StudentDAO studentDAO = new DummyStudentDAO();

    public StudentManagementFrame() {
        setTitle("Student Management");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createTitledBorder("Student Details"),
            javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        addField(form, "ID", idField);
        addField(form, "Name", nameField);
        addField(form, "Course", courseField);
        addField(form, "Phone", phoneField);
        addField(form, "Room", roomField);
        add(form, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.setBorder(javax.swing.BorderFactory.createTitledBorder("Actions"));
        buttons.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton addButton = new JButton("Add");
        JButton searchButton = new JButton("Search");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton viewButton = new JButton("View All");
        buttons.add(addButton);
        buttons.add(searchButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(viewButton);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addStudent());
        searchButton.addActionListener(event -> searchStudent());
        updateButton.addActionListener(event -> updateStudent());
        deleteButton.addActionListener(event -> deleteStudent());
        viewButton.addActionListener(event -> loadStudents());
        loadStudents();
    }

    private void addField(JPanel panel, String label, JTextField field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private Student readForm() {
        int id = Integer.parseInt(idField.getText().trim());
        int room = Integer.parseInt(roomField.getText().trim());
        if (nameField.getText().trim().isEmpty() || courseField.getText().trim().isEmpty()
                || phoneField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Name, course and phone are required.");
        }
        return new Student(id, nameField.getText().trim(), courseField.getText().trim(),
                phoneField.getText().trim(), room);
    }

    private void addStudent() {
        try {
            studentDAO.addStudent(readForm());
            showMessage("Student added.");
            loadStudents();
        } catch (NumberFormatException exception) {
            showMessage("ID and room must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void searchStudent() {
        try {
            Student student = studentDAO.searchStudent(Integer.parseInt(idField.getText().trim()));
            if (student == null) {
                showMessage("Student not found.");
                return;
            }
            nameField.setText(student.getName());
            courseField.setText(student.getCourse());
            phoneField.setText(student.getPhone());
            roomField.setText(String.valueOf(student.getRoomNumber()));
        } catch (NumberFormatException exception) {
            showMessage("Enter a numeric student ID.");
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void updateStudent() {
        try {
            studentDAO.updateStudent(readForm());
            showMessage("Student updated.");
            loadStudents();
        } catch (NumberFormatException exception) {
            showMessage("ID and room must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void deleteStudent() {
        try {
            studentDAO.deleteStudent(Integer.parseInt(idField.getText().trim()));
            showMessage("Student deleted.");
            loadStudents();
        } catch (NumberFormatException exception) {
            showMessage("Enter a numeric student ID.");
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void loadStudents() {
        try {
            tableModel.setRowCount(0);
            for (Student student : studentDAO.getStudents()) {
                tableModel.addRow(new Object[] {student.getUserId(), student.getName(), student.getCourse(),
                    student.getPhone(), student.getRoomNumber()});
            }
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
