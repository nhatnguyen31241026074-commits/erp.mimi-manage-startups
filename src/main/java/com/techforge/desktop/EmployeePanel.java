package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.*;

/**
 * Employee Panel - Modern Kanban Board with Timer Widget.
 */
public class EmployeePanel extends JPanel {

    private final ApiClient apiClient;
    private JPanel todoColumn;
    private JPanel doingColumn;
    private JPanel doneColumn;
    private JLabel statusLabel;
    private JLabel todoCountLabel;
    private JLabel doingCountLabel;
    private JLabel doneCountLabel;

    // Multi-Project Filter
    private JComboBox<String> projectFilterComboBox;
    private DefaultComboBoxModel<String> projectFilterModel;
    private java.util.Map<String, String> projectIdMap = new java.util.HashMap<>(); // projectName -> projectId

    // Timer
    private JLabel timerLabel;
    private JButton timerButton;
    private Timer timer;
    private int elapsedSeconds = 0;
    private boolean timerRunning = false;

    public EmployeePanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        initializeUI();
        loadProjects();
        loadTasks();

        // EVENT-DRIVEN REFRESH: Refresh tasks when panel becomes visible
        // This ensures tasks created in other views (Project Detail) appear immediately
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("EmployeePanel shown - refreshing tasks...");
                refreshTasks();
            }
        });
    }

    // Public method for external refresh (called after AssignTaskDialog closes)
    public void refreshTasks() {
        loadProjects();
        loadTasks();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 25));
        setOpaque(false);

        // Top section with header and filter bar
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        topSection.add(createHeader());
        topSection.add(Box.createVerticalStrut(15));
        topSection.add(createFilterBar());

        add(topSection, BorderLayout.NORTH);
        add(createKanbanBoard(), BorderLayout.CENTER);

        statusLabel = new JLabel("Loading tasks...");
        statusLabel.setFont(AppTheme.fontMain(Font.ITALIC, 12));
        statusLabel.setForeground(AppTheme.TEXT_LIGHT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createFilterBar() {
        JPanel filterBar = new JPanel(new BorderLayout(15, 0));
        filterBar.setOpaque(false);
        filterBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Left: Project filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterPanel.setOpaque(false);

        JLabel filterLabel = new JLabel("Filter by Project:");
        filterLabel.setFont(AppTheme.fontMain(Font.BOLD, 13));
        filterLabel.setForeground(AppTheme.SECONDARY);
        filterPanel.add(filterLabel);

        projectFilterModel = new DefaultComboBoxModel<>();
        projectFilterModel.addElement("All Projects");
        projectFilterComboBox = new JComboBox<>(projectFilterModel);
        projectFilterComboBox.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        projectFilterComboBox.setPreferredSize(new Dimension(250, 36));
        projectFilterComboBox.addActionListener(e -> loadTasks());
        filterPanel.add(projectFilterComboBox);

        filterBar.add(filterPanel, BorderLayout.WEST);

        // Right: Refresh button
        JButton refreshBtn = createRefreshButton();
        refreshBtn.setPreferredSize(new Dimension(120, 36));
        filterBar.add(refreshBtn, BorderLayout.EAST);

        // Load projects on init
        loadProjects();

        return filterBar;
    }

    private JButton createRefreshButton() {
        JButton btn = new JButton("🔄 Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(0xF3, 0xF4, 0xF6) : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                g2.setColor(AppTheme.PRIMARY);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 8, 8));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.fontMain(Font.BOLD, 13));
        btn.setForeground(AppTheme.PRIMARY);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            loadProjects();
            loadTasks();
        });
        return btn;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Title section
        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setOpaque(false);

        JLabel title = new JLabel("Execution Deck");
        title.setFont(AppTheme.fontHeading(28));
        title.setForeground(AppTheme.SECONDARY);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, AppTheme.PRIMARY),
                BorderFactory.createEmptyBorder(0, 15, 0, 0)
        ));
        titleSection.add(title);

        JLabel subtitle = new JLabel("Track your tasks and log your Ki energy (Time)");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_LIGHT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 19, 0, 0));
        titleSection.add(subtitle);

        header.add(titleSection, BorderLayout.WEST);

        // Timer widget - pill shape
        JPanel timerWidget = createTimerWidget();
        header.add(timerWidget, BorderLayout.EAST);

        return header;
    }

    private JPanel createTimerWidget() {
        JPanel widget = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 30, 30));
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30));
                g2.dispose();
            }
        };
        widget.setOpaque(false);
        widget.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 12));
        widget.setPreferredSize(new Dimension(320, 60));

        // Pulsing indicator
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(timerRunning ? AppTheme.SUCCESS : AppTheme.TEXT_LIGHT);
                g2.fillOval(0, 0, 12, 12);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        dot.setOpaque(false);
        widget.add(dot);

        // Timer display - digital font
        timerLabel = new JLabel("00:00:00");
        timerLabel.setFont(AppTheme.fontMono(Font.BOLD, 28));
        timerLabel.setForeground(AppTheme.SECONDARY);
        widget.add(timerLabel);

        // Start/Stop button - pill
        timerButton = new JButton("Start") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(timerRunning ? AppTheme.DANGER : AppTheme.SUCCESS);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        timerButton.setFont(AppTheme.fontMain(Font.BOLD, 12));
        timerButton.setForeground(Color.WHITE);
        timerButton.setBorderPainted(false);
        timerButton.setContentAreaFilled(false);
        timerButton.setFocusPainted(false);
        timerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        timerButton.setPreferredSize(new Dimension(70, 32));
        timerButton.addActionListener(e -> toggleTimer(dot));
        widget.add(timerButton);

        return widget;
    }

    private void toggleTimer(JPanel dot) {
        if (timerRunning) {
            stopTimer();
            timerButton.setText("Start");
        } else {
            startTimer();
            timerButton.setText("Stop");
        }
        dot.repaint();
        timerButton.repaint();
    }

    private void startTimer() {
        timerRunning = true;
        timer = new Timer(1000, e -> {
            elapsedSeconds++;
            updateTimerDisplay();
        });
        timer.start();
    }

    private void stopTimer() {
        timerRunning = false;
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateTimerDisplay() {
        int hours = elapsedSeconds / 3600;
        int minutes = (elapsedSeconds % 3600) / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private JPanel createKanbanBoard() {
        JPanel board = new JPanel(new GridLayout(1, 3, 25, 0));
        board.setOpaque(false);

        todoColumn = createKanbanColumn("To Do", AppTheme.KANBAN_TODO);
        doingColumn = createKanbanColumn("Doing", AppTheme.KANBAN_DOING);
        doneColumn = createKanbanColumn("Done", AppTheme.KANBAN_DONE);

        todoCountLabel = getCountLabel(todoColumn);
        doingCountLabel = getCountLabel(doingColumn);
        doneCountLabel = getCountLabel(doneColumn);

        board.add(wrapInScrollPane(todoColumn));
        board.add(wrapInScrollPane(doingColumn));
        board.add(wrapInScrollPane(doneColumn));

        return board;
    }

    private JLabel getCountLabel(JPanel column) {
        JPanel header = (JPanel) column.getComponent(0);
        return (JLabel) header.getComponent(1);
    }

    private JPanel createKanbanColumn(String title, Color accentColor) {
        // Rounded Panel with custom paint
        JPanel column = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw shadow first
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fill(new RoundRectangle2D.Float(3, 3, getWidth() - 3, getHeight() - 3, 15, 15));

                // Draw rounded background (#F8F9FA)
                g2.setColor(new Color(248, 249, 250));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 3, 15, 15));

                // Draw accent top border
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth() - 3, 4, 4, 4);

                g2.dispose();
            }
        };
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.fontMain(Font.BOLD, 16));
        titleLabel.setForeground(AppTheme.SECONDARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel countLabel = new JLabel("0");
        countLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        countLabel.setForeground(accentColor);
        countLabel.setOpaque(true);
        countLabel.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
        countLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        headerPanel.add(countLabel, BorderLayout.EAST);

        column.add(headerPanel);
        column.add(Box.createVerticalStrut(15));

        return column;
    }

    private JScrollPane wrapInScrollPane(JPanel column) {
        JScrollPane scrollPane = new JScrollPane(column);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.KANBAN_BG);
        return scrollPane;
    }

    private void loadTasks() {
        statusLabel.setText("Loading tasks...");
        clearColumns();

        // Get selected project filter BEFORE the worker starts
        final String selectedProject = (String) projectFilterComboBox.getSelectedItem();
        final String selectedProjectId;
        if (selectedProject != null && !selectedProject.equals("All Projects")) {
            selectedProjectId = projectIdMap.get(selectedProject);
        } else {
            selectedProjectId = null;
        }

        System.out.println("========== KANBAN LOAD TASKS START ==========");
        System.out.println("DEBUG: Selected Project: " + selectedProject);
        System.out.println("DEBUG: Selected ProjectID: " + selectedProjectId);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // CRITICAL FIX: When a specific project is selected, fetch ALL tasks for that project
                // When "All Projects" is selected, fetch tasks assigned to the current user

                if (selectedProjectId != null && !selectedProjectId.isEmpty()) {
                    // Fetch tasks for the selected project (no assignee filter)
                    String endpoint = "/tasks?projectId=" + java.net.URLEncoder.encode(selectedProjectId, "UTF-8");
                    System.out.println("DEBUG: Requesting tasks for ProjectID: " + selectedProjectId);
                    System.out.println("DEBUG: API Endpoint: " + endpoint);
                    return apiClient.get(endpoint);
                } else {
                    // Fetch tasks assigned to current user (for "All Projects")
                    JsonObject currentUser = ApiClient.getCurrentUser();
                    String userEmail = null;
                    if (currentUser != null && currentUser.has("email") && !currentUser.get("email").isJsonNull()) {
                        userEmail = currentUser.get("email").getAsString();
                    }
                    System.out.println("DEBUG: Loading tasks for user email: " + userEmail);

                    if (userEmail != null && !userEmail.isEmpty()) {
                        return apiClient.get("/tasks?assignee=" + java.net.URLEncoder.encode(userEmail, "UTF-8"));
                    }
                    return apiClient.get("/tasks");
                }
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    System.out.println("DEBUG: Raw API Response: " + (response != null ? response.substring(0, Math.min(200, response.length())) + "..." : "null"));

                    JsonArray tasks = JsonParser.parseString(response).getAsJsonArray();
                    System.out.println("DEBUG: API Response Count: " + tasks.size() + " tasks");

                    int todoCount = 0, doingCount = 0, doneCount = 0;

                    for (JsonElement elem : tasks) {
                        JsonObject task = elem.getAsJsonObject();

                        String status = getJsonString(task, "status");
                        String taskTitle = getJsonString(task, "title");
                        String priority = getJsonString(task, "priority");
                        String description = getJsonString(task, "description");
                        String id = getJsonString(task, "id");
                        String taskProjectId = getJsonString(task, "projectId");

                        // DEBUG: Print task info
                        System.out.println("DEBUG: Processing Task [ID=" + id + ", Title='" + taskTitle + "', Status='" + status + "', ProjectID=" + taskProjectId + "]");

                        // Get column index using aggressive normalization
                        int columnIndex = getColumnIndexForStatus(status);
                        System.out.println("DEBUG: -> Status '" + status + "' mapped to Column " + columnIndex + " (" + getColumnName(columnIndex) + ")");

                        // Create task card on EDT
                        final JPanel taskCard = createTaskCard(taskTitle, priority, description, id, status);
                        final int colIdx = columnIndex;

                        SwingUtilities.invokeLater(() -> {
                            switch (colIdx) {
                                case 1: // Doing
                                    doingColumn.add(taskCard);
                                    doingColumn.add(Box.createVerticalStrut(12));
                                    break;
                                case 2: // Done
                                    doneColumn.add(taskCard);
                                    doneColumn.add(Box.createVerticalStrut(12));
                                    break;
                                default: // To Do (0 or unknown)
                                    todoColumn.add(taskCard);
                                    todoColumn.add(Box.createVerticalStrut(12));
                                    break;
                            }
                        });

                        switch (columnIndex) {
                            case 1: doingCount++; break;
                            case 2: doneCount++; break;
                            default: todoCount++; break;
                        }
                    }

                    final int finalTodoCount = todoCount;
                    final int finalDoingCount = doingCount;
                    final int finalDoneCount = doneCount;
                    final int totalDisplayed = todoCount + doingCount + doneCount;

                    System.out.println("DEBUG: Total tasks displayed: " + totalDisplayed + " (TODO: " + todoCount + ", DOING: " + doingCount + ", DONE: " + doneCount + ")");
                    System.out.println("========== KANBAN LOAD TASKS END ==========");

                    SwingUtilities.invokeLater(() -> {
                        todoCountLabel.setText(String.valueOf(finalTodoCount));
                        doingCountLabel.setText(String.valueOf(finalDoingCount));
                        doneCountLabel.setText(String.valueOf(finalDoneCount));

                        if (selectedProject != null && !selectedProject.equals("All Projects")) {
                            statusLabel.setText("Loaded " + totalDisplayed + " tasks from " + selectedProject);
                        } else {
                            statusLabel.setText("Loaded " + totalDisplayed + " tasks assigned to you");
                        }

                        // CRITICAL: Force UI refresh
                        todoColumn.revalidate();
                        todoColumn.repaint();
                        doingColumn.revalidate();
                        doingColumn.repaint();
                        doneColumn.revalidate();
                        doneColumn.repaint();
                        revalidate();
                        repaint();
                    });

                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to load tasks: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error loading tasks: " + e.getMessage());
                        loadMockTasks();
                    });
                }
            }
        };
        worker.execute();
    }

    private String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "To Do";
            case 1: return "Doing";
            case 2: return "Done";
            default: return "Unknown";
        }
    }

    /**
     * Case-insensitive & loose matching for status strings.
     * Maps raw status string to column index.
     * FAIL-SAFE: Unknown statuses are forced into Column 0 (To Do), never discarded.
     * @param status The raw status string from the API (can be null)
     * @return 0 = To Do, 1 = Doing, 2 = Done
     */
    private int getColumnIndexForStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            System.out.println("[STATUS] Status is null/empty, defaulting to TODO (0)");
            return 0; // Default to To Do
        }

        // Normalize: trim, uppercase, replace spaces with underscore
        String normalized = status.trim().toUpperCase().replace(" ", "_");

        // Column 2 (Done) - Check first to avoid "TODO" matching "DONE"
        if (normalized.contains("DONE") ||
            normalized.contains("COMPLETED") ||
            normalized.contains("COMPLETE") ||
            normalized.contains("FINISH") ||
            normalized.contains("FINISHED") ||
            normalized.contains("CLOSED")) {
            return 2;
        }

        // Column 1 (Doing)
        if (normalized.contains("DOING") ||
            normalized.contains("PROGRESS") ||
            normalized.contains("IN_PROGRESS") ||
            normalized.contains("INPROGRESS") ||
            normalized.contains("ACTIVE") ||
            normalized.contains("WORKING") ||
            normalized.contains("STARTED") ||
            normalized.contains("WIP")) {
            return 1;
        }

        // Column 0 (To Do) - Catch-all for most statuses
        if (normalized.contains("TODO") ||
            normalized.contains("TO_DO") ||
            normalized.contains("PENDING") ||
            normalized.contains("BACKLOG") ||
            normalized.contains("NEW") ||
            normalized.contains("OPEN") ||
            normalized.contains("WAITING") ||
            normalized.contains("CREATED")) {
            return 0;
        }

        // FALLBACK: Unknown status - FORCE into Column 0 (To Do), never discard
        System.out.println("[STATUS] Unknown status: '" + status + "' (normalized: '" + normalized + "') - FORCING to TODO (0)");
        return 0;
    }

    private void loadProjects() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/projects");
                    JsonArray projects = JsonParser.parseString(response).getAsJsonArray();

                    SwingUtilities.invokeLater(() -> {
                        // Clear existing projects (keep "All Projects")
                        String selected = (String) projectFilterComboBox.getSelectedItem();
                        projectFilterModel.removeAllElements();
                        projectIdMap.clear();

                        // Add "All Projects" option
                        projectFilterModel.addElement("All Projects");

                        // Add projects from API
                        for (JsonElement elem : projects) {
                            JsonObject project = elem.getAsJsonObject();
                            String id = getJsonString(project, "id");
                            String name = getJsonString(project, "name");

                            if (name != null && !name.isEmpty()) {
                                projectFilterModel.addElement(name);
                                if (id != null) {
                                    projectIdMap.put(name, id);
                                }
                            }
                        }

                        // Restore selection if possible
                        if (selected != null) {
                            projectFilterComboBox.setSelectedItem(selected);
                        }
                    });

                } catch (Exception e) {
                    // Silent fail - keep "All Projects" option
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadMockTasks() {
        todoColumn.add(createTaskCard("Setup Database", "HIGH", "Configure PostgreSQL", "1", "TODO"));
        todoColumn.add(Box.createVerticalStrut(12));
        todoColumn.add(createTaskCard("Design API", "MEDIUM", "REST endpoints", "2", "TODO"));
        todoColumn.add(Box.createVerticalStrut(12));

        doingColumn.add(createTaskCard("UI Development", "HIGH", "React components", "3", "DOING"));
        doingColumn.add(Box.createVerticalStrut(12));

        doneColumn.add(createTaskCard("Requirements", "LOW", "Gather specs", "4", "DONE"));
        doneColumn.add(Box.createVerticalStrut(12));

        todoCountLabel.setText("2");
        doingCountLabel.setText("1");
        doneCountLabel.setText("1");
        statusLabel.setText("Showing mock data");
        revalidate();
        repaint();
    }

    private void clearColumns() {
        while (todoColumn.getComponentCount() > 2) todoColumn.remove(2);
        while (doingColumn.getComponentCount() > 2) doingColumn.remove(2);
        while (doneColumn.getComponentCount() > 2) doneColumn.remove(2);
    }

    private JPanel createTaskCard(String title, String priority, String description, String id, String currentStatus) {
        Color priorityColor = AppTheme.getPriorityColor(priority);

        // 3D Card with drop shadow
        JPanel card = new JPanel() {
            private boolean isHovered = false;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int shadowOffset = isHovered ? 5 : 3;
                int shadowOpacity = isHovered ? 40 : 25;

                // Draw drop shadow (offset 3px, opacity 30%)
                g2.setColor(new Color(0, 0, 0, shadowOpacity));
                g2.fill(new RoundRectangle2D.Float(shadowOffset, shadowOffset, getWidth() - shadowOffset, getHeight() - shadowOffset, 12, 12));

                // Draw white card background
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - shadowOffset, getHeight() - shadowOffset, 12, 12));

                // Draw left priority stripe (4px)
                g2.setColor(priorityColor);
                g2.fillRoundRect(0, 0, 4, getHeight() - shadowOffset, 4, 4);

                g2.dispose();
            }

            public void setHovered(boolean hovered) {
                this.isHovered = hovered;
                repaint();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        card.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 16));

        // Title
        JLabel titleLabel = new JLabel(title != null ? title : "Untitled Task");
        titleLabel.setFont(AppTheme.fontMain(Font.BOLD, 14));
        titleLabel.setForeground(AppTheme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));

        // Description
        if (description != null && !description.isEmpty()) {
            String truncated = description.length() > 45 ? description.substring(0, 42) + "..." : description;
            JLabel descLabel = new JLabel(truncated);
            descLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
            descLabel.setForeground(AppTheme.TEXT_LIGHT);
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(descLabel);
            card.add(Box.createVerticalStrut(10));
        }

        // Priority badge
        JLabel priorityLabel = new JLabel(priority != null ? priority.toUpperCase() : "NORMAL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        priorityLabel.setFont(AppTheme.fontMain(Font.BOLD, 10));
        priorityLabel.setForeground(priorityColor);
        priorityLabel.setOpaque(false);
        priorityLabel.setBackground(new Color(priorityColor.getRed(), priorityColor.getGreen(), priorityColor.getBlue(), 25));
        priorityLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        priorityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(priorityLabel);

        // Store original Y position for hover effect
        final int[] originalY = {0};
        final Point[] dragStartPoint = {null};
        final String[] draggedTaskId = {null};

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint[0] = e.getPoint();
                draggedTaskId[0] = id;
                originalY[0] = card.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedTaskId[0] == null) return;

                // Reset hover effect
                try {
                    java.lang.reflect.Method setHovered = card.getClass().getMethod("setHovered", boolean.class);
                    setHovered.invoke(card, false);
                } catch (Exception ignored) {}

                // Reset position
                if (originalY[0] > 0) {
                    card.setLocation(card.getX(), originalY[0]);
                }

                // Get mouse location on screen
                Point mouseLocationOnScreen = e.getLocationOnScreen();

                // Get bounds of each column on screen
                Rectangle todoBounds = new Rectangle(todoColumn.getLocationOnScreen(), todoColumn.getSize());
                Rectangle doingBounds = new Rectangle(doingColumn.getLocationOnScreen(), doingColumn.getSize());
                Rectangle doneBounds = new Rectangle(doneColumn.getLocationOnScreen(), doneColumn.getSize());

                // Determine which column the mouse is over
                String newStatus = null;
                if (todoBounds.contains(mouseLocationOnScreen)) {
                    newStatus = "TODO";
                } else if (doingBounds.contains(mouseLocationOnScreen)) {
                    newStatus = "DOING";
                } else if (doneBounds.contains(mouseLocationOnScreen)) {
                    newStatus = "DONE";
                }

                // Update task if status changed
                if (newStatus != null && !newStatus.equalsIgnoreCase(currentStatus)) {
                    updateTaskStatus(draggedTaskId[0], newStatus);
                }

                dragStartPoint[0] = null;
                draggedTaskId[0] = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (dragStartPoint[0] == null) {
                    showTaskActions(id, title, currentStatus);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Hover lift effect - shift up by 2px
                originalY[0] = card.getY();
                card.setLocation(card.getX(), card.getY() - 2);

                // Trigger shadow enhancement
                try {
                    java.lang.reflect.Method setHovered = card.getClass().getMethod("setHovered", boolean.class);
                    setHovered.invoke(card, true);
                } catch (Exception ignored) {
                    card.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Reset position
                if (originalY[0] > 0) {
                    card.setLocation(card.getX(), originalY[0]);
                }

                // Reset shadow
                try {
                    java.lang.reflect.Method setHovered = card.getClass().getMethod("setHovered", boolean.class);
                    setHovered.invoke(card, false);
                } catch (Exception ignored) {
                    card.repaint();
                }
            }
        });

        return card;
    }

    private void showTaskActions(String taskId, String taskTitle, String currentStatus) {
        // Fetch complete task details from API first
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/tasks/" + taskId);
                    return JsonParser.parseString(response).getAsJsonObject();
                } catch (Exception e) {
                    // If fetch fails, create a minimal object
                    JsonObject minimal = new JsonObject();
                    minimal.addProperty("id", taskId);
                    minimal.addProperty("title", taskTitle);
                    minimal.addProperty("status", currentStatus);
                    return minimal;
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject task = get();
                    showTaskDetailDialog(task);
                } catch (Exception e) {
                    // Fallback to simple dialog
                    showSimpleTaskActions(taskId, taskTitle, currentStatus);
                }
            }
        };
        worker.execute();
    }

    /**
     * Shows the enhanced Task Detail dialog with all task information.
     */
    private void showTaskDetailDialog(JsonObject task) {
        String taskId = getJsonString(task, "id");
        String title = getJsonString(task, "title");
        String description = getJsonString(task, "description");
        String status = getJsonString(task, "status");
        String priority = getJsonString(task, "priority");
        String assignedUserId = getJsonString(task, "assignedUserId");
        String startDate = getJsonString(task, "startDate");
        String dueDate = getJsonString(task, "dueDate");
        String estimatedHours = task.has("estimatedHours") && !task.get("estimatedHours").isJsonNull()
            ? String.valueOf(task.get("estimatedHours").getAsDouble()) : null;

        // Create the dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Task Details", true);
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        // Main panel with white background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Header section with title and status badge
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel(title != null ? title : "Untitled Task");
        titleLabel.setFont(AppTheme.fontHeading(20));
        titleLabel.setForeground(AppTheme.SECONDARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Status badge
        JLabel statusBadge = createStatusBadge(status);
        headerPanel.add(statusBadge, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content panel (scrollable)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Description section
        if (description != null && !description.isEmpty()) {
            contentPanel.add(createInfoSection("Description", description, false));
            contentPanel.add(Box.createVerticalStrut(15));
        }

        // Assignee section with avatar
        JPanel assigneeSection = new JPanel(new BorderLayout(10, 0));
        assigneeSection.setOpaque(false);
        assigneeSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        assigneeSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel assigneeLabel = new JLabel("Assignee");
        assigneeLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        assigneeLabel.setForeground(AppTheme.TEXT_LIGHT);

        JPanel assigneeContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        assigneeContent.setOpaque(false);

        // Create avatar placeholder
        JLabel avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.TEXT_LIGHT);
                g2.setFont(AppTheme.fontMain(Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String text = assignedUserId != null && !assignedUserId.isEmpty() ?
                    assignedUserId.substring(0, 1).toUpperCase() : "?";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        avatarLabel.setPreferredSize(new Dimension(36, 36));
        assigneeContent.add(avatarLabel);

        // Assignee name - show "Nope" if not assigned
        String assigneeName = assignedUserId != null && !assignedUserId.isEmpty() ?
            assignedUserId : "Nope";
        JLabel assigneeNameLabel = new JLabel(assigneeName);
        assigneeNameLabel.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        assigneeNameLabel.setForeground(assignedUserId != null && !assignedUserId.isEmpty() ?
            AppTheme.TEXT_MAIN : AppTheme.TEXT_LIGHT);
        assigneeContent.add(assigneeNameLabel);

        JPanel assigneeWrapper = new JPanel();
        assigneeWrapper.setLayout(new BoxLayout(assigneeWrapper, BoxLayout.Y_AXIS));
        assigneeWrapper.setOpaque(false);
        assigneeWrapper.add(assigneeLabel);
        assigneeWrapper.add(Box.createVerticalStrut(5));
        assigneeWrapper.add(assigneeContent);

        assigneeSection.add(assigneeWrapper, BorderLayout.WEST);
        contentPanel.add(assigneeSection);
        contentPanel.add(Box.createVerticalStrut(15));

        // Timeline section with dates
        JPanel timelinePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        timelinePanel.setOpaque(false);
        timelinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelinePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Start Date
        JPanel startDatePanel = createDateField("Start Date", startDate);
        timelinePanel.add(startDatePanel);

        // Due Date
        JPanel dueDatePanel = createDateField("Due Date", dueDate);
        timelinePanel.add(dueDatePanel);

        contentPanel.add(timelinePanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Priority and Estimated Hours row
        JPanel detailsRow = new JPanel(new GridLayout(1, 2, 20, 0));
        detailsRow.setOpaque(false);
        detailsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Priority
        JPanel priorityPanel = new JPanel();
        priorityPanel.setLayout(new BoxLayout(priorityPanel, BoxLayout.Y_AXIS));
        priorityPanel.setOpaque(false);

        JLabel priorityLabel = new JLabel("Priority");
        priorityLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        priorityLabel.setForeground(AppTheme.TEXT_LIGHT);
        priorityPanel.add(priorityLabel);
        priorityPanel.add(Box.createVerticalStrut(5));

        JLabel priorityValue = createPriorityBadge(priority);
        priorityPanel.add(priorityValue);
        detailsRow.add(priorityPanel);

        // Estimated Hours
        JPanel hoursPanel = new JPanel();
        hoursPanel.setLayout(new BoxLayout(hoursPanel, BoxLayout.Y_AXIS));
        hoursPanel.setOpaque(false);

        JLabel hoursLabel = new JLabel("Estimated Hours");
        hoursLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        hoursLabel.setForeground(AppTheme.TEXT_LIGHT);
        hoursPanel.add(hoursLabel);
        hoursPanel.add(Box.createVerticalStrut(5));

        JLabel hoursValue = new JLabel(estimatedHours != null ? estimatedHours + "h" : "Not set");
        hoursValue.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        hoursValue.setForeground(AppTheme.TEXT_MAIN);
        hoursPanel.add(hoursValue);
        detailsRow.add(hoursPanel);

        contentPanel.add(detailsRow);
        contentPanel.add(Box.createVerticalStrut(20));

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel at bottom - 4 buttons including Edit
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Row 1: Status change buttons
        JButton todoBtn = createActionButton("Move to To Do", AppTheme.KANBAN_TODO);
        todoBtn.addActionListener(e -> {
            updateTaskStatus(taskId, "TODO");
            dialog.dispose();
        });
        buttonPanel.add(todoBtn);

        JButton doingBtn = createActionButton("Move to Doing", AppTheme.KANBAN_DOING);
        doingBtn.addActionListener(e -> {
            updateTaskStatus(taskId, "DOING");
            dialog.dispose();
        });
        buttonPanel.add(doingBtn);

        JButton doneBtn = createActionButton("Move to Done", AppTheme.KANBAN_DONE);
        doneBtn.addActionListener(e -> {
            updateTaskStatus(taskId, "DONE");
            dialog.dispose();
        });
        buttonPanel.add(doneBtn);

        // Edit Task button - opens full task editor
        // PERMISSION: Only ADMIN/MANAGER can edit task details. EMPLOYEE cannot see this button at all.
        String currentRole = ApiClient.getCurrentUserRole();
        if (!"EMPLOYEE".equalsIgnoreCase(currentRole)) {
            JButton editBtn = createActionButton("✏ Edit Task", AppTheme.SECONDARY);
            editBtn.addActionListener(e -> {
                dialog.dispose();
                openEditTaskDialog(task);
            });
            buttonPanel.add(editBtn);
        }
        // For EMPLOYEE role: Button is completely hidden (not added to panel)

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private JPanel createInfoSection(String label, String value, boolean small) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(AppTheme.fontMain(Font.BOLD, 12));
        labelComp.setForeground(AppTheme.TEXT_LIGHT);
        panel.add(labelComp);
        panel.add(Box.createVerticalStrut(5));

        JLabel valueComp = new JLabel("<html><body style='width: 380px'>" + value + "</body></html>");
        valueComp.setFont(AppTheme.fontMain(Font.PLAIN, small ? 12 : 14));
        valueComp.setForeground(AppTheme.TEXT_MAIN);
        panel.add(valueComp);

        return panel;
    }

    private JPanel createDateField(String label, String value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(AppTheme.fontMain(Font.BOLD, 12));
        labelComp.setForeground(AppTheme.TEXT_LIGHT);
        panel.add(labelComp);
        panel.add(Box.createVerticalStrut(5));

        String displayValue = value != null && !value.isEmpty() ? formatDate(value) : "Not set";
        JLabel valueComp = new JLabel("\uD83D\uDCC5 " + displayValue); // 📅 Calendar emoji
        valueComp.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        valueComp.setForeground(AppTheme.TEXT_MAIN);
        panel.add(valueComp);

        return panel;
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Not set";
        try {
            // Try to parse ISO date and format nicely
            if (dateStr.length() >= 10) {
                return dateStr.substring(0, 10); // YYYY-MM-DD
            }
        } catch (Exception e) {
            // Return as-is if parsing fails
        }
        return dateStr;
    }

    private JLabel createStatusBadge(String status) {
        String displayStatus = status != null ? status.toUpperCase() : "UNKNOWN";
        Color bgColor;
        Color textColor;

        switch (displayStatus) {
            case "DONE":
            case "COMPLETED":
                bgColor = new Color(0xD1, 0xFA, 0xE5);
                textColor = AppTheme.SUCCESS;
                break;
            case "DOING":
            case "IN_PROGRESS":
                bgColor = new Color(0xFF, 0xED, 0xD5);
                textColor = AppTheme.PRIMARY;
                break;
            default:
                bgColor = new Color(0xE5, 0xE7, 0xEB);
                textColor = AppTheme.TEXT_LIGHT;
        }

        JLabel badge = new JLabel(displayStatus) {
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
        badge.setFont(AppTheme.fontMain(Font.BOLD, 11));
        badge.setForeground(textColor);
        badge.setBackground(bgColor);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

        return badge;
    }

    private JLabel createPriorityBadge(String priority) {
        Color priorityColor = AppTheme.getPriorityColor(priority);
        String displayPriority = priority != null ? priority.toUpperCase() : "NORMAL";

        JLabel badge = new JLabel(displayPriority) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(priorityColor.getRed(), priorityColor.getGreen(), priorityColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(AppTheme.fontMain(Font.BOLD, 11));
        badge.setForeground(priorityColor);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        return badge;
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isPressed() ? color.darker() :
                           getModel().isRollover() ? color.brighter() : color;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        button.setFont(AppTheme.fontMain(Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(120, 36));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Fallback simple dialog if task fetch fails.
     */
    private void showSimpleTaskActions(String taskId, String taskTitle, String currentStatus) {
        String[] options = {"Move to To Do", "Move to Doing", "Move to Done", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Task: " + taskTitle + "\n\nChoose action:",
                "Task Actions",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[3]
        );

        String newStatus = null;
        switch (choice) {
            case 0: newStatus = "TODO"; break;
            case 1: newStatus = "DOING"; break;
            case 2: newStatus = "DONE"; break;
            default: return;
        }

        updateTaskStatus(taskId, newStatus);
    }

    /**
     * Opens an Edit Task dialog allowing updates to Title, Description, Dates, Priority, and Assignee.
     */
    private void openEditTaskDialog(JsonObject task) {
        String taskId = getJsonString(task, "id");
        String projectId = getJsonString(task, "projectId");

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Task", true);
        dialog.setSize(500, 600);  // Fixed size: 500x600 as requested
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);  // Allow resizing

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

        // Form content
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Title field
        JTextField titleField = createEditField("Title *", getJsonString(task, "title"), formPanel);

        // Description field
        JTextArea descArea = new JTextArea(getJsonString(task, "description"), 3, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        addFieldLabel("Description", formPanel);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(descScroll);
        formPanel.add(Box.createVerticalStrut(12));

        // Status dropdown - FIXED HEIGHT TO 40px
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"TODO", "DOING", "DONE"});
        String currentStatus = getJsonString(task, "status");
        if (currentStatus != null && !currentStatus.isEmpty()) {
            statusCombo.setSelectedItem(currentStatus.toUpperCase());
        }
        statusCombo.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        addFieldLabel("Status", formPanel);
        statusCombo.setMinimumSize(new Dimension(200, 40));
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        statusCombo.setPreferredSize(new Dimension(250, 40));
        statusCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(statusCombo);
        formPanel.add(Box.createVerticalStrut(12));

        // Priority dropdown - FIXED HEIGHT TO 40px
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
        String currentPriority = getJsonString(task, "priority");
        if (currentPriority != null && !currentPriority.isEmpty()) {
            priorityCombo.setSelectedItem(currentPriority.toUpperCase());
        }
        priorityCombo.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        addFieldLabel("Priority", formPanel);
        priorityCombo.setMinimumSize(new Dimension(200, 40));
        priorityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        priorityCombo.setPreferredSize(new Dimension(250, 40));
        priorityCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(priorityCombo);
        formPanel.add(Box.createVerticalStrut(12));

        // Estimated Hours
        JTextField hoursField = createEditField("Estimated Hours",
            task.has("estimatedHours") && !task.get("estimatedHours").isJsonNull()
                ? String.valueOf(task.get("estimatedHours").getAsDouble()) : "", formPanel);

        // Start Date
        JTextField startDateField = createEditField("Start Date (yyyy-MM-dd)", getJsonString(task, "startDate"), formPanel);

        // Due Date
        JTextField dueDateField = createEditField("Due Date (yyyy-MM-dd)", getJsonString(task, "dueDate"), formPanel);

        // Assignee dropdown - FIXED HEIGHT TO 40px
        JComboBox<String> assigneeCombo = new JComboBox<>();
        assigneeCombo.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        assigneeCombo.addItem("-- Select Assignee --");
        String currentAssigneeId = getJsonString(task, "assignedUserId");

        // Load employees for dropdown
        loadEmployeesIntoComboBox(assigneeCombo, currentAssigneeId);

        addFieldLabel("Assignee", formPanel);
        assigneeCombo.setMinimumSize(new Dimension(200, 40));
        assigneeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        assigneeCombo.setPreferredSize(new Dimension(250, 40));
        assigneeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(assigneeCombo);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        mainPanel.add(formScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);

        JButton saveBtn = createActionButton("Save Changes", AppTheme.PRIMARY);
        saveBtn.addActionListener(e -> {
            // Validate
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Save changes
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    JsonObject update = new JsonObject();
                    update.addProperty("title", titleField.getText().trim());
                    update.addProperty("description", descArea.getText().trim());
                    update.addProperty("status", (String) statusCombo.getSelectedItem());
                    update.addProperty("priority", (String) priorityCombo.getSelectedItem());

                    // Estimated hours
                    try {
                        String hoursText = hoursField.getText().trim();
                        if (!hoursText.isEmpty()) {
                            update.addProperty("estimatedHours", Double.parseDouble(hoursText));
                        }
                    } catch (NumberFormatException ignored) {}

                    // Dates
                    String startDate = startDateField.getText().trim();
                    if (!startDate.isEmpty()) update.addProperty("startDate", startDate);

                    String dueDate = dueDateField.getText().trim();
                    if (!dueDate.isEmpty()) update.addProperty("dueDate", dueDate);

                    // Assignee
                    String selectedAssignee = (String) assigneeCombo.getSelectedItem();
                    if (selectedAssignee != null && !selectedAssignee.startsWith("--")) {
                        // Extract email from "Name (email)" format
                        int emailStart = selectedAssignee.lastIndexOf("(");
                        int emailEnd = selectedAssignee.lastIndexOf(")");
                        if (emailStart > 0 && emailEnd > emailStart) {
                            String email = selectedAssignee.substring(emailStart + 1, emailEnd);
                            update.addProperty("assigneeEmail", email);
                        }
                    }

                    // Keep projectId
                    if (projectId != null && !projectId.isEmpty()) {
                        update.addProperty("projectId", projectId);
                    }

                    apiClient.put("/tasks/" + taskId, update.toString());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadTasks(); // Refresh Kanban
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Error updating task: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private JTextField createEditField(String labelText, String value, JPanel parent) {
        addFieldLabel(labelText, parent);
        JTextField field = new JTextField(value);
        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)  // Increased padding
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));  // Increased height
        field.setPreferredSize(new Dimension(200, 42));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(field);
        parent.add(Box.createVerticalStrut(12));
        return field;
    }

    private void addFieldLabel(String text, JPanel parent) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.fontMain(Font.BOLD, 12));
        label.setForeground(AppTheme.SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(6));  // Slightly more space
    }

    private void loadEmployeesIntoComboBox(JComboBox<String> combo, String currentAssigneeId) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/users");
                    JsonArray users = JsonParser.parseString(response).getAsJsonArray();

                    SwingUtilities.invokeLater(() -> {
                        for (JsonElement elem : users) {
                            JsonObject user = elem.getAsJsonObject();
                            String role = getJsonString(user, "role");

                            if ("EMPLOYEE".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
                                String id = getJsonString(user, "id");
                                String email = getJsonString(user, "email");
                                String name = getJsonString(user, "fullName");
                                if (name == null || name.isEmpty()) name = email != null ? email : "Unknown";

                                String displayName = name + " (" + (email != null ? email : "") + ")";
                                combo.addItem(displayName);

                                // Select current assignee
                                if (id != null && id.equals(currentAssigneeId)) {
                                    combo.setSelectedItem(displayName);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error loading employees: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void updateTaskStatus(String taskId, String newStatus) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                JsonObject update = new JsonObject();
                update.addProperty("status", newStatus);
                apiClient.post("/tasks/" + taskId, update.toString());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadTasks();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EmployeePanel.this,
                            "Error updating task: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}

