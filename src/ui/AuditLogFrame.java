package ui;

import dao.DAOFactory;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import model.AuditEvent;

public class AuditLogFrame extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Time", "Actor ID", "Role", "Action", "Entity", "Record ID",
            "Subject", "Outcome", "Details"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public AuditLogFrame() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HostelTheme.styleFrame(this, "Security Audit Log", 1120, 620);
        setLayout(new BorderLayout(0, 10));
        JPanel header = HostelTheme.createHeaderPanel("Security Audit Log",
            "Recent authentication, authorization, and data-change events");
        HostelTheme.addBackToMenu(header, this);
        add(header, BorderLayout.NORTH);
        JTable table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        add(HostelTheme.createStyledTable(table, tableModel), BorderLayout.CENTER);
        loadEvents();
    }

    private void loadEvents() {
        new SwingWorker<List<AuditEvent>, Void>() {
            @Override
            protected List<AuditEvent> doInBackground() throws SQLException {
                return DAOFactory.auditDAO().getRecentEvents(200);
            }

            @Override
            protected void done() {
                try {
                    for (AuditEvent event : get()) {
                        tableModel.addRow(new Object[] {event.getCreatedAt(), event.getActorId(),
                            event.getActorRole(), event.getAction(), event.getEntityType(),
                            event.getEntityId(), event.getSubject(), event.getOutcome(),
                            event.getDetails()});
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(AuditLogFrame.this,
                        "Loading the audit log was interrupted.", "Audit log error",
                        JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(AuditLogFrame.this,
                        "Could not load the audit log: " + exception.getCause().getMessage(),
                        "Audit log error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
