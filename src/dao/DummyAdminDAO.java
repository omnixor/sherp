package dao;

import java.security.MessageDigest;
import model.Admin;

public class DummyAdminDAO implements AdminDAO {
    @Override
    public Admin login(String username, String password) {
        String configuredUsername = System.getenv("HOSTEL_DEMO_ADMIN_USERNAME");
        String configuredPassword = System.getenv("HOSTEL_DEMO_ADMIN_PASSWORD");
        if (configuredUsername != null && configuredPassword != null && configuredPassword.length() >= 12
                && configuredUsername.equals(username)
                && MessageDigest.isEqual(configuredPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return new Admin(1, configuredUsername);
        }
        return null;
    }
}
