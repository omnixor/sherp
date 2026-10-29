import dao.DAOFactory;
import dao.RoomDAO;
import dao.StudentDAO;
import java.sql.SQLException;
import model.Complaint;
import model.Room;
import model.Student;
import security.SecuritySession;

public final class AuthorizationSmokeTest {
    private AuthorizationSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyRoleAndOwnershipBoundaries();
        System.out.println("AuthorizationSmokeTest passed");
    }

    private static void verifyRoleAndOwnershipBoundaries() throws Exception {
        if (!DAOFactory.isDemoMode()) {
            throw new IllegalStateException("Authorization smoke test requires demo mode.");
        }
        String password = "authorization-test-password";
        StudentDAO students = DAOFactory.studentDAO();
        int firstId = 98101;
        int secondId = 98102;
        students.addStudent(new Student(firstId, "Authorization Student One", "Testing",
            "5550201", "authorization-one@example.test", 0), password);
        students.addStudent(new Student(secondId, "Authorization Student Two", "Testing",
            "5550202", "authorization-two@example.test", 0), password);

        expectDenied(() -> DAOFactory.roomDAO().addRoom(new Room(9810, 2, 0)),
            "Sign in to perform", "anonymous room creation must be denied");
        expectDenied(() -> DAOFactory.studentDAO().getStudents(),
            "Sign in to perform", "anonymous student listing must be denied");

        Student student = students.authenticate(firstId, password);
        require(student != null, "registered student should be able to sign in");
        SecuritySession.start(student);
        require(students.authenticate(secondId, password) != null,
            "authentication remains available while a student session exists");
        expectDenied(() -> DAOFactory.studentDAO().searchStudent(secondId),
            "Administrator permission", "student must not use administrator student search");
        require(DAOFactory.complaintDAO().getComplaintsByStudent(firstId).isEmpty(),
            "student should be able to list their own complaints");
        expectDenied(() -> DAOFactory.complaintDAO().getComplaintsByStudent(secondId),
            "You may only access your own records",
            "student must not view another student's complaints");
        Complaint complaint = new Complaint(0, firstId, "Plumbing", "Owned complaint",
            "A leak needs repair.", "Pending", null, null);
        DAOFactory.complaintDAO().addComplaint(complaint);
        expectDenied(() -> DAOFactory.complaintDAO().getComplaints(),
            "Administrator permission", "student must not list all complaints");
        expectDenied(() -> DAOFactory.complaintDAO().updateStatus(
            complaint.getComplaintId(), "In Progress", "Must remain pending."),
            "Administrator permission", "student must not change complaint status");
        expectDenied(() -> DAOFactory.roomDAO().addRoom(new Room(9811, 2, 0)),
            "Administrator permission", "student must not create a room");
        expectDenied(() -> DAOFactory.roomDAO().allocateRoom(firstId, 9810),
            "Administrator permission", "student must not allocate rooms");
        require(!DAOFactory.notificationDAO().getForStudent(firstId).isEmpty(),
            "student should access their own notifications");
        expectDenied(() -> DAOFactory.notificationDAO().getForStudent(secondId),
            "You may only access your own notifications",
            "student must not access another student's notifications");
        expectDenied(() -> DAOFactory.auditDAO().getRecentEvents(10),
            "Administrator permission", "student must not access audit logs");
        SecuritySession.clear();

        String adminUsername = System.getenv("HOSTEL_DEMO_ADMIN_USERNAME");
        String adminPassword = System.getenv("HOSTEL_DEMO_ADMIN_PASSWORD");
        require(adminUsername != null && adminPassword != null,
            "test administrator credentials must be configured");
        model.Admin admin = DAOFactory.adminDAO().login(adminUsername, adminPassword);
        require(admin != null, "administrator should be able to sign in");
        SecuritySession.start(admin);
        require(students.searchStudent(firstId) != null,
            "administrator should be able to search student records");
        RoomDAO rooms = DAOFactory.roomDAO();
        rooms.addRoom(new Room(9812, 2, 0));
        require(rooms.getRooms().stream().anyMatch(room -> room.getRoomNumber() == 9812),
            "administrator should be able to create rooms");
        require(DAOFactory.complaintDAO().getComplaints().stream()
            .anyMatch(item -> item.getComplaintId() == complaint.getComplaintId()),
            "administrator should be able to view all complaints");
        DAOFactory.complaintDAO().updateStatus(complaint.getComplaintId(),
            "In Progress", "Administrator action allowed.");
        SecuritySession.clear();
    }

    private static void expectDenied(CheckedRunnable operation, String expectedMessage,
            String failureMessage) throws Exception {
        try {
            operation.run();
        } catch (SQLException expected) {
            if (!expected.getMessage().contains(expectedMessage)) {
                throw new AssertionError(failureMessage + "; unexpected denial: "
                    + expected.getMessage(), expected);
            }
            return;
        }
        throw new AssertionError(failureMessage);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
