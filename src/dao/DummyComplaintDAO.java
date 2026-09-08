package dao;

import database.DummyDatabase;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Complaint;

public class DummyComplaintDAO implements ComplaintDAO {
    @Override
    public void addComplaint(Complaint complaint) {
        complaint.setComplaintId(DummyDatabase.nextComplaintId());
        DummyDatabase.complaints.add(complaint);
    }

    @Override
    public List<Complaint> getComplaints() {
        return new ArrayList<>(DummyDatabase.complaints);
    }

    @Override
    public List<Complaint> getComplaintsByStudent(int studentId) {
        List<Complaint> studentComplaints = new ArrayList<>();
        for (Complaint complaint : DummyDatabase.complaints) {
            if (complaint.getStudentId() == studentId) {
                studentComplaints.add(complaint);
            }
        }
        return studentComplaints;
    }

    @Override
    public void updateStatus(int complaintId, String status) throws SQLException {
        for (Complaint complaint : DummyDatabase.complaints) {
            if (complaint.getComplaintId() == complaintId) {
                complaint.setStatus(status);
                return;
            }
        }
        throw new SQLException("Complaint was not found.");
    }
}
