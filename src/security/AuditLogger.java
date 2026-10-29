package security;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Admin;
import model.AuditEvent;
import model.User;

public final class AuditLogger {
    private static final int MAX_FAILED_LOGINS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;

    private AuditLogger() {
    }

    public static boolean isLoginRateLimited(String role, String subject) throws SQLException {
        if (demoMode()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM security_audit_log " +
            "WHERE action = 'LOGIN_FAILURE' AND actor_role = ? AND subject = ? " +
            "AND created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MINUTE)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalize(role));
            statement.setString(2, normalize(subject));
            statement.setInt(3, LOGIN_WINDOW_MINUTES);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) >= MAX_FAILED_LOGINS;
            }
        }
    }

    public static void recordLogin(String role, String subject, Integer actorId,
            boolean succeeded, String details) throws SQLException {
        record(actorId, role, succeeded ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
            "authentication", null, subject,
            succeeded ? "SUCCESS" : "DENIED", details);
    }

    public static void recordRateLimitedLogin(String role, String subject) throws SQLException {
        record(null, role, "LOGIN_RATE_LIMITED", "authentication", null, subject,
            "DENIED", "Login temporarily blocked after repeated failures.");
    }

    public static void recordLogout(User user) throws SQLException {
        record(user.getUserId(), roleOf(user), "LOGOUT", "session",
            String.valueOf(user.getUserId()), null, "SUCCESS", "User signed out.");
    }

    public static void recordAuthorizedAction(String action, String entityType, String entityId,
            String details) throws SQLException {
        User actor = SecuritySession.getPrincipal();
        if (actor == null) {
            throw new SQLException("No authenticated user is available for the audit record.");
        }
        SecuritySession.recordActivity();
        record(actor.getUserId(), roleOf(actor), action, entityType, entityId, null,
            "SUCCESS", details);
    }

    public static void recordPublicAction(String action, String entityType, String entityId,
            String details) throws SQLException {
        record(null, "PUBLIC", action, entityType, entityId, null, "SUCCESS", details);
    }

    public static void recordDeniedAction(String action, String entityType, String entityId,
            String reason) throws SQLException {
        User actor = SecuritySession.getPrincipal();
        record(actor == null ? null : actor.getUserId(),
            actor == null ? "UNKNOWN" : roleOf(actor), action, entityType,
            entityId, null, "DENIED", reason);
    }

    public static List<AuditEvent> getRecentEvents(int limit) throws SQLException {
        if (limit < 1 || limit > 500) {
            throw new SQLException("Audit event limit must be between 1 and 500.");
        }
        List<AuditEvent> events = new ArrayList<>();
        String sql = "SELECT event_id, actor_user_id, actor_role, action, entity_type, entity_id, " +
            "subject, outcome, details, created_at FROM security_audit_log " +
            "ORDER BY event_id DESC LIMIT ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    events.add(new AuditEvent(resultSet.getLong("event_id"),
                        (Integer) resultSet.getObject("actor_user_id"),
                        resultSet.getString("actor_role"), resultSet.getString("action"),
                        resultSet.getString("entity_type"), resultSet.getString("entity_id"),
                        resultSet.getString("subject"), resultSet.getString("outcome"),
                        resultSet.getString("details"),
                        createdAt == null ? null : createdAt.toLocalDateTime()));
                }
            }
        }
        return events;
    }

    private static void record(Integer actorId, String actorRole, String action,
            String entityType, String entityId, String subject, String outcome,
            String details) throws SQLException {
        if (demoMode()) {
            return;
        }
        String sql = "INSERT INTO security_audit_log " +
            "(actor_user_id, actor_role, action, entity_type, entity_id, subject, outcome, details) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, actorId);
            }
            statement.setString(2, safe(actorRole, 32));
            statement.setString(3, safe(action, 64));
            statement.setString(4, safe(entityType, 32));
            statement.setString(5, safe(entityId, 100));
            statement.setString(6, safe(subject, 100));
            statement.setString(7, safe(outcome, 20));
            statement.setString(8, safe(details, 500));
            statement.executeUpdate();
        }
    }

    private static String safe(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private static String normalize(String value) {
        return safe(value, 100).toLowerCase(Locale.ROOT);
    }

    private static boolean demoMode() {
        return Boolean.parseBoolean(System.getenv("HOSTEL_DEMO_MODE"));
    }

    private static String roleOf(User user) {
        return user instanceof Admin ? "ADMIN" : "STUDENT";
    }
}
