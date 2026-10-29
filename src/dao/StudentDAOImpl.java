package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Student;
import security.PasswordHasher;
import java.security.GeneralSecurityException;

public class StudentDAOImpl implements StudentDAO {
    @Override
    public void addStudent(Student student) throws SQLException {
        addStudent(student, null);
    }

    @Override
    public void addStudent(Student student, String password) throws SQLException {
        validateStudent(student);
        if (student.getRoomNumber() != 0) {
            throw new SQLException("Create the student without a room, then allocate a room through Room Management.");
        }
        if (password != null && password.length() < 12) {
            throw new SQLException("Password must be at least 12 characters.");
        }
        String sql = "INSERT INTO students (student_id, name, course, phone, email, room_number, password_hash) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String passwordHash = null;
        if (password != null) {
            try {
                passwordHash = PasswordHasher.hash(password.toCharArray());
            } catch (GeneralSecurityException exception) {
                throw new SQLException("Could not securely store the student password.", exception);
            }
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, student.getUserId());
                    statement.setString(2, student.getName());
                    statement.setString(3, student.getCourse());
                    statement.setString(4, student.getPhone());
                    statement.setString(5, student.getEmail());
                    statement.setInt(6, student.getRoomNumber());
                    statement.setString(7, passwordHash);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO notifications (student_id, message) VALUES (?, ?)")) {
                    statement.setInt(1, student.getUserId());
                    statement.setString(2, "Your student account has been created.");
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    @Override
    public Student authenticate(int studentId, String password) throws SQLException {
        if (studentId <= 0 || password == null) {
            return null;
        }
        String sql = "SELECT student_id, name, course, phone, email, room_number, password_hash " +
            "FROM students WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                String passwordHash = resultSet.getString("password_hash");
                if (passwordHash == null) {
                    return null;
                }
                try {
                    return PasswordHasher.verify(password.toCharArray(), passwordHash)
                        ? readStudent(resultSet) : null;
                } catch (GeneralSecurityException exception) {
                    throw new SQLException("Could not verify student credentials.", exception);
                }
            }
        }
    }

    @Override
    public List<Student> getStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_id, name, course, phone, email, room_number FROM students ORDER BY student_id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                students.add(readStudent(resultSet));
            }
        }
        return students;
    }

    @Override
    public Student searchStudent(int studentId) throws SQLException {
        String sql = "SELECT student_id, name, course, phone, email, room_number FROM students WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return readStudent(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateStudent(Student student) throws SQLException {
        updateStudent(student, null);
    }

    @Override
    public void updateStudent(Student student, String password) throws SQLException {
        validateStudent(student);
        boolean updatePassword = password != null && !password.isEmpty();
        String sql = updatePassword
            ? "UPDATE students SET name = ?, course = ?, phone = ?, email = ?, password_hash = ? WHERE student_id = ?"
            : "UPDATE students SET name = ?, course = ?, phone = ?, email = ? WHERE student_id = ?";
        String passwordHash = null;
        if (updatePassword) {
            if (password.length() < 12) {
                throw new SQLException("Password must be at least 12 characters.");
            }
            try {
                passwordHash = PasswordHasher.hash(password.toCharArray());
            } catch (GeneralSecurityException exception) {
                throw new SQLException("Could not securely store the student password.", exception);
            }
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getCourse());
            statement.setString(3, student.getPhone());
            statement.setString(4, student.getEmail());
            if (updatePassword) {
                statement.setString(5, passwordHash);
                statement.setInt(6, student.getUserId());
            } else {
                statement.setInt(5, student.getUserId());
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Student was not found.");
            }
        }
    }

    @Override
    public void deleteStudent(int studentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int roomNumber;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT room_number FROM students WHERE student_id = ? FOR UPDATE")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Student was not found.");
                        }
                        roomNumber = resultSet.getInt("room_number");
                    }
                }
                if (roomNumber > 0) {
                    throw new SQLException("Vacate the student's room before deleting the student.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM complaints WHERE student_id = ?")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        if (resultSet.getInt(1) > 0) {
                            throw new SQLException("Student complaint history exists; deletion is not allowed.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM room_allocations WHERE student_id = ?")) {
                    statement.setInt(1, studentId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        if (resultSet.getInt(1) > 0) {
                            throw new SQLException("Student allocation history exists; deletion is not allowed.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM notifications WHERE student_id = ?")) {
                    statement.setInt(1, studentId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM students WHERE student_id = ?")) {
                    statement.setInt(1, studentId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    private Student readStudent(ResultSet resultSet) throws SQLException {
        return new Student(
            resultSet.getInt("student_id"),
            resultSet.getString("name"),
            resultSet.getString("course"),
            resultSet.getString("phone"),
            resultSet.getString("email"),
            resultSet.getInt("room_number")
        );
    }

    private void validateStudent(Student student) throws SQLException {
        if (student == null || student.getUserId() <= 0 || student.getRoomNumber() < 0
                || student.getName() == null || student.getName().trim().isEmpty()
                || student.getCourse() == null || student.getCourse().trim().isEmpty()
                || student.getPhone() == null || student.getPhone().trim().isEmpty()
                || student.getEmail() == null
                || !student.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new SQLException("Student ID, name, course, valid email, and phone are required.");
        }
    }
}
