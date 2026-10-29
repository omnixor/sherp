package dao;

import java.sql.SQLException;
import java.util.List;
import model.HostelNotification;

public interface NotificationDAO {
    List<HostelNotification> getForStudent(int studentId) throws SQLException;
    void markRead(int notificationId, int studentId) throws SQLException;
}
