package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import model.Admin;
import security.PasswordHasher;

public class AdminDAOImpl implements AdminDAO {
    @Override
    public Admin login(String username, String password) throws SQLException {
        ensureInitialAdmin();
        String sql = "SELECT admin_id, username, password FROM admin WHERE username = ?";
        try (Connection connection = DBConnection.getConnection()) {
            int adminId;
            String storedPassword;
            String adminUsername;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    adminId = resultSet.getInt("admin_id");
                    storedPassword = resultSet.getString("password");
                    adminUsername = resultSet.getString("username");
                }
            }
            if (storedPassword == null || !verifyPassword(password, storedPassword)) {
                return null;
            }
            if (!PasswordHasher.isEncoded(storedPassword)) {
                upgradePassword(connection, adminId, storedPassword, password);
            }
            return new Admin(adminId, adminUsername);
        }
    }

    private void ensureInitialAdmin() throws SQLException {
        String username = System.getenv("HOSTEL_ADMIN_USERNAME");
        String password = System.getenv("HOSTEL_ADMIN_PASSWORD");
        try (Connection connection = DBConnection.getConnection()) {
            int adminCount;
            try (PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) FROM admin");
                 ResultSet resultSet = countStatement.executeQuery()) {
                resultSet.next();
                adminCount = resultSet.getInt(1);
            }
            if (adminCount != 0) {
                return;
            }
            if (username == null || username.trim().isEmpty() || username.trim().length() > 50
                    || password == null || password.length() < 12) {
                throw new SQLException("No administrator exists. Configure HOSTEL_ADMIN_USERNAME and "
                    + "HOSTEL_ADMIN_PASSWORD (at least 12 characters) to create the first account.");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO admin (username, password) VALUES (?, ?)")) {
                insert.setString(1, username.trim());
                insert.setString(2, PasswordHasher.hash(password.toCharArray()));
                insert.executeUpdate();
            } catch (GeneralSecurityException exception) {
                throw new SQLException("Could not securely initialize the administrator account.", exception);
            }
        }
    }

    private boolean verifyPassword(String submitted, String stored) throws SQLException {
        if (submitted == null) {
            return false;
        }
        if (PasswordHasher.isEncoded(stored)) {
            try {
                return PasswordHasher.verify(submitted.toCharArray(), stored);
            } catch (GeneralSecurityException exception) {
                throw new SQLException("Could not verify administrator credentials.", exception);
            }
        }
        return MessageDigest.isEqual(stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            submitted.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void upgradePassword(Connection connection, int adminId, String oldPassword, String password)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE admin SET password = ? WHERE admin_id = ? AND password = ?")) {
            statement.setString(1, PasswordHasher.hash(password.toCharArray()));
            statement.setInt(2, adminId);
            statement.setString(3, oldPassword);
            statement.executeUpdate();
        } catch (GeneralSecurityException exception) {
            throw new SQLException("Could not upgrade the administrator password hash.", exception);
        }
    }
}
