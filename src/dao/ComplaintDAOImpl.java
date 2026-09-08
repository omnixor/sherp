package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Complaint;

public class ComplaintDAOImpl implements ComplaintDAO {
    @Override
    public void addComplaint(Complaint complaint) throws SQLException {
        String sql = "INSERT INTO complaints (student_id, description, status) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaint.getStudentId());
            statement.setString(2, complaint.getDescription());
            statement.setString(3, complaint.getStatus());
            statement.executeUpdate();
        }
    }

    @Override
    public List<Complaint> getComplaints() throws SQLException {
        return findComplaints("SELECT complaint_id, student_id, description, status FROM complaints ORDER BY complaint_id", null);
    }

    @Override
    public List<Complaint> getComplaintsByStudent(int studentId) throws SQLException {
        return findComplaints("SELECT complaint_id, student_id, description, status FROM complaints WHERE student_id = ? ORDER BY complaint_id", studentId);
    }

    private List<Complaint> findComplaints(String sql, Integer studentId) throws SQLException {
        List<Complaint> complaints = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(new Complaint(
                        resultSet.getInt("complaint_id"),
                        resultSet.getInt("student_id"),
                        resultSet.getString("description"),
                        resultSet.getString("status")
                    ));
                }
            }
        }
        return complaints;
    }

    @Override
    public void updateStatus(int complaintId, String status) throws SQLException {
        String sql = "UPDATE complaints SET status = ? WHERE complaint_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, complaintId);
            statement.executeUpdate();
        }
    }
}
