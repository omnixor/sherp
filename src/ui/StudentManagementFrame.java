package ui;

import dao.DAOFactory;
import dao.StudentDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import model.Student;

public class StudentManagementFrame extends JFrame {
    private final JTextField idField = HostelTheme.createTextField("e.g. 1001");
    private final JTextField nameField = HostelTheme.createTextField("Full name");
    private final JTextField emailField = HostelTheme.createTextField("name@example.com");
    private final JTextField courseField = HostelTheme.createTextField("e.g. S3 OOP");
    private final JTextField phoneField = HostelTheme.createTextField("Phone number");
    private final JTextField roomField = HostelTheme.createTextField("Room number");
    private final JPasswordField passwordField = HostelTheme.passwordField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"ID", "Name", "Email", "Course", "Phone", "Room"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable();
    private final StudentDAO studentDAO = DAOFactory.studentDAO();
    private final JTextField searchField = HostelTheme.createTextField("Search ID, name, course, or phone");
    private final JLabel pageLabel = new JLabel();
    private final JLabel studentCountLabel = new JLabel("0 students");
    private final List<Student> loadedStudents = new ArrayList<>();
    private int pageIndex;
    private int pageSize = 10;

    public StudentManagementFrame() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HostelTheme.styleFrame(this, "Student Management", 1020, 720);
        setMinimumSize(new Dimension(820, 600));
        setResizable(true);
        roomField.setEditable(false);
        roomField.setToolTipText("Use Room Management to allocate or vacate rooms.");
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────
        JPanel header = HostelTheme.createHeaderPanel("Student Management",
            "Add, search, update, and remove student records");
        HostelTheme.addBackToMenu(header, this);
        add(header, BorderLayout.NORTH);

        // ── Body ────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(HostelTheme.BG_DARK);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Form card
        JPanel formCard = HostelTheme.createCardPanel();
        formCard.setLayout(new BorderLayout());
        JLabel formTitle = HostelTheme.createSectionLabel("Student Details");
        formTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.weightx = 1.0;

        // Row 1 — ID and Name
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("ID"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        form.add(idField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Name"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.7;
        form.add(nameField, gbc);

        // Row 2 — Email and Course
        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Email"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        form.add(emailField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Course"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        form.add(courseField, gbc);

        // Row 3 — Room
        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Phone"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        form.add(phoneField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Room"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        form.add(roomField, gbc);
        gbc.gridy = 3; gbc.gridx = 0; gbc.weightx = 0;
        form.add(HostelTheme.createLabel("Set / reset password"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
        form.add(passwordField, gbc);
        gbc.gridwidth = 1;

        JPanel actionCard = HostelTheme.createCardPanel();
        actionCard.setLayout(new BorderLayout(0, 10));
        JLabel actionTitle = HostelTheme.createSectionLabel("Student Actions");
        actionCard.add(actionTitle, BorderLayout.NORTH);
        JPanel actionButtons = new JPanel(new java.awt.GridLayout(0, 4, 8, 8));
        actionButtons.setOpaque(false);
        JButton addButton = HostelTheme.createSuccessButton("Add student");
        JButton searchButton = HostelTheme.createAccentButton("Search");
        JButton updateButton = HostelTheme.createOutlineButton("Update");
        JButton allocateButton = HostelTheme.createOutlineButton("Allocate / vacate room");
        JButton deleteButton = HostelTheme.createDangerButton("Delete");
        JButton clearButton = HostelTheme.createOutlineButton("Clear fields");
        JButton refreshButton = HostelTheme.createOutlineButton("Refresh list");
        actionButtons.add(addButton);
        actionButtons.add(searchButton);
        actionButtons.add(updateButton);
        actionButtons.add(allocateButton);
        actionButtons.add(deleteButton);
        actionButtons.add(clearButton);
        actionButtons.add(refreshButton);
        actionCard.add(actionButtons, BorderLayout.CENTER);

        formCard.add(form, BorderLayout.CENTER);
        JPanel topCards = new JPanel();
        topCards.setLayout(new BoxLayout(topCards, BoxLayout.Y_AXIS));
        topCards.setOpaque(false);
        topCards.add(formCard);
        topCards.add(javax.swing.Box.createVerticalStrut(12));
        topCards.add(actionCard);
        body.add(topCards, BorderLayout.NORTH);

        JPanel tableCard = HostelTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel tableHeader = new JPanel(new BorderLayout(8, 0));
        tableHeader.setOpaque(false);
        JPanel listHeading = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        listHeading.setOpaque(false);
        listHeading.add(HostelTheme.createSectionLabel("Registered Students"));
        studentCountLabel.setFont(HostelTheme.FONT_SMALL);
        studentCountLabel.setForeground(HostelTheme.TEXT_MUTED);
        listHeading.add(studentCountLabel);
        tableHeader.add(listHeading, BorderLayout.WEST);
        tableHeader.add(searchField, BorderLayout.EAST);
        tableCard.add(tableHeader, BorderLayout.NORTH);
        JScrollPane scrollPane = HostelTheme.createStyledTable(table, tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        JPanel pager = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        pager.setOpaque(false);
        JButton previousButton = HostelTheme.createOutlineButton("Previous");
        JButton nextButton = HostelTheme.createOutlineButton("Next");
        JComboBox<Integer> pageSizeBox = new JComboBox<>(new Integer[] {10, 25, 50});
        HostelTheme.styleComboBox(pageSizeBox);
        pager.add(new JLabel("Rows"));
        pager.add(pageSizeBox);
        pager.add(previousButton);
        pager.add(pageLabel);
        pager.add(nextButton);
        tableCard.add(pager, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);

        JScrollPane bodyScroll = new JScrollPane(body,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getViewport().setBackground(HostelTheme.BG_PRIMARY);
        add(bodyScroll, BorderLayout.CENTER);

        // ── Events ──────────────────────────────────────────────────────
        addButton.addActionListener(event -> addStudent());
        searchButton.addActionListener(event -> searchStudent());
        idField.addActionListener(event -> searchStudent());
        searchField.addActionListener(event -> searchStudent());
        updateButton.addActionListener(event -> updateStudent());
        allocateButton.addActionListener(event -> openRoomAllocation());
        deleteButton.addActionListener(event -> deleteStudent());
        clearButton.addActionListener(event -> clearForm());
        refreshButton.addActionListener(event -> loadStudents());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filterStudents(); }
            @Override public void removeUpdate(DocumentEvent event) { filterStudents(); }
            @Override public void changedUpdate(DocumentEvent event) { filterStudents(); }
        });
        previousButton.addActionListener(event -> {
            if (pageIndex > 0) {
                pageIndex--;
                renderStudents();
            }
        });
        nextButton.addActionListener(event -> {
            if ((pageIndex + 1) * pageSize < filteredStudents().size()) {
                pageIndex++;
                renderStudents();
            }
        });
        pageSizeBox.addActionListener(event -> {
            pageSize = (Integer) pageSizeBox.getSelectedItem();
            pageIndex = 0;
            renderStudents();
        });

        // Click table row to populate form
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                idField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                emailField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                courseField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
                phoneField.setText(String.valueOf(tableModel.getValueAt(row, 4)));
                roomField.setText(String.valueOf(tableModel.getValueAt(row, 5)));
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent event) { loadStudents(); }
        });
        loadStudents();
    }

    private void openRoomAllocation() {
        String value = idField.getText().trim();
        if (table.getSelectedRow() >= 0) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            value = String.valueOf(tableModel.getValueAt(row, 0));
        }
        try {
            int studentId = Integer.parseInt(value);
            if (studentDAO.searchStudent(studentId) == null) {
                showMessage("Select or enter a registered student first.");
                return;
            }
            new RoomManagementFrame(studentId).setVisible(true);
        } catch (NumberFormatException exception) {
            showMessage("Select a student from the registered students list first.");
        } catch (SQLException exception) {
            showMessage("Could not load the selected student: " + exception.getMessage());
        }
    }

    private Student readForm() {
        int id = Integer.parseInt(idField.getText().trim());
        String roomText = roomField.getText().trim();
        int room = roomText.isEmpty() || "Unallocated".equals(roomText)
            ? 0 : Integer.parseInt(roomText);
        if (id <= 0 || room < 0) {
            throw new IllegalArgumentException("Student ID must be positive and room cannot be negative.");
        }
        if (nameField.getText().trim().isEmpty() || courseField.getText().trim().isEmpty()
                || phoneField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()
                || !emailField.getText().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Name, course, phone and a valid email are required.");
        }
        return new Student(id, nameField.getText().trim(), courseField.getText().trim(),
                phoneField.getText().trim(), emailField.getText().trim(), room);
    }

    private void addStudent() {
        try {
            String password = new String(passwordField.getPassword());
            if (password.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters.");
            }
            studentDAO.addStudent(readForm(), password);
            showMessage("Student added successfully.");
            loadStudents();
            clearForm();
        } catch (NumberFormatException exception) {
            showMessage("ID and room must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void searchStudent() {
        String idQuery = idField.getText().trim();
        String query = idQuery.isEmpty() ? searchField.getText().trim() : idQuery;
        if (query.isEmpty()) {
            showMessage("Enter a student ID or search the registered students list.");
            return;
        }

        final int studentId;
        try {
            studentId = Integer.parseInt(query);
        } catch (NumberFormatException notNumeric) {
            if (!idQuery.isEmpty()) {
                showMessage("Enter a numeric student ID.");
                return;
            }
            filterStudents();
            if (tableModel.getRowCount() == 1) {
                table.setRowSelectionInterval(0, 0);
            } else if (tableModel.getRowCount() == 0) {
                showMessage("No students match that search.");
            } else {
                showMessage("More than one student matches. Refine the search or select a student from the list.");
            }
            return;
        }

        try {
            Student student = studentDAO.searchStudent(studentId);
            if (student == null) {
                nameField.setText("");
                emailField.setText("");
                courseField.setText("");
                phoneField.setText("");
                roomField.setText("");
                passwordField.setText("");
                showMessage("Student not found.");
                return;
            }
            searchField.setText("");
            idField.setText(String.valueOf(student.getUserId()));
            nameField.setText(student.getName());
            emailField.setText(student.getEmail() == null ? "" : student.getEmail());
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
            String password = new String(passwordField.getPassword());
            if (!password.isEmpty() && password.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters.");
            }
            studentDAO.updateStudent(readForm(), password);
            showMessage("Student updated successfully.");
            loadStudents();
            passwordField.setText("");
        } catch (NumberFormatException exception) {
            showMessage("ID and room must be numbers.");
        } catch (IllegalArgumentException | SQLException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void deleteStudent() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete student " + id + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            studentDAO.deleteStudent(id);
            showMessage("Student deleted.");
            loadStudents();
            clearForm();
        } catch (NumberFormatException exception) {
            showMessage("Enter a numeric student ID.");
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void loadStudents() {
        try {
            loadedStudents.clear();
            loadedStudents.addAll(studentDAO.getStudents());
            pageIndex = 0;
            renderStudents();
        } catch (SQLException exception) {
            showMessage("Database error: " + exception.getMessage());
        }
    }

    private void filterStudents() {
        pageIndex = 0;
        renderStudents();
    }

    private List<Student> filteredStudents() {
        String query = searchField.getText().trim().toLowerCase();
        List<Student> results = new ArrayList<>();
        for (Student student : loadedStudents) {
            if (query.isEmpty()
                    || String.valueOf(student.getUserId()).contains(query)
                    || student.getName().toLowerCase().contains(query)
                    || (student.getEmail() != null && student.getEmail().toLowerCase().contains(query))
                    || student.getCourse().toLowerCase().contains(query)
                    || student.getPhone().toLowerCase().contains(query)) {
                results.add(student);
            }
        }
        return results;
    }

    private void renderStudents() {
        List<Student> filtered = filteredStudents();
        int pageCount = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        pageIndex = Math.min(pageIndex, pageCount - 1);
        tableModel.setRowCount(0);
        int start = pageIndex * pageSize;
        for (int index = start; index < Math.min(start + pageSize, filtered.size()); index++) {
            Student student = filtered.get(index);
            tableModel.addRow(new Object[] {student.getUserId(), student.getName(), student.getEmail(),
                student.getCourse(), student.getPhone(),
                student.getRoomNumber() == 0 ? "Unallocated" : student.getRoomNumber()});
        }
        pageLabel.setText((pageIndex + 1) + " / " + pageCount + " (" + filtered.size() + ")");
        studentCountLabel.setText(loadedStudents.isEmpty() ? "No students registered"
            : loadedStudents.size() + (loadedStudents.size() == 1 ? " student" : " students"));
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        courseField.setText("");
        phoneField.setText("");
        emailField.setText("");
        roomField.setText("");
        passwordField.setText("");
        table.clearSelection();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
