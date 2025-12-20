package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.*;

/**
 * ProjectDetailDialog - Task List for a specific project (like Jira Backlog).
 * Shows all tasks belonging to the selected project.
 */
public class ProjectDetailDialog extends JDialog {

    private final ApiClient apiClient;
    private final String projectId;
    private final String projectName;
    private final Runnable onTaskUpdated;

    private DefaultTableModel taskTableModel;
    private JLabel statusLabel;
    private JTable taskTable;
    private java.util.List<String> taskIds = new java.util.ArrayList<>();

    public ProjectDetailDialog(Frame parent, ApiClient apiClient, String projectId, String projectName, Runnable onTaskUpdated) {
        super(parent, "Project Tasks: " + projectName, true);
        this.apiClient = apiClient;
        this.projectId = projectId;
        this.projectName = projectName;
        this.onTaskUpdated = onTaskUpdated;

        initializeUI();
        loadTasks();
    }

    private void initializeUI() {
        setSize(800, 600);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header
        add(createHeader(), BorderLayout.NORTH);

        // Center: Task Table
        add(createTaskTablePanel(), BorderLayout.CENTER);

        // Footer
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Left: Title with project icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UIUtils.getFolderIcon(24));
        titlePanel.add(iconLabel);

        JLabel titleLabel = new JLabel(projectName);
        titleLabel.setFont(AppTheme.fontHeading(18));
        titleLabel.setForeground(AppTheme.HEADER_BLUE);
        titlePanel.add(titleLabel);

        header.add(titlePanel, BorderLayout.WEST);

        // Right: New Task Button
        JButton newTaskBtn = createPrimaryButton("+ New Task");
        newTaskBtn.setPreferredSize(new Dimension(120, 36));
        newTaskBtn.addActionListener(e -> openNewTaskDialog());
        header.add(newTaskBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel createTaskTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Table header bar
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("📋 Task Backlog");
        tableTitle.setFont(AppTheme.fontMain(Font.BOLD, 14));
        tableTitle.setForeground(AppTheme.SECONDARY);
        tableHeader.add(tableTitle, BorderLayout.WEST);

        // Status label
        statusLabel = new JLabel("Loading tasks...");
        statusLabel.setFont(AppTheme.fontMain(Font.ITALIC, 12));
        statusLabel.setForeground(AppTheme.TEXT_LIGHT);
        tableHeader.add(statusLabel, BorderLayout.EAST);

        panel.add(tableHeader, BorderLayout.NORTH);

        // Create Table with Edit and Delete columns
        String[] columns = {"Task Name", "Assignee", "Priority", "Status", "Est. Hours", "Edit", "Delete"};
        taskTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 6; // Edit and Delete columns are editable (for button clicks)
            }
        };

        taskTable = new JTable(taskTableModel);
        styleTable(taskTable);

        // Column widths
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Task Name
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Assignee
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(70); // Priority
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(70); // Status
        taskTable.getColumnModel().getColumn(4).setPreferredWidth(70); // Est. Hours
        taskTable.getColumnModel().getColumn(5).setPreferredWidth(55); // Edit
        taskTable.getColumnModel().getColumn(6).setPreferredWidth(65); // Delete

        // Custom renderers
        taskTable.getColumnModel().getColumn(2).setCellRenderer(new PriorityBadgeRenderer());
        taskTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        taskTable.getColumnModel().getColumn(5).setCellRenderer(new EditTaskButtonRenderer());
        taskTable.getColumnModel().getColumn(5).setCellEditor(new EditTaskButtonEditor());
        taskTable.getColumnModel().getColumn(6).setCellRenderer(new DeleteTaskButtonRenderer());
        taskTable.getColumnModel().getColumn(6).setCellEditor(new DeleteTaskButtonEditor());

        // Right-click context menu for delete
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("Delete Task");
        deleteMenuItem.setForeground(AppTheme.DANGER);
        deleteMenuItem.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow >= 0) {
                deleteTask(selectedRow);
            }
        });
        contextMenu.add(deleteMenuItem);

        taskTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = taskTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < taskTable.getRowCount()) {
                        taskTable.setRowSelectionInterval(row, row);
                        contextMenu.show(taskTable, e.getX(), e.getY());
                    }
                }
            }
        });

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45);
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
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER));

        table.setFocusable(false);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JButton closeBtn = createOutlineButton("Close");
        closeBtn.setPreferredSize(new Dimension(100, 36));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        return footer;
    }

    // ============================================
    // DATA LOADING
    // ============================================

    private void loadTasks() {
        statusLabel.setText("Loading tasks...");
        taskTableModel.setRowCount(0);
        taskIds.clear(); // Clear task IDs

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            private int taskCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Fetch all tasks and filter by projectId
                    String response = apiClient.get("/tasks");
                    JsonArray tasks = JsonParser.parseString(response).getAsJsonArray();

                    for (JsonElement elem : tasks) {
                        JsonObject task = elem.getAsJsonObject();

                        // Filter: Only show tasks for this project
                        String taskProjectId = task.has("projectId") && !task.get("projectId").isJsonNull()
                                ? task.get("projectId").getAsString() : null;

                        if (projectId != null && projectId.equals(taskProjectId)) {
                            taskCount++;

                            String id = task.has("id") && !task.get("id").isJsonNull()
                                    ? task.get("id").getAsString() : null;

                            String title = task.has("title") && !task.get("title").isJsonNull()
                                    ? task.get("title").getAsString() : "Untitled Task";

                            String assignee = task.has("assigneeEmail") && !task.get("assigneeEmail").isJsonNull()
                                    ? task.get("assigneeEmail").getAsString() : "Unassigned";

                            String priority = task.has("priority") && !task.get("priority").isJsonNull()
                                    ? task.get("priority").getAsString() : "MEDIUM";

                            String status = task.has("status") && !task.get("status").isJsonNull()
                                    ? task.get("status").getAsString() : "TODO";

                            String estimatedHours = task.has("estimatedHours") && !task.get("estimatedHours").isJsonNull()
                                    ? String.format("%.1fh", task.get("estimatedHours").getAsDouble()) : "-";

                            // Publish row to be added on EDT (include task ID for deletion)
                            publish(new Object[]{id, title, assignee, priority, status, estimatedHours, "EDIT", "DELETE"});
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    String taskId = (String) row[0];
                    taskIds.add(taskId); // Store task ID
                    // Add row without the ID (columns: title, assignee, priority, status, estHours, edit, delete)
                    taskTableModel.addRow(new Object[]{row[1], row[2], row[3], row[4], row[5], row[6], row[7]});
                }
            }

            @Override
            protected void done() {
                if (taskCount == 0) {
                    statusLabel.setText("No tasks found. Click '+ New Task' to create one.");
                    // Add empty state message to table
                    taskTableModel.addRow(new Object[]{"No tasks yet", "-", "-", "-", "-", "-", "-"});
                } else {
                    statusLabel.setText("Loaded " + taskCount + " tasks");
                }
            }
        };
        worker.execute();
    }

    // ============================================
    // DIALOG ACTIONS
    // ============================================

    private void openNewTaskDialog() {
        // Close this dialog
        dispose();

        // Open AssignTaskDialog with projectId
        AssignTaskDialog taskDialog = new AssignTaskDialog(
                (Frame) getParent(),
                apiClient,
                projectId, // IMPORTANT: Pass projectId so task is linked to this project
                () -> {
                    // Callback: Refresh parent panel
                    if (onTaskUpdated != null) {
                        onTaskUpdated.run();
                    }
                }
        );
        taskDialog.setVisible(true);
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

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

    private JButton createOutlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(0xF9, 0xFA, 0xFB) : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));

                g2.setColor(AppTheme.TEXT_LIGHT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ============================================
    // TABLE RENDERERS
    // ============================================

    /**
     * Priority Badge Renderer - Colored labels for priority levels
     */
    private class PriorityBadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel label = new JLabel();

        public PriorityBadgeRenderer() {
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

            String priority = value != null ? value.toString().toUpperCase() : "MEDIUM";
            label.setText(priority);

            Color bg, fg;
            switch (priority) {
                case "HIGH":
                case "URGENT":
                    bg = new Color(0xFE, 0xE2, 0xE2);
                    fg = new Color(0xDC, 0x26, 0x26);
                    break;
                case "MEDIUM":
                    bg = new Color(0xFF, 0xED, 0xD5);
                    fg = new Color(0xEA, 0x58, 0x0C);
                    break;
                case "LOW":
                    bg = new Color(0xDC, 0xFC, 0xE7);
                    fg = new Color(0x16, 0xA3, 0x4A);
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
     * Status Badge Renderer - Colored labels for task status
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

            String status = value != null ? value.toString().toUpperCase() : "TODO";
            label.setText(status);

            Color bg, fg;
            switch (status) {
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
                case "TODO":
                case "PENDING":
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
     * Delete Task with confirmation dialog
     */
    private void deleteTask(int row) {
        if (row < 0 || row >= taskIds.size()) return;

        String taskId = taskIds.get(row);
        String taskName = (String) taskTableModel.getValueAt(row, 0);

        // Show confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this task?\n\n" +
                "Task: " + taskName,
                "Confirm Delete Task",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Perform deletion in background
        statusLabel.setText("Deleting task...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.delete("/tasks/" + taskId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Remove from table
                    taskIds.remove(row);
                    taskTableModel.removeRow(row);
                    statusLabel.setText("Task deleted successfully");

                    // Notify parent to refresh
                    if (onTaskUpdated != null) {
                        onTaskUpdated.run();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            ProjectDetailDialog.this,
                            "Error deleting task: " + e.getMessage(),
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Error deleting task");
                }
            }
        };
        worker.execute();
    }

    /**
     * Edit Task Button Renderer - Orange outline button
     */
    private class EditTaskButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("EDIT");

        public EditTaskButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 6));
            setOpaque(true);

            button.setFont(AppTheme.fontMain(Font.BOLD, 10));
            button.setForeground(AppTheme.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
            button.setPreferredSize(new Dimension(50, 28));
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
     * Edit Task Button Editor - Handles edit button clicks
     */
    private class EditTaskButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        private final JButton button = new JButton("EDIT");
        private int currentRow;

        public EditTaskButtonEditor() {
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            button.setFont(AppTheme.fontMain(Font.BOLD, 10));
            button.setForeground(AppTheme.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
            button.setPreferredSize(new Dimension(50, 28));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
                openEditTaskDialog(currentRow);
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
     * Opens a dialog to edit task details.
     * COMPLETELY OVERHAULED: Uses GridBagLayout with 40px height fields.
     */
    private void openEditTaskDialog(int row) {
        if (row < 0 || row >= taskIds.size()) return;

        String taskId = taskIds.get(row);
        String taskName = (String) taskTableModel.getValueAt(row, 0);
        String assignee = (String) taskTableModel.getValueAt(row, 1);
        String priority = (String) taskTableModel.getValueAt(row, 2);
        String status = (String) taskTableModel.getValueAt(row, 3);
        String estHours = (String) taskTableModel.getValueAt(row, 4);

        // Fetch full task details from API for dates
        SwingWorker<JsonObject, Void> fetchWorker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/tasks/" + taskId);
                    return JsonParser.parseString(response).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject taskData = get();
                    showEditTaskDialogUI(taskId, taskName, assignee, priority, status, estHours, taskData);
                } catch (Exception e) {
                    showEditTaskDialogUI(taskId, taskName, assignee, priority, status, estHours, null);
                }
            }
        };
        fetchWorker.execute();
    }

    /**
     * Shows the Edit Task dialog UI with all fields properly sized.
     */
    private void showEditTaskDialogUI(String taskId, String taskName, String assignee,
            String priority, String status, String estHours, JsonObject taskData) {

        // Extract dates from task data if available
        String startDate = "";
        String dueDate = "";
        String estimatedHoursValue = "";

        if (taskData != null) {
            startDate = taskData.has("startDate") && !taskData.get("startDate").isJsonNull()
                ? taskData.get("startDate").getAsString() : "";
            dueDate = taskData.has("dueDate") && !taskData.get("dueDate").isJsonNull()
                ? taskData.get("dueDate").getAsString() : "";
            if (taskData.has("estimatedHours") && !taskData.get("estimatedHours").isJsonNull()) {
                estimatedHoursValue = String.valueOf(taskData.get("estimatedHours").getAsDouble());
            }
        }

        // Parse estHours if not from API
        if (estimatedHoursValue.isEmpty() && estHours != null && !estHours.equals("-")) {
            estimatedHoursValue = estHours.replace("h", "").trim();
        }

        // Create edit dialog with increased size
        JDialog dialog = new JDialog(this, "Edit Task", true);
        dialog.setSize(500, 650);  // Increased size for all fields
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);
        JLabel headerIcon = new JLabel("✏");
        headerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        headerPanel.add(headerIcon);
        JLabel headerTitle = new JLabel("Edit Task");
        headerTitle.setFont(AppTheme.fontHeading(18));
        headerTitle.setForeground(AppTheme.SECONDARY);
        headerPanel.add(headerTitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form content using GridBagLayout for proper spacing
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);  // Proper spacing (breathing room)
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        // 1. Task Title (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Task Title *"), gbc);

        JTextField nameField = createEditTextField(taskName);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(nameField, gbc);
        row++;

        // 2. Assignee Dropdown (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Assignee"), gbc);

        JComboBox<String> assigneeCombo = createEditComboBox();
        assigneeCombo.addItem("-- Select Assignee --");
        String currentAssignee = (assignee != null && !assignee.equals("Unassigned")) ? assignee : null;
        loadAssigneesIntoComboBox(assigneeCombo, currentAssignee);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(assigneeCombo, gbc);
        row++;

        // 3. Start Date (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Start Date (yyyy-MM-dd)"), gbc);

        JTextField startDateField = createEditTextField(startDate);
        startDateField.setToolTipText("Format: yyyy-MM-dd (e.g., 2025-01-15)");
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(startDateField, gbc);
        row++;

        // 4. Due Date (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Due Date (yyyy-MM-dd)"), gbc);

        JTextField dueDateField = createEditTextField(dueDate);
        dueDateField.setToolTipText("Format: yyyy-MM-dd (e.g., 2025-12-31)");
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(dueDateField, gbc);
        row++;

        // 5. Estimated Hours (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Estimated Hours"), gbc);

        JTextField hoursField = createEditTextField(estimatedHoursValue);
        hoursField.setToolTipText("Enter hours (e.g., 8 or 4.5)");
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(hoursField, gbc);
        row++;

        // 6. Priority Dropdown (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Priority"), gbc);

        JComboBox<String> priorityCombo = createEditComboBox();
        priorityCombo.addItem("LOW");
        priorityCombo.addItem("MEDIUM");
        priorityCombo.addItem("HIGH");
        priorityCombo.addItem("CRITICAL");
        priorityCombo.setSelectedItem(priority != null ? priority.toUpperCase() : "MEDIUM");
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(priorityCombo, gbc);
        row++;

        // 7. Status Dropdown (Height 40px)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Status"), gbc);

        JComboBox<String> statusCombo = createEditComboBox();
        statusCombo.addItem("TODO");
        statusCombo.addItem("DOING");
        statusCombo.addItem("DONE");
        statusCombo.setSelectedItem(status != null ? status.toUpperCase() : "TODO");
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(statusCombo, gbc);
        row++;

        // Add vertical filler
        gbc.gridx = 0; gbc.gridy = row; gbc.weighty = 1;
        gbc.gridwidth = 2;
        formPanel.add(Box.createVerticalGlue(), gbc);

        // Wrap form in scroll pane
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);

        JButton saveBtn = createPrimaryButton("Save Changes");
        saveBtn.setPreferredSize(new Dimension(130, 38));
        saveBtn.addActionListener(e -> {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Task Title is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    JsonObject update = new JsonObject();
                    update.addProperty("title", nameField.getText().trim());

                    // Assignee
                    String selectedAssignee = (String) assigneeCombo.getSelectedItem();
                    if (selectedAssignee != null && !selectedAssignee.startsWith("--") && !selectedAssignee.equals("No Employees")) {
                        // Extract email from "Name (email)" format
                        int emailStart = selectedAssignee.lastIndexOf("(");
                        int emailEnd = selectedAssignee.lastIndexOf(")");
                        if (emailStart > 0 && emailEnd > emailStart) {
                            String email = selectedAssignee.substring(emailStart + 1, emailEnd);
                            update.addProperty("assigneeEmail", email);
                        } else {
                            update.addProperty("assigneeEmail", selectedAssignee);
                        }
                    }

                    // Dates
                    String startDateVal = startDateField.getText().trim();
                    if (!startDateVal.isEmpty()) {
                        update.addProperty("startDate", startDateVal);
                    }

                    String dueDateVal = dueDateField.getText().trim();
                    if (!dueDateVal.isEmpty()) {
                        update.addProperty("dueDate", dueDateVal);
                    }

                    // Estimated Hours
                    String hoursVal = hoursField.getText().trim();
                    if (!hoursVal.isEmpty()) {
                        try {
                            update.addProperty("estimatedHours", Double.parseDouble(hoursVal));
                        } catch (NumberFormatException ignored) {}
                    }

                    update.addProperty("priority", (String) priorityCombo.getSelectedItem());
                    update.addProperty("status", (String) statusCombo.getSelectedItem());
                    update.addProperty("projectId", projectId);

                    apiClient.put("/tasks/" + taskId, update.toString());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadTasks(); // Refresh table
                        if (onTaskUpdated != null) onTaskUpdated.run();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
     * Creates a styled text field with 40px height (prevents text cut-off).
     */
    private JTextField createEditTextField(String initialValue) {
        JTextField field = new JTextField(initialValue != null ? initialValue : "");
        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        // CRITICAL: Height 40px is MANDATORY to prevent text cut-off
        field.setPreferredSize(new Dimension(300, 40));
        field.setMinimumSize(new Dimension(300, 40));
        return field;
    }

    /**
     * Creates a styled combo box with 40px height (prevents text cut-off).
     */
    private JComboBox<String> createEditComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        // CRITICAL: Height 40px is MANDATORY to prevent text cut-off
        combo.setPreferredSize(new Dimension(300, 40));
        combo.setMinimumSize(new Dimension(300, 40));
        return combo;
    }

    /**
     * Loads employees into the assignee dropdown.
     * If list is empty, shows "No Employees".
     */
    private void loadAssigneesIntoComboBox(JComboBox<String> combo, String currentAssignee) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean hasEmployees = false;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/users");
                    JsonArray users = JsonParser.parseString(response).getAsJsonArray();

                    SwingUtilities.invokeLater(() -> {
                        for (JsonElement elem : users) {
                            JsonObject user = elem.getAsJsonObject();
                            String role = user.has("role") && !user.get("role").isJsonNull()
                                ? user.get("role").getAsString() : "";

                            if ("EMPLOYEE".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
                                hasEmployees = true;
                                String email = user.has("email") && !user.get("email").isJsonNull()
                                    ? user.get("email").getAsString() : "";
                                String name = user.has("fullName") && !user.get("fullName").isJsonNull()
                                    ? user.get("fullName").getAsString() : email;

                                if (name.isEmpty()) name = email;
                                String displayName = name + " (" + email + ")";
                                combo.addItem(displayName);

                                // Select current assignee
                                if (currentAssignee != null &&
                                    (displayName.contains(currentAssignee) || email.equals(currentAssignee))) {
                                    combo.setSelectedItem(displayName);
                                }
                            }
                        }

                        // If no employees found, show placeholder
                        if (!hasEmployees) {
                            combo.removeAllItems();
                            combo.addItem("No Employees");
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        combo.removeAllItems();
                        combo.addItem("Failed to load");
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.fontMain(Font.BOLD, 11));
        label.setForeground(AppTheme.SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Delete Task Button Renderer - Red outline button
     */
    private class DeleteTaskButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("DELETE");

        public DeleteTaskButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 6));
            setOpaque(true);

            button.setFont(AppTheme.fontMain(Font.BOLD, 10));
            button.setForeground(AppTheme.DANGER);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1, true));
            button.setPreferredSize(new Dimension(60, 28));
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
     * Delete Task Button Editor - Handles delete button clicks with confirmation
     */
    private class DeleteTaskButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        private final JButton button = new JButton("DELETE");
        private int currentRow;

        public DeleteTaskButtonEditor() {
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            button.setFont(AppTheme.fontMain(Font.BOLD, 10));
            button.setForeground(AppTheme.DANGER);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1, true));
            button.setPreferredSize(new Dimension(60, 28));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
                deleteTask(currentRow);
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

