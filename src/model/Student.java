package model;

public class Student extends User {
    private String name;
    private String course;
    private String phone;
    private String email;
    private int roomNumber;

    public Student(int studentId, String name, String course, String phone, int roomNumber) {
        this(studentId, name, course, phone, null, roomNumber);
    }

    public Student(int studentId, String name, String course, String phone, String email, int roomNumber) {
        super(studentId, name);
        this.name = name;
        this.course = course;
        this.phone = phone;
        this.email = email;
        this.roomNumber = roomNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setUsername(name);
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String getRoleDescription() {
        return "Student user";
    }
}
