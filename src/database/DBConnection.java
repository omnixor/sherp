package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = valueOrDefault(
        "HOSTEL_DB_URL", "jdbc:mysql://localhost:3306/smart_hostel");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String username = System.getenv("HOSTEL_DB_USER");
        String password = System.getenv("HOSTEL_DB_PASSWORD");
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new SQLException("Set HOSTEL_DB_USER and HOSTEL_DB_PASSWORD before connecting.");
        }
        return DriverManager.getConnection(URL, username, password);
    }

    private static String valueOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
