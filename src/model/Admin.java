package model;

public class Admin extends User {
    private String password;

    public Admin(int adminId, String username, String password) {
        super(adminId, username);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getRoleDescription() {
        return "Administrator user";
    }
}
