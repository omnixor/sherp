package model;

import java.time.LocalDateTime;

public class HostelNotification {
    private final int notificationId;
    private final int studentId;
    private final String message;
    private final LocalDateTime createdAt;
    private final boolean read;

    public HostelNotification(int notificationId, int studentId, String message,
            LocalDateTime createdAt, boolean read) {
        this.notificationId = notificationId;
        this.studentId = studentId;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getNotificationId() { return notificationId; }
    public int getStudentId() { return studentId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
}
