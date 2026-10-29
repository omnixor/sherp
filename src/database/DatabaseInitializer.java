package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String catalog = connection.getCatalog();
            ensureColumn(connection, catalog, "students", "password_hash",
                "ALTER TABLE students ADD COLUMN password_hash VARCHAR(255) NULL");
            ensureColumn(connection, catalog, "students", "email",
                "ALTER TABLE students ADD COLUMN email VARCHAR(254) NULL");
            if (!hasIndex(connection.getMetaData(), catalog, "students", "uq_students_email")) {
                execute(connection, "CREATE UNIQUE INDEX uq_students_email ON students(email)");
            }
            ensureColumn(connection, catalog, "complaints", "category",
                "ALTER TABLE complaints ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'Other'");
            ensureColumn(connection, catalog, "complaints", "title",
                "ALTER TABLE complaints ADD COLUMN title VARCHAR(120) NOT NULL DEFAULT 'Complaint'");
            ensureColumn(connection, catalog, "complaints", "submitted_at",
                "ALTER TABLE complaints ADD COLUMN submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(connection, catalog, "complaints", "resolution_note",
                "ALTER TABLE complaints ADD COLUMN resolution_note VARCHAR(1000) NULL");
            if (getColumnSize(connection.getMetaData(), catalog, "admin", "password") < 255) {
                execute(connection, "ALTER TABLE admin MODIFY password VARCHAR(255) NOT NULL");
            }
            execute(connection, "CREATE TABLE IF NOT EXISTS room_allocations (" +
                "allocation_id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "student_id INT NOT NULL, room_number INT NOT NULL, " +
                "allocated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, vacated_at TIMESTAMP NULL, " +
                "active_student_id INT GENERATED ALWAYS AS " +
                    "(CASE WHEN vacated_at IS NULL THEN student_id ELSE NULL END) STORED, " +
                "UNIQUE KEY uq_room_allocations_active_student (active_student_id), " +
                "INDEX ix_room_allocations_student (student_id, allocated_at), " +
                "INDEX ix_room_allocations_room (room_number, vacated_at), " +
                "CONSTRAINT fk_allocations_student FOREIGN KEY (student_id) REFERENCES students(student_id), " +
                "CONSTRAINT fk_allocations_room FOREIGN KEY (room_number) REFERENCES rooms(room_number))");
            execute(connection, "CREATE TABLE IF NOT EXISTS complaint_status_history (" +
                "history_id BIGINT PRIMARY KEY AUTO_INCREMENT, complaint_id INT NOT NULL, " +
                "status VARCHAR(20) NOT NULL, note VARCHAR(1000) NULL, " +
                "changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX ix_complaint_history (complaint_id, changed_at), " +
                "CONSTRAINT fk_complaint_history_complaint FOREIGN KEY (complaint_id) " +
                    "REFERENCES complaints(complaint_id))");
            execute(connection, "CREATE TABLE IF NOT EXISTS notifications (" +
                "notification_id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id INT NOT NULL, " +
                "message VARCHAR(500) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "is_read BOOLEAN NOT NULL DEFAULT FALSE, INDEX ix_notifications_student (student_id, is_read), " +
                "CONSTRAINT fk_notifications_student FOREIGN KEY (student_id) REFERENCES students(student_id))");
            execute(connection, "CREATE TABLE IF NOT EXISTS security_audit_log (" +
                "event_id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_user_id INT NULL, " +
                "actor_role VARCHAR(32) NOT NULL, action VARCHAR(64) NOT NULL, " +
                "entity_type VARCHAR(32) NULL, entity_id VARCHAR(100) NULL, subject VARCHAR(100) NULL, " +
                "outcome VARCHAR(20) NOT NULL, details VARCHAR(500) NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX ix_audit_created_at (created_at), " +
                "INDEX ix_audit_login_rate (action, actor_role, subject, created_at))");
            execute(connection, "INSERT INTO room_allocations (student_id, room_number) " +
                "SELECT s.student_id, s.room_number FROM students s WHERE s.room_number > 0 " +
                "AND NOT EXISTS (SELECT 1 FROM room_allocations a " +
                    "WHERE a.student_id = s.student_id AND a.vacated_at IS NULL)");
            execute(connection, "UPDATE rooms r SET occupied = " +
                "(SELECT COUNT(*) FROM students s WHERE s.room_number = r.room_number)");
            execute(connection, "INSERT INTO complaint_status_history (complaint_id, status, note) " +
                "SELECT c.complaint_id, c.status, 'Imported existing complaint' FROM complaints c " +
                "WHERE NOT EXISTS (SELECT 1 FROM complaint_status_history h " +
                    "WHERE h.complaint_id = c.complaint_id)");
        }
    }

    private static void ensureColumn(Connection connection, String catalog, String table,
            String column, String ddl) throws SQLException {
        if (!hasColumn(connection.getMetaData(), catalog, table, column)) {
            execute(connection, ddl);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static boolean hasColumn(DatabaseMetaData metadata, String catalog,
            String table, String column) throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, table, column)) {
            return resultSet.next();
        }
    }

    private static int getColumnSize(DatabaseMetaData metadata, String catalog,
            String table, String column) throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, table, column)) {
            return resultSet.next() ? resultSet.getInt("COLUMN_SIZE") : 0;
        }
    }

    private static boolean hasIndex(DatabaseMetaData metadata, String catalog,
            String table, String index) throws SQLException {
        try (ResultSet resultSet = metadata.getIndexInfo(catalog, null, table, false, false)) {
            while (resultSet.next()) {
                if (index.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
