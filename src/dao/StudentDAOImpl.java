package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Student;

public class StudentDAOImpl implements StudentDAO {
    @Override
    public void addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (student_id, name, course, phone, room_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, student.getUserId());
            statement.setString(2, student.getName());
            statement.setString(3, student.getCourse());
            statement.setString(4, student.getPhone());
            statement.setInt(5, student.getRoomNumber());
            statement.executeUpdate();
        }
    }

    @Override
    public List<Student> getStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_id, name, course, phone, room_number FROM students ORDER BY student_id";
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
        String sql = "SELECT student_id, name, course, phone, room_number FROM students WHERE student_id = ?";
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
        String sql = "UPDATE students SET name = ?, course = ?, phone = ?, room_number = ? WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getCourse());
            statement.setString(3, student.getPhone());
            statement.setInt(4, student.getRoomNumber());
            statement.setInt(5, student.getUserId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteStudent(int studentId) throws SQLException {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    private Student readStudent(ResultSet resultSet) throws SQLException {
        return new Student(
            resultSet.getInt("student_id"),
            resultSet.getString("name"),
            resultSet.getString("course"),
            resultSet.getString("phone"),
            resultSet.getInt("room_number")
        );
    }
}
