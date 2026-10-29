import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import database.DatabaseInitializer;
import ui.LoginFrame;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        if (!dao.DAOFactory.isDemoMode()) {
            try {
                DatabaseInitializer.initialize();
            } catch (SQLException exception) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Database initialization failed. Check the database and HOSTEL_DB_* settings.\n"
                        + exception.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE));
                return;
            }
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
