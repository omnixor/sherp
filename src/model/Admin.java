package model;

public class Admin extends User {
    public Admin(int adminId, String username) {
        super(adminId, username);
    }

    @Override
    public String getRoleDescription() {
        return "Administrator user";
    }
}
