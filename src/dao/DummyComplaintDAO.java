package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Complaint;
import model.ComplaintStatusChange;
import model.HostelNotification;

public class DummyComplaintDAO implements ComplaintDAO {
    @Override
    public void addComplaint(Complaint complaint) throws SQLException {
        if (complaint == null || complaint.getStudentId() <= 0
                || complaint.getDescription() == null || complaint.getDescription().trim().isEmpty()
                || complaint.getDescription().trim().length() > 500
                || complaint.getTitle() == null || complaint.getTitle().trim().isEmpty()
                || complaint.getTitle().trim().length() > 120
                || !isValidCategory(complaint.getCategory())
                || !"Pending".equals(complaint.getStatus())) {
            throw new SQLException("A valid complaint title, category, student, and description are required.");
        }
        complaint.setComplaintId(DummyDatabase.nextComplaintId());
        complaint.setSubmittedAt(LocalDateTime.now());
        DummyDatabase.complaints.add(complaint);
        DummyDatabase.complaintHistory.computeIfAbsent(complaint.getComplaintId(), key -> new ArrayList<>())
            .add(new ComplaintStatusChange("Pending", "", LocalDateTime.now()));
        DummyDatabase.notifications.add(new HostelNotification(DummyDatabase.nextNotificationId(),
            complaint.getStudentId(), "Complaint #" + complaint.getComplaintId() + " was submitted.",
            LocalDateTime.now(), false));
    }

    @Override
    public List<Complaint> getComplaints() {
        return new ArrayList<>(DummyDatabase.complaints);
    }

    @Override
    public List<Complaint> getComplaintsByStudent(int studentId) {
        List<Complaint> results = new ArrayList<>();
        for (Complaint complaint : DummyDatabase.complaints) {
            if (complaint.getStudentId() == studentId) {
                results.add(complaint);
            }
        }
        return results;
    }

    @Override
    public void updateStatus(int complaintId, String status) throws SQLException {
        updateStatus(complaintId, status, "");
    }

    @Override
    public void updateStatus(int complaintId, String status, String note) throws SQLException {
        if (complaintId <= 0 || !isValidStatus(status) || (note != null && note.length() > 1000)
                || ("Resolved".equals(status) && (note == null || note.trim().isEmpty()))) {
            throw new SQLException("Choose a valid complaint and status; a resolution note is required to resolve.");
        }
        for (Complaint complaint : DummyDatabase.complaints) {
            if (complaint.getComplaintId() != complaintId) {
                continue;
            }
            if (!isForwardTransition(complaint.getStatus(), status)) {
                throw new SQLException("Complaint status cannot be reopened or moved backwards.");
            }
            if (complaint.getStatus().equals(status)) {
                if (note != null && !note.trim().isEmpty()) {
                    complaint.setResolutionNote(note.trim());
                }
                return;
            }
            complaint.setStatus(status);
            complaint.setResolutionNote(note == null || note.trim().isEmpty() ? null : note.trim());
            DummyDatabase.complaintHistory.computeIfAbsent(complaintId, key -> new ArrayList<>())
                .add(new ComplaintStatusChange(status, note, LocalDateTime.now()));
            DummyDatabase.notifications.add(new HostelNotification(DummyDatabase.nextNotificationId(),
                complaint.getStudentId(), "Complaint #" + complaintId + " is now " + status + ".",
                LocalDateTime.now(), false));
            return;
        }
        throw new SQLException("Complaint was not found.");
    }

    @Override
    public List<ComplaintStatusChange> getStatusHistory(int complaintId) {
        return new ArrayList<>(DummyDatabase.complaintHistory.getOrDefault(complaintId, new ArrayList<>()));
    }

    private boolean isValidCategory(String category) {
        return "Maintenance".equals(category) || "Plumbing".equals(category)
            || "Electricity".equals(category) || "Cleanliness".equals(category)
            || "Security".equals(category) || "Other".equals(category);
    }

    private boolean isValidStatus(String status) {
        return "Pending".equals(status) || "In Progress".equals(status) || "Resolved".equals(status);
    }

    private boolean isForwardTransition(String current, String next) {
        return current.equals(next) || ("Pending".equals(current)
            && ("In Progress".equals(next) || "Resolved".equals(next)))
            || ("In Progress".equals(current) && "Resolved".equals(next));
    }
}
