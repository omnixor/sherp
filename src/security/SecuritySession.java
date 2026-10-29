package security;

import java.time.Duration;
import model.Admin;
import model.Student;
import model.User;

public final class SecuritySession {
    private static final long IDLE_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();
    private static User principal;
    private static long lastActivity;

    private SecuritySession() {
    }

    public static synchronized void start(User user) {
        if (!(user instanceof Admin) && !(user instanceof Student)) {
            throw new IllegalArgumentException("Only authenticated administrators or students may start a session.");
        }
        principal = user;
        lastActivity = System.currentTimeMillis();
    }

    public static synchronized User getPrincipal() {
        if (principal != null && System.currentTimeMillis() - lastActivity >= IDLE_TIMEOUT_MILLIS) {
            principal = null;
            lastActivity = 0;
        }
        return principal;
    }

    public static synchronized void clear() {
        principal = null;
        lastActivity = 0;
    }

    public static synchronized void recordActivity() {
        if (getPrincipal() != null) {
            lastActivity = System.currentTimeMillis();
        }
    }
}
