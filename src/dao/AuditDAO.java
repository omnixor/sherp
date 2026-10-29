package dao;

import java.sql.SQLException;
import java.util.List;
import model.AuditEvent;

public interface AuditDAO {
    List<AuditEvent> getRecentEvents(int limit) throws SQLException;
}
