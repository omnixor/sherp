package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Admin;

public class AdminDAOImpl implements AdminDAO {
    @Override
    public Admin login(String username, String password) throws SQLException {
        String sql = "SELECT admin_id, username, password FROM admin WHERE username = ? AND password = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Admin(
                        resultSet.getInt("admin_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password")
                    );
                }
            }
        }
        return null;
    }
}
