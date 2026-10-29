package security;

import java.sql.SQLException;
import model.Admin;
import model.Student;
import model.User;

public final class Authorization {
    private Authorization() {
    }

    public static User requireAuthenticated(String action, String entityType, String entityId)
            throws SQLException {
        User actor = SecuritySession.getPrincipal();
        if (actor == null) {
            throw denied(action, entityType, entityId, "Authentication required.",
                "Sign in to perform this operation.");
        }
        SecuritySession.recordActivity();
        return actor;
    }

    public static Admin requireAdmin(String action, String entityType, String entityId)
            throws SQLException {
        User actor = requireAuthenticated(action, entityType, entityId);
        if (!(actor instanceof Admin)) {
            throw denied(action, entityType, entityId, "Administrator permission required.",
                "Administrator permission is required for this operation.");
        }
        return (Admin) actor;
    }

    public static User requireSelfOrAdmin(int studentId, String action, String entityType)
            throws SQLException {
        User actor = requireAuthenticated(action, entityType, String.valueOf(studentId));
        if (actor instanceof Student && actor.getUserId() != studentId) {
            throw denied(action, entityType, String.valueOf(studentId),
                "Student attempted to access another student's record.",
                "You may only access your own records.");
        }
        return actor;
    }

    public static Student requireStudent(String action, String entityType, String entityId)
            throws SQLException {
        User actor = requireAuthenticated(action, entityType, entityId);
        if (!(actor instanceof Student)) {
            throw denied(action, entityType, entityId, "Student permission required.",
                "A student account is required for this operation.");
        }
        return (Student) actor;
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
