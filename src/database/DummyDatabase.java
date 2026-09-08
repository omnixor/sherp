package database;

import java.util.ArrayList;
import java.util.List;
import model.Admin;
import model.Complaint;
import model.Room;
import model.Student;

// Simple in-memory data used for demonstrations without MySQL.
public class DummyDatabase {
    public static final List<Student> students = new ArrayList<>();
    public static final List<Room> rooms = new ArrayList<>();
    public static final List<Complaint> complaints = new ArrayList<>();
    public static final List<Admin> admins = new ArrayList<>();
    private static int nextComplaintId = 1;

    static {
        admins.add(new Admin(1, "admin", "admin123"));
        students.add(new Student(1001, "Demo Student", "S3 OOP", "9999999999", 101));
        rooms.add(new Room(101, 4, 1));
        rooms.add(new Room(102, 3, 0));
    }

    private DummyDatabase() {
    }

    public static int nextComplaintId() {
        return nextComplaintId++;
    }
}
