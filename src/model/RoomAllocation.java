package model;

import java.time.LocalDateTime;

public class RoomAllocation {
    private final int allocationId;
    private final int studentId;
    private final String studentName;
    private final int roomNumber;
    private final LocalDateTime allocatedAt;
    private final LocalDateTime vacatedAt;

    public RoomAllocation(int allocationId, int studentId, String studentName, int roomNumber,
            LocalDateTime allocatedAt, LocalDateTime vacatedAt) {
        this.allocationId = allocationId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomNumber = roomNumber;
        this.allocatedAt = allocatedAt;
        this.vacatedAt = vacatedAt;
    }

    public int getAllocationId() { return allocationId; }
    public int getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public int getRoomNumber() { return roomNumber; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public LocalDateTime getVacatedAt() { return vacatedAt; }
    public boolean isActive() { return vacatedAt == null; }
}
