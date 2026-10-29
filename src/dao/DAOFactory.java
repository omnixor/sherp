package dao;

public final class DAOFactory {
    private static final boolean DEMO_MODE =
        Boolean.parseBoolean(System.getenv("HOSTEL_DEMO_MODE"));

    private DAOFactory() {
    }

    public static AdminDAO adminDAO() {
        return SecuredDAOs.secure(DEMO_MODE ? new DummyAdminDAO() : new AdminDAOImpl());
    }

    public static StudentDAO studentDAO() {
        return SecuredDAOs.secure(DEMO_MODE ? new DummyStudentDAO() : new StudentDAOImpl());
    }

    public static RoomDAO roomDAO() {
        return SecuredDAOs.secure(DEMO_MODE ? new DummyRoomDAO() : new RoomDAOImpl());
    }

    public static ComplaintDAO complaintDAO() {
        return SecuredDAOs.secure(DEMO_MODE ? new DummyComplaintDAO() : new ComplaintDAOImpl());
    }

    public static NotificationDAO notificationDAO() {
        return SecuredDAOs.secure(DEMO_MODE ? new DummyNotificationDAO() : new NotificationDAOImpl());
    }

    public static AuditDAO auditDAO() {
        return SecuredDAOs.secure(limit -> security.AuditLogger.getRecentEvents(limit));
    }

    public static boolean isDemoMode() {
        return DEMO_MODE;
    }
}
