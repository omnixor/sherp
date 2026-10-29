package model;

import java.time.LocalDateTime;

public class Complaint {
    private int complaintId;
    private int studentId;
    private String description;
    private String title;
    private String status;
    private String category;
    private LocalDateTime submittedAt;
    private String resolutionNote;

    public Complaint(int complaintId, int studentId, String description, String status) {
        this(complaintId, studentId, "Other", description, status, null, null);
    }

    public Complaint(int complaintId, int studentId, String category, String description,
            String status, LocalDateTime submittedAt, String resolutionNote) {
        this(complaintId, studentId, category, "Complaint", description, status, submittedAt, resolutionNote);
    }

    public Complaint(int complaintId, int studentId, String category, String title, String description,
            String status, LocalDateTime submittedAt, String resolutionNote) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.description = description;
        this.title = title;
        this.status = status;
        this.category = category;
        this.submittedAt = submittedAt;
        this.resolutionNote = resolutionNote;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
