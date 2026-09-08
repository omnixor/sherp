package model;

public class Complaint {
    private int complaintId;
    private int studentId;
    private String description;
    private String status;

    public Complaint(int complaintId, int studentId, String description, String status) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.description = description;
        this.status = status;
    }

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
