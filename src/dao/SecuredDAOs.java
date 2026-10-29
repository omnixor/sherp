package dao;

import java.sql.SQLException;
import java.util.List;
import model.Admin;
import model.AuditEvent;
import model.Complaint;
import model.ComplaintStatusChange;
import model.HostelNotification;
import model.Room;
import model.RoomAllocation;
import model.Student;
import model.User;
import security.AuditLogger;
import security.Authorization;
import security.SecuritySession;

final class SecuredDAOs {
    private SecuredDAOs() {
    }

    static AdminDAO secure(AdminDAO delegate) {
        return (username, password) -> {
            if (AuditLogger.isLoginRateLimited("ADMIN", username)) {
                AuditLogger.recordRateLimitedLogin("ADMIN", username);
                return null;
            }
            Admin admin;
            try {
                admin = delegate.login(username, password);
            } catch (SQLException exception) {
                recordLoginFailure("ADMIN", username, exception);
                throw exception;
            }
            AuditLogger.recordLogin("ADMIN", username,
                admin == null ? null : admin.getUserId(), admin != null,
                admin == null ? "Credential verification failed." : "Administrator signed in.");
            return admin;
        };
    }

    static StudentDAO secure(StudentDAO delegate) {
        return new StudentDAO() {
            @Override public void addStudent(Student student) throws SQLException {
                addStudent(student, null);
            }

            @Override public void addStudent(Student student, String password) throws SQLException {
                User actor = SecuritySession.getPrincipal();
                if (actor == null) {
                    if (password == null || student == null || student.getRoomNumber() != 0) {
                        throw denied("STUDENT_REGISTER", "student",
                            student == null ? null : String.valueOf(student.getUserId()),
                            "Only public account registration is allowed without an authenticated session.",
                            "Sign in as an administrator to create student records.");
                    }
                } else if (!(actor instanceof Admin)) {
                    throw denied("STUDENT_CREATE", "student",
                        student == null ? null : String.valueOf(student.getUserId()),
                        "Student account cannot create another student record.",
                        "Only administrators may create student records.");
                } else {
                    Authorization.requireAdmin("STUDENT_CREATE", "student",
                        student == null ? null : String.valueOf(student.getUserId()));
                }
                delegate.addStudent(student, password);
                String action = actor == null ? "STUDENT_REGISTER" : "STUDENT_CREATE";
                if (actor == null) {
                    try {
                        AuditLogger.recordPublicAction(action, "student",
                            String.valueOf(student.getUserId()), "Student self-registration completed.");
                    } catch (SQLException exception) {
                        throw auditFailure(exception);
                    }
                } else {
                    record(action, "student", student.getUserId(), "Student account created.");
                }
            }

            @Override public Student authenticate(int studentId, String password) throws SQLException {
                String subject = String.valueOf(studentId);
                if (AuditLogger.isLoginRateLimited("STUDENT", subject)) {
                    AuditLogger.recordRateLimitedLogin("STUDENT", subject);
                    return null;
                }
                Student student;
                try {
                    student = delegate.authenticate(studentId, password);
                } catch (SQLException exception) {
                    recordLoginFailure("STUDENT", subject, exception);
                    throw exception;
                }
                AuditLogger.recordLogin("STUDENT", subject,
                    student == null ? null : student.getUserId(), student != null,
                    student == null ? "Credential verification failed." : "Student signed in.");
                return student;
            }

            @Override public List<Student> getStudents() throws SQLException {
                Authorization.requireAdmin("STUDENT_LIST", "student", null);
                return delegate.getStudents();
            }

            @Override public Student searchStudent(int studentId) throws SQLException {
                Authorization.requireAdmin("STUDENT_READ", "student", String.valueOf(studentId));
                return delegate.searchStudent(studentId);
            }

            @Override public void updateStudent(Student student) throws SQLException {
                updateStudent(student, null);
            }

            @Override public void updateStudent(Student student, String password) throws SQLException {
                Authorization.requireAdmin("STUDENT_UPDATE", "student",
                    student == null ? null : String.valueOf(student.getUserId()));
                delegate.updateStudent(student, password);
                record("STUDENT_UPDATE", "student", student.getUserId(),
                    "Student details updated; no credential values recorded.");
            }

            @Override public void deleteStudent(int studentId) throws SQLException {
                Authorization.requireAdmin("STUDENT_DELETE", "student", String.valueOf(studentId));
                delegate.deleteStudent(studentId);
                record("STUDENT_DELETE", "student", studentId, "Student record deleted.");
            }
        };
    }

    static RoomDAO secure(RoomDAO delegate) {
        return new RoomDAO() {
            @Override public void addRoom(Room room) throws SQLException {
                Authorization.requireAdmin("ROOM_CREATE", "room",
                    room == null ? null : String.valueOf(room.getRoomNumber()));
                delegate.addRoom(room);
                record("ROOM_CREATE", "room", room.getRoomNumber(), "Room created.");
            }

            @Override public List<Room> getRooms() throws SQLException {
                Authorization.requireAuthenticated("ROOM_LIST", "room", null);
                return delegate.getRooms();
            }

            @Override public void updateRoom(Room room) throws SQLException {
                Authorization.requireAdmin("ROOM_UPDATE", "room",
                    room == null ? null : String.valueOf(room.getRoomNumber()));
                delegate.updateRoom(room);
                record("ROOM_UPDATE", "room", room.getRoomNumber(), "Room capacity updated.");
            }

            @Override public void deleteRoom(int roomNumber) throws SQLException {
                Authorization.requireAdmin("ROOM_DELETE", "room", String.valueOf(roomNumber));
                delegate.deleteRoom(roomNumber);
                record("ROOM_DELETE", "room", roomNumber, "Room deleted.");
            }

            @Override public void allocateRoom(int studentId, int roomNumber) throws SQLException {
                Authorization.requireAdmin("ROOM_ALLOCATE", "student", String.valueOf(studentId));
                delegate.allocateRoom(studentId, roomNumber);
                record("ROOM_ALLOCATE", "student", studentId, "Room " + roomNumber + " allocated.");
            }

            @Override public void vacateRoom(int studentId) throws SQLException {
                Authorization.requireAdmin("ROOM_VACATE", "student", String.valueOf(studentId));
                delegate.vacateRoom(studentId);
                record("ROOM_VACATE", "student", studentId, "Room allocation vacated.");
            }

            @Override public List<RoomAllocation> getAllocationHistory(Integer studentId)
                    throws SQLException {
                if (studentId == null) {
                    Authorization.requireAdmin("ALLOCATION_HISTORY_READ", "allocation", null);
                } else {
                    Authorization.requireSelfOrAdmin(studentId, "ALLOCATION_HISTORY_READ", "allocation");
                }
                return delegate.getAllocationHistory(studentId);
            }
        };
    }

    static ComplaintDAO secure(ComplaintDAO delegate) {
        return new ComplaintDAO() {
            @Override public void addComplaint(Complaint complaint) throws SQLException {
                Authorization.requireSelfOrAdmin(
                    complaint == null ? 0 : complaint.getStudentId(), "COMPLAINT_CREATE", "complaint");
                delegate.addComplaint(complaint);
                record("COMPLAINT_CREATE", "complaint", complaint.getComplaintId(),
                    "Complaint submitted with initial status Pending.");
            }

            @Override public List<Complaint> getComplaints() throws SQLException {
                Authorization.requireAdmin("COMPLAINT_LIST", "complaint", null);
                return delegate.getComplaints();
            }

            @Override public List<Complaint> getComplaintsByStudent(int studentId) throws SQLException {
                Authorization.requireSelfOrAdmin(studentId, "COMPLAINT_LIST", "complaint");
                return delegate.getComplaintsByStudent(studentId);
            }

            @Override public void updateStatus(int complaintId, String status) throws SQLException {
                updateStatus(complaintId, status, "");
            }

            @Override public void updateStatus(int complaintId, String status, String note)
                    throws SQLException {
                Authorization.requireAdmin("COMPLAINT_STATUS_UPDATE", "complaint",
                    String.valueOf(complaintId));
                delegate.updateStatus(complaintId, status, note);
                record("COMPLAINT_STATUS_UPDATE", "complaint", complaintId,
                    "Complaint status updated to " + status + ".");
            }

            @Override public List<ComplaintStatusChange> getStatusHistory(int complaintId)
                    throws SQLException {
                User actor = Authorization.requireAuthenticated(
                    "COMPLAINT_HISTORY_READ", "complaint", String.valueOf(complaintId));
                if (!(actor instanceof Admin)
                        && delegate.getComplaintsByStudent(actor.getUserId()).stream()
                            .noneMatch(complaint -> complaint.getComplaintId() == complaintId)) {
                    throw denied("COMPLAINT_HISTORY_READ", "complaint", String.valueOf(complaintId),
                        "Student attempted to access another student's complaint history.",
                        "You may only access your own complaint history.");
                }
                return delegate.getStatusHistory(complaintId);
            }
        };
    }

    static NotificationDAO secure(NotificationDAO delegate) {
        return new NotificationDAO() {
            @Override public List<HostelNotification> getForStudent(int studentId) throws SQLException {
                Authorization.requireStudent("NOTIFICATION_LIST", "notification",
                    String.valueOf(studentId));
                if (SecuritySession.getPrincipal().getUserId() != studentId) {
                    throw denied("NOTIFICATION_LIST", "notification", String.valueOf(studentId),
                        "Student attempted to access another student's notifications.",
                        "You may only access your own notifications.");
                }
                return delegate.getForStudent(studentId);
            }

            @Override public void markRead(int notificationId, int studentId) throws SQLException {
                Authorization.requireStudent("NOTIFICATION_MARK_READ", "notification",
                    String.valueOf(notificationId));
                if (SecuritySession.getPrincipal().getUserId() != studentId) {
                    throw denied("NOTIFICATION_MARK_READ", "notification", String.valueOf(notificationId),
                        "Student attempted to modify another student's notification.",
                        "You may only update your own notifications.");
                }
                delegate.markRead(notificationId, studentId);
                record("NOTIFICATION_MARK_READ", "notification", notificationId,
                    "Notification marked as read.");
            }
        };
    }

    static AuditDAO secure(AuditDAO delegate) {
        return limit -> {
            Authorization.requireAdmin("AUDIT_LOG_READ", "security_audit_log", null);
            return delegate.getRecentEvents(limit);
        };
    }

    private static void record(String action, String entityType, int entityId, String details)
            throws SQLException {
        try {
            AuditLogger.recordAuthorizedAction(action, entityType, String.valueOf(entityId), details);
        } catch (SQLException exception) {
            throw auditFailure(exception);
        }
    }

    private static SQLException auditFailure(SQLException cause) {
        return new SQLException("The operation completed, but its audit record could not be saved. "
            + "Contact an administrator before retrying.", cause);
    }

    private static void recordLoginFailure(String role, String subject, SQLException original) {
        try {
            AuditLogger.recordLogin(role, subject, null, false, "Authentication could not be completed.");
        } catch (SQLException auditFailure) {
            original.addSuppressed(auditFailure);
        }
    }

    private static SQLException denied(String action, String entityType, String entityId,
            String reason, String message) {
        SQLException exception = new SQLException(message);
        try {
            AuditLogger.recordDeniedAction(action, entityType, entityId, reason);
        } catch (SQLException auditFailure) {
            exception.addSuppressed(auditFailure);
        }
        return exception;
    }
}
