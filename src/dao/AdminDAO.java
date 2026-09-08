package dao;

import java.sql.SQLException;
import model.Admin;

public interface AdminDAO {
    Admin login(String username, String password) throws SQLException;
}
