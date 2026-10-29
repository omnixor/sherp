package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Complaint;
import model.ComplaintStatusChange;

public class ComplaintDAOImpl implements ComplaintDAO {
    private static final String[] CATEGORIES =
        {"Maintenance", "Plumbing", "Electricity", "Cleanliness", "Security", "Other"};

    @Override
    public void addComplaint(Complaint complaint) throws SQLException {
        validateComplaint(complaint);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int complaintId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO complaints (student_id, category, title, description, status) " +
                            "VALUES (?, ?, ?, ?, 'Pending')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, complaint.getStudentId());
                    statement.setString(2, complaint.getCategory());
                    statement.setString(3, complaint.getTitle().trim());
                    statement.setString(4, complaint.getDescription().trim());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Complaint ID was not generated.");
                        }
                        complaintId = keys.getInt(1);
                    }
                }
                insertStatusHistory(connection, complaintId, "Pending", "");
                insertNotification(connection, complaint.getStudentId(),
                    "Complaint #" + complaintId + " was submitted.");
                connection.commit();
                complaint.setComplaintId(complaintId);
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public List<Complaint> getComplaints() throws SQLException {
        return findComplaints(null);
    }

    @Override
    public List<Complaint> getComplaintsByStudent(int studentId) throws SQLException {
        if (studentId <= 0) {
            throw new SQLException("Student ID must be positive.");
        }
        return findComplaints(studentId);
    }

    private List<Complaint> findComplaints(Integer studentId) throws SQLException {
        String sql = "SELECT complaint_id, student_id, category, title, description, status, submitted_at, " +
            "resolution_note FROM complaints " + (studentId == null ? "" : "WHERE student_id = ? ") +
            "ORDER BY submitted_at DESC, complaint_id DESC";
        List<Complaint> complaints = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp submitted = resultSet.getTimestamp("submitted_at");
                    complaints.add(new Complaint(resultSet.getInt("complaint_id"),
                        resultSet.getInt("student_id"), resultSet.getString("category"),
                        resultSet.getString("title"),
                        resultSet.getString("description"), resultSet.getString("status"),
                        submitted == null ? null : submitted.toLocalDateTime(),
                        resultSet.getString("resolution_note")));
                }
            }
        }
        return complaints;
    }

    @Override
    public void updateStatus(int complaintId, String status) throws SQLException {
        updateStatus(complaintId, status, "");
    }

    @Override
    public void updateStatus(int complaintId, String status, String note) throws SQLException {
        validateStatus(complaintId, status, note);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int studentId;
                String currentStatus;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT student_id, status FROM complaints WHERE complaint_id = ? FOR UPDATE")) {
                    statement.setInt(1, complaintId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Complaint was not found.");
                        }
                        studentId = resultSet.getInt("student_id");
                        currentStatus = resultSet.getString("status");
                    }
                }
                if (!isForwardTransition(currentStatus, status)) {
                    throw new SQLException("Complaint status cannot be reopened or moved backwards.");
                }
                if (!currentStatus.equals(status)) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE complaints SET status = ?, resolution_note = ? WHERE complaint_id = ?")) {
                        statement.setString(1, status);
                        statement.setString(2, note == null || note.trim().isEmpty() ? null : note.trim());
                        statement.setInt(3, complaintId);
                        statement.executeUpdate();
                    }
                    insertStatusHistory(connection, complaintId, status, note);
                    insertNotification(connection, studentId,
                        "Complaint #" + complaintId + " is now " + status + ".");
                } else if (note != null && !note.trim().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE complaints SET resolution_note = ? WHERE complaint_id = ?")) {
                        statement.setString(1, note.trim());
                        statement.setInt(2, complaintId);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    @Override
    public List<ComplaintStatusChange> getStatusHistory(int complaintId) throws SQLException {
        if (complaintId <= 0) {
            throw new SQLException("Complaint ID must be positive.");
        }
        List<ComplaintStatusChange> history = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT status, note, changed_at FROM complaint_status_history " +
                     "WHERE complaint_id = ? ORDER BY changed_at, history_id")) {
            statement.setInt(1, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp changedAt = resultSet.getTimestamp("changed_at");
                    history.add(new ComplaintStatusChange(resultSet.getString("status"),
                        resultSet.getString("note"),
                        changedAt == null ? null : changedAt.toLocalDateTime()));
                }
            }
        }
        return history;
    }

    private void validateComplaint(Complaint complaint) throws SQLException {
        if (complaint == null || complaint.getStudentId() <= 0
                || complaint.getDescription() == null || complaint.getDescription().trim().isEmpty()
                || complaint.getDescription().trim().length() > 500
                || complaint.getTitle() == null || complaint.getTitle().trim().isEmpty()
                || complaint.getTitle().trim().length() > 120
                || !isValidCategory(complaint.getCategory())) {
            throw new SQLException("A student ID, valid category, and description of 1 to 500 characters are required.");
        }
    }

    private boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }
        for (String valid : CATEGORIES) {
            if (valid.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private void validateStatus(int complaintId, String status, String note) throws SQLException {
        if (complaintId <= 0 || !("Pending".equals(status)
                || "In Progress".equals(status) || "Resolved".equals(status))
        || (note != null && note.length() > 1000)
        || ("Resolved".equals(status) && (note == null || note.trim().isEmpty()))) {
            throw new SQLException("Choose a valid complaint and status; notes may not exceed 1000 characters.");
        }
    }

    private boolean isForwardTransition(String current, String next) {
        return current.equals(next) || ("Pending".equals(current)
            && ("In Progress".equals(next) || "Resolved".equals(next)))
            || ("In Progress".equals(current) && "Resolved".equals(next));
    }

    private void insertStatusHistory(Connection connection, int complaintId, String status, String note)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO complaint_status_history (complaint_id, status, note) VALUES (?, ?, ?)")) {
            statement.setInt(1, complaintId);
            statement.setString(2, status);
            statement.setString(3, note == null || note.trim().isEmpty() ? null : note.trim());
            statement.executeUpdate();
        }
    }

    private void insertNotification(Connection connection, int studentId, String message)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notifications (student_id, message) VALUES (?, ?)")) {
            statement.setInt(1, studentId);
            statement.setString(2, message);
            statement.executeUpdate();
        }
    }

    private void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
