import dao.DummyComplaintDAO;
import dao.DummyRoomDAO;
import dao.DummyStudentDAO;
import dao.DummyNotificationDAO;
import java.sql.SQLException;
import model.Complaint;
import model.Room;
import model.Student;
import security.PasswordHasher;

public final class WorkflowSmokeTest {
    private static final String PASSWORD = "correct-horse-battery";

    private WorkflowSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyPasswordHashing();
        verifyStudentRoomAndComplaintWorkflow();
        System.out.println("WorkflowSmokeTest passed");
    }

    private static void verifyPasswordHashing() throws Exception {
        String hash = PasswordHasher.hash(PASSWORD.toCharArray());
        require(!hash.contains(PASSWORD), "password hash must not contain the raw password");
        require(PasswordHasher.verify(PASSWORD.toCharArray(), hash), "correct password must verify");
        require(!PasswordHasher.verify("wrong-password".toCharArray(), hash),
            "incorrect password must not verify");
        require(!hash.equals(PasswordHasher.hash(PASSWORD.toCharArray())),
            "password hashes must use unique salts");
    }

    private static void verifyStudentRoomAndComplaintWorkflow() throws Exception {
        DummyStudentDAO students = new DummyStudentDAO();
        DummyRoomDAO rooms = new DummyRoomDAO();
        DummyComplaintDAO complaints = new DummyComplaintDAO();
        int studentId = 98001;
        int roomNumber = 980;
        Student student = new Student(studentId, "Workflow Test", "Testing", "5550100",
            "workflow-test@example.test", 0);
        students.addStudent(student, PASSWORD);

        require(students.authenticate(studentId, PASSWORD) != null,
            "student must authenticate with the stored password");
        require(students.authenticate(studentId, "wrong-password") == null,
            "student must not authenticate with a wrong password");

        rooms.addRoom(new Room(roomNumber, 1, 0));
        rooms.addRoom(new Room(roomNumber + 1, 2, 0));
        rooms.updateRoom(new Room(roomNumber + 1, 3, 0));
        require(rooms.getRooms().stream().anyMatch(room ->
            room.getRoomNumber() == roomNumber + 1 && room.getCapacity() == 3),
            "room updates must persist");
        rooms.deleteRoom(roomNumber + 1);
        rooms.allocateRoom(studentId, roomNumber);
        require(students.searchStudent(studentId).getRoomNumber() == roomNumber,
            "allocation must update the student's room");
        require(rooms.getRooms().stream().anyMatch(room ->
            room.getRoomNumber() == roomNumber && room.getOccupied() == 1),
            "allocation must update room occupancy");
        require(rooms.getAllocationHistory(studentId).size() == 1,
            "room allocation must be recorded in history");

        expectSQLException(() -> rooms.allocateRoom(studentId, roomNumber),
            "a student must not receive multiple active allocations");
        Student secondStudent = new Student(studentId + 1, "Second Student", "Testing", "5550101",
            "second-test@example.test", 0);
        students.addStudent(secondStudent);
        String resetPassword = "new-password-for-test";
        students.updateStudent(secondStudent, resetPassword);
        require(students.authenticate(studentId + 1, resetPassword) != null,
            "administrator password reset must update the student's credentials");
        expectSQLException(() -> rooms.allocateRoom(studentId + 1, roomNumber),
            "a full room must reject another allocation");
        students.deleteStudent(studentId + 1);
        require(students.searchStudent(studentId + 1) == null,
            "a student without retained history should be deletable");
        require(new DummyNotificationDAO().getForStudent(studentId + 1).isEmpty(),
            "deleting a student must remove their non-audit notifications");

        rooms.vacateRoom(studentId);
        require(students.searchStudent(studentId).getRoomNumber() == 0,
            "vacating must clear the student's current room");
        require(rooms.getRooms().stream().anyMatch(room ->
            room.getRoomNumber() == roomNumber && room.getOccupied() == 0),
            "vacating must update room occupancy");
        require(rooms.getAllocationHistory(studentId).size() == 1
                && !rooms.getAllocationHistory(studentId).get(0).isActive(),
            "vacating must preserve historical allocation records");
        int historyStudentId = studentId + 2;
        int historyRoomNumber = roomNumber + 2;
        students.addStudent(new Student(historyStudentId, "Allocation History Student", "Testing",
            "5550102", "allocation-history@example.test", 0));
        rooms.addRoom(new Room(historyRoomNumber, 1, 0));
        rooms.allocateRoom(historyStudentId, historyRoomNumber);
        rooms.vacateRoom(historyStudentId);
        expectSQLException(() -> students.deleteStudent(historyStudentId),
            "students with allocation history must not be deleted");

        complaints.addComplaint(new Complaint(0, studentId, "Plumbing", "Leaking tap",
            "Tap leak", "Pending", null, null));
        require(complaints.getComplaintsByStudent(studentId).size() == 1,
            "student complaints must be available to that student");
        int complaintId = complaints.getComplaintsByStudent(studentId).get(0).getComplaintId();
        complaints.updateStatus(complaintId, "In Progress", "Maintenance scheduled");
        require("Maintenance scheduled".equals(
            complaints.getComplaintsByStudent(studentId).get(0).getResolutionNote()),
            "students must be able to retrieve the administrator's response note");
        complaints.updateStatus(complaintId, "Resolved", "Tap repaired");
        require("Resolved".equals(complaints.getComplaintsByStudent(studentId).get(0).getStatus()),
            "complaint status changes must persist");
        require("Tap repaired".equals(
            complaints.getComplaintsByStudent(studentId).get(0).getResolutionNote()),
            "resolution note must be available with the complaint record");
        require(complaints.getStatusHistory(complaintId).size() == 3,
            "complaint status changes must be audited");
        expectSQLException(() -> complaints.updateStatus(complaintId, "Pending", ""),
            "resolved complaints must not be reopened");
        expectSQLException(() -> students.deleteStudent(studentId),
            "student complaint history must prevent destructive deletion");
        DummyNotificationDAO notifications = new DummyNotificationDAO();
        require(!notifications.getForStudent(studentId).isEmpty(),
            "student activity should create persisted demo notifications");
        int notificationId = notifications.getForStudent(studentId).get(0).getNotificationId();
        notifications.markRead(notificationId, studentId);
        require(notifications.getForStudent(studentId).stream()
            .anyMatch(notification -> notification.getNotificationId() == notificationId && notification.isRead()),
            "notification read state must persist");
    }

    private static void expectSQLException(CheckedRunnable action, String message) throws Exception {
        try {
            action.run();
        } catch (SQLException expected) {
            return;
        }
        throw new AssertionError(message);
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
