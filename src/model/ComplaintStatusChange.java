package model;

import java.time.LocalDateTime;

public class ComplaintStatusChange {
    private final String status;
    private final String note;
    private final LocalDateTime changedAt;

    public ComplaintStatusChange(String status, String note, LocalDateTime changedAt) {
        this.status = status;
        this.note = note;
        this.changedAt = changedAt;
    }

    public String getStatus() { return status; }
    public String getNote() { return note; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
