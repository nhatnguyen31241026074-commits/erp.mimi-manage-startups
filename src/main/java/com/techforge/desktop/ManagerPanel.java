package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.*;

/**
 * ManagerPanel - Project Planning Dashboard.
 * Clean rewrite with proper BorderLayout structure.
 *
 * Layout:
 * - NORTH: Dashboard Header with Stat Cards
 * - CENTER: Project Table (JScrollPane + JTable)
 */
public class ManagerPanel extends JPanel {

    private final ApiClient apiClient;
    private DefaultTableModel tableModel;

    // Stat card labels
    private JLabel statActiveProjects;
    private JLabel statTotalBudget;
    private JLabel statPendingTasks;
    private JLabel statusLabel;

    // Store project IDs for table rows
    private java.util.List<String> projectIds = new ArrayList<>();

    public ManagerPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        initializeUI();
        loadProjects();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // NORTH: Dashboard Header
        add(createDashboardHeader(), BorderLayout.NORTH);

        // CENTER: Project Table
        add(createProjectTablePanel(), BorderLayout.CENTER);
    }

    // ============================================
    // DASHBOARD HEADER (Stat Cards + New Project Button)
    // ============================================

    private JPanel createDashboardHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 20));
        header.setOpaque(false);

        // Title Row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        // Title with accent bar
        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setOpaque(false);

        JLabel title = new JLabel("Project Planning");
        title.setFont(AppTheme.fontHeading(28));
        title.setForeground(AppTheme.SECONDARY);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, AppTheme.PRIMARY),
                BorderFactory.createEmptyBorder(0, 15, 0, 0)
        ));
        titleSection.add(title);

        JLabel subtitle = new JLabel("Manage budgets, timelines, and team assignments");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_LIGHT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 19, 0, 0));
        titleSection.add(subtitle);

        titleRow.add(titleSection, BorderLayout.WEST);

        // Buttons Panel (Export + New Project)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        // Export Report Button
        JButton exportBtn = createOutlineButton("📄 Export Report");
        exportBtn.setPreferredSize(new Dimension(140, 45));
        exportBtn.addActionListener(e -> showExportReportDialog());
        buttonsPanel.add(exportBtn);

        // New Project Button
        JButton newProjectBtn = createPrimaryButton("+ New Project");
        newProjectBtn.setPreferredSize(new Dimension(150, 45));
        newProjectBtn.addActionListener(e -> openProjectDialog());
        buttonsPanel.add(newProjectBtn);

        titleRow.add(buttonsPanel, BorderLayout.EAST);

        header.add(titleRow, BorderLayout.NORTH);

        // Stat Cards Row
        header.add(createStatCardsPanel(), BorderLayout.CENTER);

        return header;
    }

    private JPanel createStatCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 100));

        // Card 1: Active Projects
        statActiveProjects = new JLabel("0");
        panel.add(createStatCard("Active Projects", statActiveProjects, AppTheme.SUCCESS, "📊"));

        // Card 2: Total Budget
        statTotalBudget = new JLabel("$0");
        panel.add(createStatCard("Total Budget", statTotalBudget, AppTheme.PRIMARY, "💰"));

        // Card 3: Pending Tasks
        statPendingTasks = new JLabel("0");
        panel.add(createStatCard("Pending Tasks", statPendingTasks, AppTheme.WARNING, "📋"));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor, String icon) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // White background with rounded corners
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

                // Left accent bar
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(15, 0));
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(AppTheme.fontEmoji(28));
        card.add(iconLabel, BorderLayout.WEST);

        // Text content
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        valueLabel.setFont(AppTheme.fontMain(Font.BOLD, 24));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(valueLabel);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        titleLabel.setForeground(AppTheme.TEXT_LIGHT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);

        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    // ============================================
    // PROJECT TABLE PANEL
    // ============================================

    private JPanel createProjectTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Table Header Bar
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("📁 Project Hub");
        tableTitle.setFont(AppTheme.fontMain(Font.BOLD, 16));
        tableTitle.setForeground(AppTheme.SECONDARY);
        tableHeader.add(tableTitle, BorderLayout.WEST);

        // Status label
        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(AppTheme.fontMain(Font.ITALIC, 12));
        statusLabel.setForeground(AppTheme.TEXT_LIGHT);
        tableHeader.add(statusLabel, BorderLayout.EAST);

        panel.add(tableHeader, BorderLayout.NORTH);

        // Create Table - Added "Risk" column to integrate RiskAlert class
        String[] columns = {"Project Name", "Client", "Budget", "Deadline", "Status", "Risk", "Action", "Edit", "Delete"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6 || column == 7 || column == 8; // Action, Edit and Delete columns are editable (for button clicks)
            }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Project Name
        table.getColumnModel().getColumn(1).setPreferredWidth(85);  // Client
        table.getColumnModel().getColumn(2).setPreferredWidth(75);  // Budget
        table.getColumnModel().getColumn(3).setPreferredWidth(85);  // Deadline
        table.getColumnModel().getColumn(4).setPreferredWidth(75);  // Status
        table.getColumnModel().getColumn(5).setPreferredWidth(60);  // Risk
        table.getColumnModel().getColumn(6).setPreferredWidth(80);  // Action
        table.getColumnModel().getColumn(7).setPreferredWidth(60);  // Edit
        table.getColumnModel().getColumn(8).setPreferredWidth(70);  // Delete

        // Custom renderers
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new RiskBadgeRenderer());  // NEW: Risk column renderer
        table.getColumnModel().getColumn(6).setCellRenderer(new ManageButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ManageButtonEditor(table));
        table.getColumnModel().getColumn(7).setCellRenderer(new EditButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new EditButtonEditor(table));
        table.getColumnModel().getColumn(8).setCellRenderer(new DeleteButtonRenderer());
        table.getColumnModel().getColumn(8).setCellEditor(new DeleteButtonEditor(table));

        // Wrap in scroll pane with white background
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Card wrapper for table
        JPanel cardWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        cardWrapper.add(scrollPane, BorderLayout.CENTER);

        panel.add(cardWrapper, BorderLayout.CENTER);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(50);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 245, 235));
        table.setSelectionForeground(AppTheme.TEXT_MAIN);
        table.setFont(AppTheme.fontMain(Font.PLAIN, 13));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(0xF9, 0xFA, 0xFB));
        header.setForeground(AppTheme.SECONDARY);
        header.setFont(AppTheme.fontMain(Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 45));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER));

        // Remove focus border
        table.setFocusable(false);
    }

    // ============================================
    // DATA LOADING
    // ============================================

    private void loadProjects() {
        statusLabel.setText("Loading projects...");
        tableModel.setRowCount(0);
        projectIds.clear();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.get("/projects");
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    JsonArray projects = JsonParser.parseString(response).getAsJsonArray();

                    double totalBudget = 0;
                    int activeCount = 0;

                    for (JsonElement elem : projects) {
                        JsonObject proj = elem.getAsJsonObject();

                        String id = getJsonString(proj, "id");
                        String name = getJsonString(proj, "name");
                        String client = getJsonString(proj, "clientId");
                        double budget = proj.has("budget") && !proj.get("budget").isJsonNull()
                                ? proj.get("budget").getAsDouble() : 0;
                        String endDate = getJsonString(proj, "endDate");
                        String status = getJsonString(proj, "status");

                        totalBudget += budget;
                        if ("ACTIVE".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status)) {
                            activeCount++;
                        }

                        // Calculate Risk Level (integrates RiskAlert concept)
                        String riskLevel = calculateProjectRisk(id, endDate, status);

                        // Store project ID
                        projectIds.add(id);

                        // Add row to table (now with Risk column)
                        tableModel.addRow(new Object[]{
                                name != null ? name : "Unnamed Project",
                                client != null ? client : "-",
                                String.format("$%.0f", budget),
                                formatDate(endDate),
                                status != null ? status : "PLANNING",
                                riskLevel,  // NEW: Risk column
                                "MANAGE",   // Action button text
                                "EDIT",     // Edit button text
                                "DELETE"    // Delete button text
                        });
                    }

                    // Update stats
                    statActiveProjects.setText(String.valueOf(activeCount));
                    statTotalBudget.setText(String.format("$%.0fK", totalBudget / 1000));
                    statusLabel.setText("Loaded " + projects.size() + " projects");

                    // Load pending tasks count
                    loadPendingTasksCount();

                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("Error loading projects");
                    loadMockData();
                }
            }
        };
        worker.execute();
    }

    /**
     * Calculates project risk level based on deadline and status.
     * This integrates the RiskAlert model concept into the UI.
     *
     * @param projectId The project ID for API lookup
     * @param endDate The project deadline
     * @param status The project status
     * @return Risk level: "LOW", "MEDIUM", or "HIGH"
     */
    private String calculateProjectRisk(String projectId, String endDate, String status) {
        try {
            // Try to get risk from ProgressReport API
            String reportResponse = apiClient.get("/reports/project/" + projectId + "/progress");
            JsonObject report = JsonParser.parseString(reportResponse).getAsJsonObject();
            if (report.has("riskLevel")) {
                return report.get("riskLevel").getAsString();
            }
        } catch (Exception e) {
            // Fallback: Calculate risk locally
        }

        // Local risk calculation fallback
        if (endDate == null || endDate.isEmpty()) {
            return "LOW";
        }

        try {
            java.time.LocalDate deadline = java.time.LocalDate.parse(endDate.substring(0, 10));
            java.time.LocalDate today = java.time.LocalDate.now();
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, deadline);

            if (daysRemaining < 0) {
                return "HIGH"; // Overdue
            } else if (daysRemaining <= 7) {
                return "HIGH"; // Due within a week
            } else if (daysRemaining <= 30) {
                return "MEDIUM"; // Due within a month
            } else {
                return "LOW";
            }
        } catch (Exception e) {
            return "LOW";
        }
    }

    private void loadPendingTasksCount() {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                String response = apiClient.get("/tasks");
                JsonArray tasks = JsonParser.parseString(response).getAsJsonArray();
                int pending = 0;
                for (JsonElement elem : tasks) {
                    JsonObject task = elem.getAsJsonObject();
                    String status = getJsonString(task, "status");
                    if ("TODO".equalsIgnoreCase(status) || "DOING".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }
                return pending;
            }

            @Override
            protected void done() {
                try {
                    statPendingTasks.setText(String.valueOf(get()));
                } catch (Exception e) {
                    statPendingTasks.setText("?");
                }
            }
        };
        worker.execute();
    }

    private void loadMockData() {
        projectIds.add("mock-1");
        projectIds.add("mock-2");
        projectIds.add("mock-3");

        tableModel.addRow(new Object[]{"E-Commerce Platform", "CapsuleCorp", "$50,000", "2024-06-30", "ACTIVE", "MANAGE", "DELETE"});
        tableModel.addRow(new Object[]{"Mobile App", "SaiyanTech", "$35,000", "2024-08-15", "PLANNING", "MANAGE", "DELETE"});
        tableModel.addRow(new Object[]{"ERP System", "FriezaInc", "$120,000", "2024-12-31", "ACTIVE", "MANAGE", "DELETE"});

        statActiveProjects.setText("2");
        statTotalBudget.setText("$205K");
        statPendingTasks.setText("5");
        statusLabel.setText("Showing mock data");
    }

    // ============================================
    // DIALOGS
    // ============================================

    private void showExportReportDialog() {
        ReportUtils.showExportDialog(this, apiClient, "Project Report");
    }

    private JButton createOutlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(0xF3, 0xF4, 0xF6) : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setColor(AppTheme.PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.fontMain(Font.BOLD, 13));
        btn.setForeground(AppTheme.PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void openProjectDialog() {
        ProjectDialog dialog = new ProjectDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                this::loadProjects
        );
        dialog.setVisible(true);
    }

    private void openProjectDetailDialog(int row) {
        if (row < 0 || row >= projectIds.size()) return;

        String projectId = projectIds.get(row);
        String projectName = (String) tableModel.getValueAt(row, 0);

        // Use the dedicated ProjectDetailDialog class
        ProjectDetailDialog dialog = new ProjectDetailDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                projectId,
                projectName,
                this::loadProjects // Refresh callback
        );
        dialog.setVisible(true);
    }

    /**
     * Delete project with confirmation dialog.
     * Cascade deletes all associated tasks before deleting the project.
     */
    private void deleteProject(int row) {
        if (row < 0 || row >= projectIds.size()) return;

        String projectId = projectIds.get(row);
        String projectName = (String) tableModel.getValueAt(row, 0);

        // Show confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deleting this project will remove ALL its tasks.\n\n" +
                "Project: " + projectName + "\n\n" +
                "Are you sure you want to continue?",
                "Confirm Delete Project",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Perform deletion in background
        statusLabel.setText("Deleting project...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.delete("/projects/" + projectId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Remove from table
                    projectIds.remove(row);
                    tableModel.removeRow(row);
                    statusLabel.setText("Project deleted successfully");

                    // Refresh stats
                    loadProjects();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            ManagerPanel.this,
                            "Error deleting project: " + e.getMessage(),
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Error deleting project");
                }
            }
        };
        worker.execute();
    }


    // ============================================
    // UTILITY METHODS
    // ============================================

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "-";
        try {
            if (dateStr.length() > 10) {
                dateStr = dateStr.substring(0, 10);
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = AppTheme.PRIMARY;
                if (getModel().isPressed()) {
                    bg = bg.darker();
                } else if (getModel().isRollover()) {
                    bg = new Color(0xFF, 0x6B, 0x2A);
                }

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.fontMain(Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }


    // ============================================
    // TABLE RENDERERS & EDITORS
    // ============================================

    /**
     * Status Badge Renderer - Colored pill for status column
     */
    private class StatusBadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel label = new JLabel();

        public StatusBadgeRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
            setOpaque(true);
            label.setFont(AppTheme.fontMain(Font.BOLD, 11));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            String status = value != null ? value.toString().toUpperCase() : "UNKNOWN";
            label.setText(status);

            Color bg, fg;
            switch (status) {
                case "ACTIVE":
                case "DONE":
                case "COMPLETED":
                    bg = new Color(0xDC, 0xFC, 0xE7);
                    fg = new Color(0x16, 0xA3, 0x4A);
                    break;
                case "DOING":
                case "IN_PROGRESS":
                    bg = new Color(0xFF, 0xED, 0xD5);
                    fg = new Color(0xEA, 0x58, 0x0C);
                    break;
                case "PLANNING":
                case "TODO":
                    bg = new Color(0xFE, 0xF9, 0xC3);
                    fg = new Color(0xA1, 0x62, 0x07);
                    break;
                default:
                    bg = new Color(0xF3, 0xF4, 0xF6);
                    fg = new Color(0x6B, 0x72, 0x80);
            }

            label.setBackground(bg);
            label.setForeground(fg);
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            return this;
        }
    }

    /**
     * Risk Badge Renderer - Colored pill for risk column.
     * Integrates RiskAlert model concept into the UI.
     */
    private class RiskBadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel label = new JLabel();

        public RiskBadgeRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
            setOpaque(true);
            label.setFont(AppTheme.fontMain(Font.BOLD, 10));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            String risk = value != null ? value.toString().toUpperCase() : "LOW";
            label.setText(risk);

            Color bg, fg;
            switch (risk) {
                case "HIGH":
                    bg = new Color(0xFE, 0xE2, 0xE2);  // Light red
                    fg = new Color(0xDC, 0x26, 0x26);  // Red
                    label.setText("⚠ HIGH");
                    break;
                case "MEDIUM":
                    bg = new Color(0xFF, 0xED, 0xD5);  // Light orange
                    fg = new Color(0xF5, 0x9E, 0x0B);  // Amber
                    label.setText("⚡ MED");
                    break;
                default: // LOW
                    bg = new Color(0xDC, 0xFC, 0xE7);  // Light green
                    fg = new Color(0x10, 0xB9, 0x81);  // Green
                    label.setText("✓ LOW");
            }

            label.setBackground(bg);
            label.setForeground(fg);
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            return this;
        }
    }

    /**
     * Manage Button Renderer - Blue outline button
     */
    private class ManageButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("MANAGE");

        public ManageButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
            setOpaque(true);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.SECONDARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.SECONDARY, 1, true));
            button.setPreferredSize(new Dimension(80, 32));
            button.setFocusPainted(false);

            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    /**
     * Manage Button Editor - Handles button clicks
     */
    private class ManageButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        private final JButton button = new JButton("MANAGE");
        private int currentRow;

        public ManageButtonEditor(JTable table) {
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.SECONDARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.SECONDARY, 1, true));
            button.setPreferredSize(new Dimension(80, 32));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
                openProjectDetailDialog(currentRow);
            });

            panel.add(button);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "MANAGE";
        }
    }

    /**
     * Edit Button Renderer - Orange outline button for edit action
     */
    private class EditButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("EDIT");

        public EditButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
            setOpaque(true);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
            button.setPreferredSize(new Dimension(55, 32));
            button.setFocusPainted(false);

            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    /**
     * Edit Button Editor - Handles edit button clicks
     */
    private class EditButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        private final JButton button = new JButton("EDIT");
        private int currentRow;

        public EditButtonEditor(JTable table) {
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
            button.setPreferredSize(new Dimension(55, 32));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
                openEditProjectDialog(currentRow);
            });

            panel.add(button);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "EDIT";
        }
    }

    /**
     * Opens the Edit Project dialog to update project details.
     */
    private void openEditProjectDialog(int row) {
        if (row < 0 || row >= projectIds.size()) return;

        String projectId = projectIds.get(row);
        String projectName = (String) tableModel.getValueAt(row, 0);
        String budget = (String) tableModel.getValueAt(row, 2);
        String endDate = (String) tableModel.getValueAt(row, 3); // Deadline column

        // Create edit dialog - increased height for new date fields
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Project", true);
        dialog.setSize(500, 580);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);
        JLabel headerIcon = new JLabel("✏");
        headerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        headerPanel.add(headerIcon);
        JLabel headerTitle = new JLabel("Edit Project");
        headerTitle.setFont(AppTheme.fontHeading(18));
        headerTitle.setForeground(AppTheme.SECONDARY);
        headerPanel.add(headerTitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Scrollable Form content
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Project Name
        formPanel.add(createFormLabel("Project Name *"));
        formPanel.add(Box.createVerticalStrut(5));
        JTextField nameField = createFormTextField(projectName);
        formPanel.add(nameField);
        formPanel.add(Box.createVerticalStrut(12));

        // Description
        formPanel.add(createFormLabel("Description"));
        formPanel.add(Box.createVerticalStrut(5));
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(descScroll);
        formPanel.add(Box.createVerticalStrut(12));

        // Budget
        formPanel.add(createFormLabel("Budget ($)"));
        formPanel.add(Box.createVerticalStrut(5));
        String budgetValue = budget.replace("$", "").replace(",", "");
        JTextField budgetField = createFormTextField(budgetValue);
        formPanel.add(budgetField);
        formPanel.add(Box.createVerticalStrut(12));

        // Date fields side by side
        JPanel datePanel = new JPanel(new GridLayout(1, 2, 15, 0));
        datePanel.setOpaque(false);
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Start Date
        JPanel startDatePanel = new JPanel();
        startDatePanel.setLayout(new BoxLayout(startDatePanel, BoxLayout.Y_AXIS));
        startDatePanel.setOpaque(false);
        startDatePanel.add(createFormLabel("Start Date (yyyy-MM-dd)"));
        startDatePanel.add(Box.createVerticalStrut(5));
        JTextField startDateField = createFormTextField("");
        startDateField.setToolTipText("Format: yyyy-MM-dd (e.g., 2025-01-15)");
        startDatePanel.add(startDateField);
        datePanel.add(startDatePanel);

        // End Date
        JPanel endDatePanel = new JPanel();
        endDatePanel.setLayout(new BoxLayout(endDatePanel, BoxLayout.Y_AXIS));
        endDatePanel.setOpaque(false);
        endDatePanel.add(createFormLabel("End Date (yyyy-MM-dd)"));
        endDatePanel.add(Box.createVerticalStrut(5));
        JTextField endDateField = createFormTextField(endDate != null && !endDate.equals("-") ? endDate : "");
        endDateField.setToolTipText("Format: yyyy-MM-dd (e.g., 2025-12-31)");
        endDatePanel.add(endDateField);
        datePanel.add(endDatePanel);

        formPanel.add(datePanel);

        // Wrap form in scroll pane
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(Color.WHITE);
        mainPanel.add(formScroll, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);

        JButton saveBtn = createPrimaryButton("Save Changes");
        saveBtn.setPreferredSize(new Dimension(130, 36));
        saveBtn.addActionListener(e -> {
            // Validate
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Project Name is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Save changes
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    JsonObject update = new JsonObject();
                    update.addProperty("name", nameField.getText().trim());
                    update.addProperty("description", descArea.getText().trim());

                    try {
                        double budgetVal = Double.parseDouble(budgetField.getText().trim());
                        update.addProperty("budget", budgetVal);
                    } catch (NumberFormatException ex) {
                        // Keep existing budget if invalid
                    }

                    // Add dates if provided
                    String startDate = startDateField.getText().trim();
                    String endDateVal = endDateField.getText().trim();
                    if (!startDate.isEmpty()) {
                        update.addProperty("startDate", startDate);
                    }
                    if (!endDateVal.isEmpty()) {
                        update.addProperty("endDate", endDateVal);
                    }

                    apiClient.put("/projects/" + projectId, update.toString());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, "Project updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadProjects(); // Refresh table
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Error updating project: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });
        buttonPanel.add(saveBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Helper: Create a styled form label
     */
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.fontMain(Font.BOLD, 12));
        label.setForeground(AppTheme.SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Helper: Create a styled form text field with proper height
     */
    private JTextField createFormTextField(String initialValue) {
        JTextField field = new JTextField(initialValue);
        field.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setPreferredSize(new Dimension(200, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    /**
     * Delete Button Renderer - Red outline button for delete action
     */
    private class DeleteButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("DELETE");

        public DeleteButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
            setOpaque(true);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.DANGER);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1, true));
            button.setPreferredSize(new Dimension(70, 32));
            button.setFocusPainted(false);

            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    /**
     * Delete Button Editor - Handles delete button clicks with confirmation
     */
    private class DeleteButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        private final JButton button = new JButton("DELETE");
        private int currentRow;

        public DeleteButtonEditor(JTable table) {
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            button.setFont(AppTheme.fontMain(Font.BOLD, 11));
            button.setForeground(AppTheme.DANGER);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1, true));
            button.setPreferredSize(new Dimension(70, 32));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
                deleteProject(currentRow);
            });

            panel.add(button);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "DELETE";
        }
    }
}

