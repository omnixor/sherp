package model;

public class Student extends User {
    private String name;
    private String course;
    private String phone;
    private int roomNumber;

    public Student(int studentId, String name, String course, String phone, int roomNumber) {
        super(studentId, name);
        this.name = name;
        this.course = course;
        this.phone = phone;
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
