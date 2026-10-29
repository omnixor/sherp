package database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import model.Complaint;
import model.Room;
import model.RoomAllocation;
import model.Student;
import model.HostelNotification;

// Simple in-memory data used for demonstrations without MySQL.
public class DummyDatabase {
    public static final List<Student> students = new ArrayList<>();
    public static final List<Room> rooms = new ArrayList<>();
    public static final List<Complaint> complaints = new ArrayList<>();
    public static final Map<Integer, String> studentPasswords = new HashMap<>();
    public static final List<RoomAllocation> allocations = new ArrayList<>();
    public static final List<HostelNotification> notifications = new ArrayList<>();
    public static final Map<Integer, List<model.ComplaintStatusChange>> complaintHistory = new HashMap<>();
    private static int nextComplaintId = 1;
    private static int nextAllocationId = 1;
    private static int nextNotificationId = 1;

    static {
        students.add(new Student(1001, "Demo Student", "S3 OOP", "9999999999", 101));
        rooms.add(new Room(101, 4, 1));
        rooms.add(new Room(102, 3, 0));
        allocations.add(new RoomAllocation(nextAllocationId++, 1001, "Demo Student", 101,
            LocalDateTime.now(), null));
    }

    private DummyDatabase() {
    }

    public static int nextComplaintId() {
        return nextComplaintId++;
    }

    public static int nextAllocationId() { return nextAllocationId++; }
    public static int nextNotificationId() { return nextNotificationId++; }
}
