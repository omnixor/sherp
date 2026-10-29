package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import model.Student;
import model.Room;
import security.PasswordHasher;

public class DummyStudentDAO implements StudentDAO {
    @Override
    public void addStudent(Student student) throws SQLException {
        addStudent(student, null);
    }

    @Override
    public void addStudent(Student student, String password) throws SQLException {
        validateStudent(student);
        if (password != null && password.length() < 12) {
            throw new SQLException("Password must be at least 12 characters.");
        }
        if (student.getRoomNumber() != 0) {
            throw new SQLException("Create the student without a room, then allocate a room through Room Management.");
        }
        if (searchStudent(student.getUserId()) != null) {
            throw new SQLException("Student ID already exists.");
        }
        for (Student existing : DummyDatabase.students) {
            if (student.getEmail().equalsIgnoreCase(existing.getEmail())) {
                throw new SQLException("Email address is already registered.");
            }
        }
        Room room = findRoom(student.getRoomNumber());
        if (student.getRoomNumber() > 0 && room == null) {
            throw new SQLException("Selected room was not found.");
        }
        if (room != null && !room.isAvailable()) {
            throw new SQLException("Selected room is full.");
        }
        DummyDatabase.students.add(student);
        if (password != null) {
            try {
                DummyDatabase.studentPasswords.put(student.getUserId(),
                    PasswordHasher.hash(password.toCharArray()));
            } catch (GeneralSecurityException exception) {
                DummyDatabase.students.remove(student);
                throw new SQLException("Could not securely store the student password.", exception);
            }
        }
        if (room != null) {
            room.setOccupied(room.getOccupied() + 1);
        }
        DummyDatabase.notifications.add(new model.HostelNotification(DummyDatabase.nextNotificationId(),
            student.getUserId(), "Your student account has been created.",
            java.time.LocalDateTime.now(), false));
    }

    @Override
    public Student authenticate(int studentId, String password) throws SQLException {
        if (studentId <= 0 || password == null) {
            return null;
        }
        Student student = searchStudent(studentId);
        String passwordHash = DummyDatabase.studentPasswords.get(studentId);
        if (student == null || passwordHash == null) {
            return null;
        }
        try {
            return PasswordHasher.verify(password.toCharArray(), passwordHash) ? student : null;
        } catch (GeneralSecurityException exception) {
            throw new SQLException("Could not verify student credentials.", exception);
        }
    }

    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(DummyDatabase.students);
    }

    @Override
    public Student searchStudent(int studentId) {
        for (Student student : DummyDatabase.students) {
            if (student.getUserId() == studentId) {
                return student;
            }
        }
        return null;
    }

    @Override
    public void updateStudent(Student student) throws SQLException {
        updateStudent(student, null);
    }

    @Override
    public void updateStudent(Student student, String password) throws SQLException {
        validateStudent(student);
        Student existingStudent = searchStudent(student.getUserId());
        if (existingStudent == null) {
            throw new SQLException("Student was not found.");
        }
        for (Student other : DummyDatabase.students) {
            if (other.getUserId() != student.getUserId() && student.getEmail().equalsIgnoreCase(other.getEmail())) {
                throw new SQLException("Email address is already registered.");
            }
        }
        existingStudent.setName(student.getName());
        existingStudent.setCourse(student.getCourse());
        existingStudent.setPhone(student.getPhone());
        existingStudent.setEmail(student.getEmail());
        if (password != null && !password.isEmpty()) {
            if (password.length() < 12) {
                throw new SQLException("Password must be at least 12 characters.");
            }
            try {
                DummyDatabase.studentPasswords.put(student.getUserId(),
                    PasswordHasher.hash(password.toCharArray()));
            } catch (GeneralSecurityException exception) {
                throw new SQLException("Could not securely store the student password.", exception);
            }
        }
    }

    @Override
    public void deleteStudent(int studentId) throws SQLException {
        Student student = searchStudent(studentId);
        if (student == null) {
            throw new SQLException("Student was not found.");
        }
        if (student.getRoomNumber() > 0) {
            throw new SQLException("Vacate the student's room before deleting the student.");
        }
        for (model.Complaint complaint : DummyDatabase.complaints) {
            if (complaint.getStudentId() == studentId) {
                throw new SQLException("Student complaint history exists; deletion is not allowed.");
            }
        }
        if (DummyDatabase.allocations.stream().anyMatch(
                allocation -> allocation.getStudentId() == studentId)) {
            throw new SQLException("Student allocation history exists; deletion is not allowed.");
        }
        DummyDatabase.students.remove(student);
        DummyDatabase.studentPasswords.remove(studentId);
        DummyDatabase.notifications.removeIf(notification -> notification.getStudentId() == studentId);
    }

    private Room findRoom(int roomNumber) {
        for (Room room : DummyDatabase.rooms) {
            if (room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }

    private void validateStudent(Student student) throws SQLException {
        if (student == null || student.getUserId() <= 0 || student.getRoomNumber() < 0
                || student.getName() == null || student.getName().trim().isEmpty()
                || student.getCourse() == null || student.getCourse().trim().isEmpty()
                || student.getPhone() == null || student.getPhone().trim().isEmpty()
                || student.getEmail() == null
                || !student.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new SQLException("Student ID, name, course, valid email, and phone are required.");
        }
    }
}
