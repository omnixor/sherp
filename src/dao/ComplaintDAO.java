package dao;

import java.sql.SQLException;
import java.util.List;
import model.Complaint;
import model.ComplaintStatusChange;

public interface ComplaintDAO {
    void addComplaint(Complaint complaint) throws SQLException;
    List<Complaint> getComplaints() throws SQLException;
    List<Complaint> getComplaintsByStudent(int studentId) throws SQLException;
    void updateStatus(int complaintId, String status) throws SQLException;
    void updateStatus(int complaintId, String status, String note) throws SQLException;
    List<ComplaintStatusChange> getStatusHistory(int complaintId) throws SQLException;
}
