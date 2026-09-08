package dao;

import database.DummyDatabase;
import model.Admin;

public class DummyAdminDAO implements AdminDAO {
    @Override
    public Admin login(String username, String password) {
        for (Admin admin : DummyDatabase.admins) {
            if (admin.getUsername().equals(username) && admin.getPassword().equals(password)) {
                return admin;
            }
        }
        return null;
    }
}
