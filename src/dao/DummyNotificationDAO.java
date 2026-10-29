package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.HostelNotification;

public class DummyNotificationDAO implements NotificationDAO {
    @Override
    public List<HostelNotification> getForStudent(int studentId) {
        List<HostelNotification> results = new ArrayList<>();
        for (HostelNotification notification : DummyDatabase.notifications) {
            if (notification.getStudentId() == studentId) {
                results.add(notification);
            }
        }
        return results;
    }

    @Override
    public void markRead(int notificationId, int studentId) throws SQLException {
        for (int index = 0; index < DummyDatabase.notifications.size(); index++) {
            HostelNotification notification = DummyDatabase.notifications.get(index);
            if (notification.getNotificationId() == notificationId
                    && notification.getStudentId() == studentId) {
                DummyDatabase.notifications.set(index, new HostelNotification(notificationId, studentId,
                    notification.getMessage(), notification.getCreatedAt(), true));
                return;
            }
        }
        throw new SQLException("Notification was not found.");
    }
}
