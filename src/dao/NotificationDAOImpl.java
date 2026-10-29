package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.HostelNotification;

public class NotificationDAOImpl implements NotificationDAO {
    @Override
    public List<HostelNotification> getForStudent(int studentId) throws SQLException {
        List<HostelNotification> notifications = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT notification_id, student_id, message, created_at, is_read " +
                     "FROM notifications WHERE student_id = ? ORDER BY created_at DESC")) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    notifications.add(new HostelNotification(resultSet.getInt("notification_id"),
                        resultSet.getInt("student_id"), resultSet.getString("message"),
                        createdAt == null ? null : createdAt.toLocalDateTime(),
                        resultSet.getBoolean("is_read")));
                }
            }
        }
        return notifications;
    }

    @Override
    public void markRead(int notificationId, int studentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND student_id = ?")) {
            statement.setInt(1, notificationId);
            statement.setInt(2, studentId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Notification was not found for this student.");
            }
        }
    }
}
