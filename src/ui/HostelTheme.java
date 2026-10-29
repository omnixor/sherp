package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.User;
import security.AuditLogger;
import security.SecuritySession;

// Shared professional theme for all Smart Hostel frames.
public final class HostelTheme {
    private static Timer sessionTimer;
    private static AWTEventListener sessionActivityListener;

    // ── Colour palette — clean, professional light theme ─────────────────
    public static final Color BG_PRIMARY    = new Color(244, 247, 251);
    public static final Color BG_SECONDARY  = Color.WHITE;
    public static final Color BG_SURFACE    = new Color(248, 250, 252);
    public static final Color BG_HOVER      = new Color(232, 239, 248);
    public static final Color ACCENT        = new Color(37, 99, 235);
    public static final Color ACCENT_HOVER  = new Color(29, 78, 216);
    public static final Color SUCCESS       = new Color(15, 118, 89);
    public static final Color DANGER        = new Color(185, 54, 54);
    public static final Color WARNING       = new Color(180, 113, 20);
    public static final Color TEXT_PRIMARY   = new Color(23, 43, 77);
    public static final Color TEXT_SECONDARY = new Color(71, 85, 105);
    public static final Color TEXT_MUTED     = new Color(100, 116, 139);
    public static final Color BORDER         = new Color(218, 226, 237);
    public static final Color TABLE_ALT      = new Color(247, 249, 252);
    public static final Color TABLE_SELECT   = new Color(219, 234, 254);
    public static final Color HEADER_BG      = Color.WHITE;
    public static final Color BG_DARK        = BG_PRIMARY;
    public static final Color BG_FIELD       = BG_SURFACE;
    public static final Color BORDER_COLOR   = BORDER;
    public static final Color ACCENT_GREEN   = SUCCESS;
    public static final Color ACCENT_AMBER   = WARNING;

    // ── Fonts ───────────────────────────────────────────────────────────
    public static final Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SECTION   = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL     = new Font("Segoe UI", Font.PLAIN, 11);

    private HostelTheme() {
    }

    public static void startSessionMonitor() {
        stopSessionMonitor();
        sessionActivityListener = event -> SecuritySession.recordActivity();
        Toolkit.getDefaultToolkit().addAWTEventListener(sessionActivityListener,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        sessionTimer = new Timer(15_000, event -> {
            if (SecuritySession.getPrincipal() == null) {
                stopSessionMonitor();
                for (Window window : Window.getWindows()) {
                    if (window.isDisplayable()) {
                        window.dispose();
                    }
                }
                JOptionPane.showMessageDialog(null,
                    "Your session expired after 30 minutes of inactivity. Please sign in again.",
                    "Session expired", JOptionPane.INFORMATION_MESSAGE);
                new LoginFrame().setVisible(true);
            }
        });
        sessionTimer.start();
    }

    private static void stopSessionMonitor() {
        if (sessionTimer != null) {
            sessionTimer.stop();
            sessionTimer = null;
        }
        if (sessionActivityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(sessionActivityListener);
            sessionActivityListener = null;
        }
    }

    // ── Global defaults ─────────────────────────────────────────────────
    public static void applyGlobalDefaults() {
        UIManager.put("OptionPane.background", BG_SECONDARY);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Panel.background", BG_SECONDARY);
        UIManager.put("Button.background", BG_SURFACE);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", BG_SECONDARY);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
    }

    // ── Frame setup ─────────────────────────────────────────────────────
    public static void styleFrame(JFrame frame, String title, int width, int height) {
        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG_PRIMARY);
    }

    public static JPanel createHeaderPanel(String title, String subtitle) {
        return createTopBar(title, subtitle);
    }

    public static void addSignOut(JPanel header) {
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        JButton signOutButton = dangerButton("Sign out");
        signOutButton.setPreferredSize(new Dimension(100, 34));
        actions.add(signOutButton);
        header.add(actions, java.awt.BorderLayout.EAST);
        signOutButton.addActionListener(event -> signOut());
    }

    public static void addAdminActions(JPanel header) {
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton auditButton = secondaryButton("Audit log");
        auditButton.setPreferredSize(new Dimension(110, 34));
        JButton signOutButton = dangerButton("Sign out");
        signOutButton.setPreferredSize(new Dimension(100, 34));
        actions.add(auditButton);
        actions.add(signOutButton);
        header.add(actions, java.awt.BorderLayout.EAST);
        auditButton.addActionListener(event -> new AuditLogFrame().setVisible(true));
        signOutButton.addActionListener(event -> signOut());
    }

    public static void addBackToMenu(JPanel header, Window currentWindow) {
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        JButton backButton = secondaryButton("Back to menu");
        backButton.setPreferredSize(new Dimension(120, 34));
        actions.add(backButton);
        header.add(actions, java.awt.BorderLayout.EAST);
        backButton.addActionListener(event -> returnToMenu(currentWindow));
    }

    public static void returnToMenu(Window currentWindow) {
        Window menu = null;
        for (Window window : Window.getWindows()) {
            if (window != currentWindow && window.isDisplayable()
                    && (window instanceof AdminDashboard || window instanceof StudentDashboard)) {
                menu = window;
                break;
            }
        }
        if (menu == null) {
            for (Window window : Window.getWindows()) {
                if (window != currentWindow && window.isDisplayable() && window instanceof LoginFrame) {
                    menu = window;
                    break;
                }
            }
        }
        if (currentWindow != null) {
            currentWindow.dispose();
        }
        if (menu == null) {
            menu = new LoginFrame();
        }
        menu.setVisible(true);
        menu.toFront();
        menu.requestFocus();
    }

    public static void signOut() {
        stopSessionMonitor();
        User user = SecuritySession.getPrincipal();
        SQLException auditFailure = null;
        if (user != null) {
            try {
                AuditLogger.recordLogout(user);
            } catch (SQLException exception) {
                auditFailure = exception;
            }
        }
        SecuritySession.clear();
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.dispose();
            }
        }
        if (auditFailure != null) {
            JOptionPane.showMessageDialog(null,
                "Your session was signed out, but the logout event could not be recorded: "
                    + auditFailure.getMessage(),
                "Audit logging error", JOptionPane.ERROR_MESSAGE);
        }
        new LoginFrame().setVisible(true);
    }

    public static JPanel createCardPanel() {
        return card();
    }

    public static JLabel createSectionLabel(String text) {
        return sectionLabel(text);
    }

    public static JLabel createLabel(String text) {
        return fieldLabel(text);
    }

    public static JTextField createTextField(String placeholder) {
        JTextField field = new PlaceholderTextField(placeholder);
        applyFieldStyle(field);
        field.setToolTipText(placeholder);
        return field;
    }

    public static JScrollPane createStyledTable(JTable table, DefaultTableModel model) {
        return styledTable(table, model);
    }

    public static JButton createAccentButton(String text) {
        return primaryButton(text);
    }

    public static JButton createSuccessButton(String text) {
        return successButton(text);
    }

    public static JButton createDangerButton(String text) {
        return dangerButton(text);
    }

    public static JButton createOutlineButton(String text) {
        return secondaryButton(text);
    }

    // ── Top bar ─────────────────────────────────────────────────────────
    public static JPanel createTopBar(String title, String subtitle) {
        JPanel bar = new JPanel(new java.awt.BorderLayout());
        bar.setBackground(HEADER_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(18, 28, 18, 28)
        ));

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new javax.swing.BoxLayout(textBlock, javax.swing.BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textBlock.add(titleLabel);

        if (subtitle != null) {
            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(FONT_SUBTITLE);
            subLabel.setForeground(TEXT_MUTED);
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textBlock.add(subLabel);
        }

        bar.add(textBlock, java.awt.BorderLayout.WEST);
        return bar;
    }

    // ── Section label ───────────────────────────────────────────────────
    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SECTION);
        label.setForeground(TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return label;
    }

    // ── Field label ─────────────────────────────────────────────────────
    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    // ── Text field ──────────────────────────────────────────────────────
    public static JTextField textField() {
        JTextField field = new JTextField();
        applyFieldStyle(field);
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        applyFieldStyle(field);
        return field;
    }

    private static void applyFieldStyle(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_SURFACE);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(TABLE_SELECT);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedLineBorder(BORDER, 6),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        field.setPreferredSize(new Dimension(0, 34));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(ACCENT, 6),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(BORDER, 6),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
                ));
            }
        });
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (placeholder == null || placeholder.isEmpty() || hasFocus() || !getText().isEmpty()) {
                return;
            }
            graphics.setColor(TEXT_MUTED);
            graphics.setFont(getFont());
            java.awt.FontMetrics metrics = graphics.getFontMetrics();
            Insets insets = getInsets();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(placeholder, insets.left, y);
        }
    }

    // ── Combo box ───────────────────────────────────────────────────────
    public static void styleComboBox(JComboBox<?> box) {
        box.setFont(FONT_BODY);
        box.setForeground(TEXT_PRIMARY);
        box.setBackground(BG_SURFACE);
        box.setBorder(BorderFactory.createCompoundBorder(
            new RoundedLineBorder(BORDER, 6),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        box.setPreferredSize(new Dimension(0, 34));
    }

    // ── Buttons ─────────────────────────────────────────────────────────
    public static JButton primaryButton(String text) {
        return makeButton(text, ACCENT, Color.WHITE);
    }

    public static JButton successButton(String text) {
        return makeButton(text, SUCCESS, Color.WHITE);
    }

    public static JButton dangerButton(String text) {
        return makeButton(text, DANGER, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        return makeButton(text, BG_SURFACE, TEXT_PRIMARY);
    }

    private static JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = hover ? brighter(bg, 25) : bg;
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 34));
        btn.setMargin(new Insets(6, 18, 6, 18));
        return btn;
    }

    private static Color brighter(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount)
        );
    }

    // ── Table ───────────────────────────────────────────────────────────
    public static JScrollPane styledTable(JTable table, DefaultTableModel model) {
        table.setModel(model);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_SECONDARY);
        table.setSelectionBackground(TABLE_SELECT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? BG_SECONDARY : TABLE_ALT);
                    c.setForeground(TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_SECTION);
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                l.setBackground(HEADER_BG);
                l.setForeground(TEXT_SECONDARY);
                l.setFont(FONT_SECTION);
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)
                ));
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(BG_SECONDARY);
        return sp;
    }

    // ── Card panel ──────────────────────────────────────────────────────
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_SECONDARY);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        return p;
    }

    // ── Rounded line border utility ─────────────────────────────────────
    public static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        public RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }
    }

    public static class RoundedBorder extends RoundedLineBorder {
        public RoundedBorder(Color color, int radius) {
            super(color, radius);
        }
    }
}
