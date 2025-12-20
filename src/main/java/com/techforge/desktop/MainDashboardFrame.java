package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.JsonObject;

/**
 * Main Dashboard Frame - Modern role-based UI with polished sidebar.
 */
public class MainDashboardFrame extends JFrame {

    private final String userName;
    private final String userRole;
    private final ApiClient apiClient;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel sidebarPanel;

    // Sidebar nav buttons
    private JButton btnProjectPlanning;
    private JButton btnExecution;
    private JButton btnPayroll;
    private JButton btnMonitoring;

    private String currentView = "";

    // Panel reference for refresh on view switch
    private EmployeePanel employeePanelRef;

    // Sidebar profile UI references for synchronization
    private JLabel sidebarNameLabel;
    private JLabel sidebarAvatarLabel;

    public MainDashboardFrame(String userName, ApiClient apiClient) {
        this.userName = userName;
        this.apiClient = apiClient;

        JsonObject currentUser = ApiClient.getCurrentUser();
        this.userRole = currentUser != null && currentUser.has("role")
                ? currentUser.get("role").getAsString()
                : "EMPLOYEE";

        initializeUI();
        switchToRoleDefaultView();
    }

    private void initializeUI() {
        setTitle("TechForge ERP - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 700));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(AppTheme.BG_LIGHT);

        // Create sidebar
        createSidebar();
        mainPanel.add(sidebarPanel, BorderLayout.WEST);

        // Create content area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_LIGHT);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Initialize panels and store references for refresh
        ManagerPanel managerPanel = new ManagerPanel(apiClient);
        EmployeePanel employeePanel = new EmployeePanel(apiClient);
        AdminPanel adminPanel = new AdminPanel(apiClient);
        ClientPanel clientPanel = new ClientPanel(apiClient);

        contentPanel.add(managerPanel, "manager");
        contentPanel.add(employeePanel, "employee");
        contentPanel.add(adminPanel, "admin");
        contentPanel.add(clientPanel, "client");

        // Store employee panel reference for refresh on view switch
        this.employeePanelRef = employeePanel;

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private void createSidebar() {
        // Initialize sidebar with gradient background
        sidebarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                        0, 0, AppTheme.SIDEBAR_BG,
                        0, getHeight(), new Color(0x03, 0x0A, 0x30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sidebarPanel.setPreferredSize(new Dimension(AppTheme.SIDEBAR_WIDTH, 0));
        sidebarPanel.setLayout(new BorderLayout());

        // Top Section: Logo + Navigation (using CENTER for proper layout)
        JPanel centerSection = new JPanel();
        centerSection.setOpaque(false);
        centerSection.setLayout(new BoxLayout(centerSection, BoxLayout.Y_AXIS));
        centerSection.setBorder(BorderFactory.createEmptyBorder(25, 20, 20, 20));

        // Logo with high-quality Dragon icon
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dragonIcon = new JLabel("🐉");
        dragonIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
        dragonIcon.setForeground(AppTheme.PRIMARY);
        logoPanel.add(dragonIcon);

        JLabel brand = new JLabel("TechForge");
        brand.setFont(AppTheme.fontHeading(28));
        brand.setForeground(AppTheme.PRIMARY);
        logoPanel.add(brand);

        centerSection.add(logoPanel);
        centerSection.add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("Saiyan Edition");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        subtitle.setForeground(new Color(255, 255, 255, 150));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        centerSection.add(subtitle);
        centerSection.add(Box.createVerticalStrut(35));

        // Navigation Section
        JLabel navLabel = new JLabel("MAIN MENU");
        navLabel.setFont(AppTheme.fontMain(Font.BOLD, 11));
        navLabel.setForeground(new Color(255, 255, 255, 100));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerSection.add(navLabel);
        centerSection.add(Box.createVerticalStrut(15));

        // Create navigation buttons with proper icons
        btnProjectPlanning = createNavButton("Project Planning", "manager");
        btnProjectPlanning.setIcon(UIUtils.getFolderIcon(20));

        btnExecution = createNavButton("Execution", "employee");
        btnExecution.setIcon(UIUtils.getCodeIcon(20));

        btnPayroll = createNavButton("Payroll", "admin");
        btnPayroll.setIcon(UIUtils.getMoneyIcon(20));

        btnMonitoring = createNavButton("Monitoring", "client");
        btnMonitoring.setIcon(UIUtils.getChartIcon(20));

        centerSection.add(btnProjectPlanning);
        centerSection.add(Box.createVerticalStrut(8));
        centerSection.add(btnExecution);
        centerSection.add(Box.createVerticalStrut(8));
        centerSection.add(btnPayroll);
        centerSection.add(Box.createVerticalStrut(8));
        centerSection.add(btnMonitoring);

        // Add flexible space to push content to top
        centerSection.add(Box.createVerticalGlue());

        // Configure visibility based on role
        configureNavButtonsForRole(userRole);

        sidebarPanel.add(centerSection, BorderLayout.CENTER);

        // Bottom Section: User Profile (pinned to SOUTH)
        JPanel bottomSection = createUserProfileSection();
        sidebarPanel.add(bottomSection, BorderLayout.SOUTH);
    }

    private void configureNavButtonsForRole(String role) {
        btnProjectPlanning.setVisible(false);
        btnExecution.setVisible(false);
        btnPayroll.setVisible(false);
        btnMonitoring.setVisible(false);

        if (role == null) role = "EMPLOYEE";

        switch (role.toUpperCase()) {
            case "MANAGER":
                btnProjectPlanning.setVisible(true);
                btnExecution.setVisible(true);
                break;
            case "EMPLOYEE":
                btnExecution.setVisible(true);
                break;
            case "ADMIN":
                btnProjectPlanning.setVisible(true);
                btnPayroll.setVisible(true);
                btnExecution.setVisible(true);
                break;
            case "CLIENT":
                btnMonitoring.setVisible(true);
                break;
            default:
                btnExecution.setVisible(true);
                break;
        }

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private JButton createNavButton(String text, String viewName) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Active state: Bright orange rounded background
                if (currentView.equals(viewName)) {
                    g2.setColor(AppTheme.PRIMARY);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                }
                // Hover state: Subtle white overlay
                else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setFont(AppTheme.fontMain(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(240, 48));
        button.setPreferredSize(new Dimension(240, 48));
        button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> switchView(viewName));

        return button;
    }

    private JPanel createUserProfileSection() {
        // Create panel with gradient background
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Gradient from dark navy to slightly lighter navy
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x03, 0x0A, 0x30),
                        0, getHeight(), new Color(0x05, 0x15, 0x50)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Top border line (1px solid)
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawLine(0, 0, getWidth(), 0);

                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setPreferredSize(new Dimension(AppTheme.SIDEBAR_WIDTH, 140));

        // User info container
        JPanel userInfo = new JPanel(new BorderLayout(12, 0));
        userInfo.setOpaque(false);
        userInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        userInfo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Avatar with perfect circle using UIUtils
        String avatarFileName;
        switch (userRole != null ? userRole.toUpperCase() : "EMPLOYEE") {
            case "MANAGER":
                avatarFileName = "vegeta.png";
                break;
            case "ADMIN":
                avatarFileName = "bulma.png";
                break;
            case "CLIENT":
                avatarFileName = "frieza.png";
                break;
            case "EMPLOYEE":
            default:
                avatarFileName = "goku.png";
                break;
        }

        // Use UIUtils to create perfect circular avatar with border
        ImageIcon avatarIcon = UIUtils.createCircleImage("/assets/" + avatarFileName, 50);
        JLabel avatar = new JLabel(avatarIcon);
        avatar.setPreferredSize(new Dimension(50, 50));
        userInfo.add(avatar, BorderLayout.WEST);
        this.sidebarAvatarLabel = avatar; // Store reference for sync

        // Name and role
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(userName);
        nameLabel.setFont(AppTheme.fontMain(Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        textPanel.add(nameLabel);
        this.sidebarNameLabel = nameLabel; // Store reference for sync
        textPanel.add(Box.createVerticalStrut(3));

        // Role badge - small colored pill
        JLabel roleLabel = new JLabel(userRole) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roleLabel.setFont(AppTheme.fontMain(Font.BOLD, 10));
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setOpaque(false);
        roleLabel.setBackground(getRoleBadgeColor(userRole));
        roleLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        textPanel.add(roleLabel);

        userInfo.add(textPanel, BorderLayout.CENTER);

        // Edit icon on the right
        JLabel editIcon = new JLabel("✏️");
        editIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        editIcon.setForeground(new Color(255, 255, 255, 100));
        userInfo.add(editIcon, BorderLayout.EAST);

        // Make user info clickable to open ProfileDialog
        userInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        userInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProfileDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                userInfo.setBackground(new Color(255, 255, 255, 20));
                userInfo.setOpaque(true);
                editIcon.setForeground(new Color(255, 255, 255, 200));
                userInfo.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                userInfo.setOpaque(false);
                editIcon.setForeground(new Color(255, 255, 255, 100));
                userInfo.repaint();
            }
        });

        panel.add(userInfo);
        panel.add(Box.createVerticalStrut(12));

        // Logout button - outlined style
        JButton logoutBtn = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Subtle border
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 10, 10));

                // Hover fill
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 10, 10));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        logoutBtn.setForeground(new Color(255, 255, 255, 180));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setMaximumSize(new Dimension(240, 40));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> handleLogout());
        panel.add(logoutBtn);

        return panel;
    }

    private void switchView(String viewName) {
        currentView = viewName;
        cardLayout.show(contentPanel, viewName);
        sidebarPanel.repaint();

        // Refresh EmployeePanel when switching to it (for Kanban sync)
        if ("employee".equals(viewName) && employeePanelRef != null) {
            employeePanelRef.refreshTasks();
        }
    }

    private void switchToRoleDefaultView() {
        String roleUpper = userRole != null ? userRole.toUpperCase() : "EMPLOYEE";

        switch (roleUpper) {
            case "MANAGER":
                switchView("manager");
                break;
            case "EMPLOYEE":
                switchView("employee");
                break;
            case "ADMIN":
                switchView("admin");
                break;
            case "CLIENT":
                switchView("client");
                break;
            default:
                switchView("employee");
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            apiClient.logout();
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }

    private void openProfileDialog() {
        // Pass callback to update sidebar when profile is saved
        UserProfileDialog dialog = new UserProfileDialog(this, apiClient, this::updateSidebarProfile);
        dialog.setVisible(true);
    }

    /**
     * Callback to update sidebar profile after UserProfileDialog saves changes.
     */
    private void updateSidebarProfile(com.google.gson.JsonObject updatedUser) {
        if (updatedUser == null) return;

        SwingUtilities.invokeLater(() -> {
            // Update name label
            if (sidebarNameLabel != null && updatedUser.has("fullName")) {
                String newName = updatedUser.get("fullName").getAsString();
                sidebarNameLabel.setText(newName);
            }

            // Repaint sidebar to reflect changes
            if (sidebarPanel != null) {
                sidebarPanel.repaint();
            }

            System.out.println("Sidebar profile updated successfully");
        });
    }

    private Color getRoleBadgeColor(String role) {
        if (role == null) return AppTheme.TEXT_LIGHT;
        switch (role.toUpperCase()) {
            case "MANAGER":
                return new Color(220, 38, 38); // Red
            case "ADMIN":
                return new Color(147, 51, 234); // Purple
            case "EMPLOYEE":
                return new Color(59, 130, 246); // Blue
            case "CLIENT":
                return new Color(16, 185, 129); // Green
            default:
                return AppTheme.TEXT_LIGHT;
        }
    }
}

