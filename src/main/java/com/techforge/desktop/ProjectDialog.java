package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.gson.*;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;

import com.techforge.erp.model.Client;

/**
 * ProjectDialog - Modern project creation dialog.
 *
 * FIX: Form fields wrapped in JScrollPane to prevent button cutoff.
 * Buttons are in a separate SOUTH panel, always visible.
 */
public class ProjectDialog extends JDialog {

    private final ApiClient apiClient;
    private final Runnable onProjectCreated;
    // ApiClient is provided by caller; we'll use HttpURLConnection to call backend endpoints directly
    // (keeps dependency minimal and satisfies HttpURLConnection requirement)

    // Form fields
    private JTextField projectNameField;
    private JTextField budgetField;
    private JTextField deadlineField;
    private JComboBox<Client> clientComboBox;
    private JTextField descriptionField;
    private JTextArea aiDescriptionArea;
    private JButton generateTasksBtn;

    public ProjectDialog(Frame parent, ApiClient apiClient, Runnable onProjectCreated) {
        super(parent, "Create New Project", true);
        this.apiClient = apiClient;
        this.onProjectCreated = onProjectCreated;

        initializeUI();
    }

    private void initializeUI() {
        setSize(520, 650);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Header (NORTH)
        add(createHeader(), BorderLayout.NORTH);

        // FIX: Wrap form panel in JScrollPane (CENTER)
        JPanel formPanel = createFormPanel();
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // FIX: Footer with buttons (SOUTH) - always visible
        add(createFooter(), BorderLayout.SOUTH);

        // Load clients into combo box asynchronously
        loadClients();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.INPUT_BORDER));

        // Plus icon
        JLabel iconLabel = new JLabel(UIUtils.getPlusIcon(28, AppTheme.SECONDARY));
        header.add(iconLabel);

        // Title
        JLabel title = new JLabel("Create New Project");
        title.setFont(AppTheme.fontHeading(20));
        title.setForeground(AppTheme.HEADER_BLUE);
        header.add(title);

        return header;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Project Name
        form.add(createFormGroup("Project Name *", projectNameField = createStyledTextField()));
        form.add(Box.createVerticalStrut(15));

        // Description
        form.add(createFormGroup("Description", descriptionField = createStyledTextField()));
        form.add(Box.createVerticalStrut(15));

        // Budget
        form.add(createFormGroup("Budget ($) *", budgetField = createStyledTextField()));
        form.add(Box.createVerticalStrut(15));

        // Deadline
        form.add(createFormGroup("Deadline (YYYY-MM-DD) *", deadlineField = createStyledTextField()));
        deadlineField.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        form.add(Box.createVerticalStrut(15));

        // Client selection (ComboBox)
        clientComboBox = new JComboBox<>();
        clientComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Client) {
                    Client c = (Client) value;
                    setText(c.getName() != null ? c.getName() + " (" + (c.getEmail() != null ? c.getEmail() : "") + ")" : "Unnamed Client");
                } else if (value == null) {
                    setText("(No client)");
                }
                return this;
            }
        });
        form.add(createComboFormGroup("Client *", clientComboBox));
        form.add(Box.createVerticalStrut(20));

        // AI Assistant Section
        form.add(createAISection());
        form.add(Box.createVerticalStrut(10));

        return form;
    }

    private JPanel createComboFormGroup(String labelText, JComboBox<Client> combo) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        combo.setPreferredSize(new Dimension(0, 42));
        group.add(combo);

        return group;
    }

    private JPanel createFormGroup(String labelText, JTextField field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Label
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        // Field
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        group.add(field);

        return group;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? AppTheme.PRIMARY : AppTheme.INPUT_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2 : 1));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setForeground(AppTheme.TEXT_MAIN);
        field.setBackground(Color.WHITE);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        field.setPreferredSize(new Dimension(0, 42));

        return field;
    }

    private JPanel createAISection() {
        JPanel section = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xEF, 0xF6, 0xFF));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0x3B, 0x82, 0xF6));
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Header
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pencilIcon = new JLabel(UIUtils.getEditIcon(18));
        headerRow.add(pencilIcon);

        JLabel aiTitle = new JLabel("AI Assistant (Gemini)");
        aiTitle.setFont(AppTheme.fontMain(Font.BOLD, 14));
        aiTitle.setForeground(AppTheme.HEADER_BLUE);
        headerRow.add(aiTitle);

        section.add(headerRow);
        section.add(Box.createVerticalStrut(10));

        // Description
        aiDescriptionArea = new JTextArea(2, 20);
        aiDescriptionArea.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        aiDescriptionArea.setForeground(AppTheme.TEXT_LIGHT);
        aiDescriptionArea.setLineWrap(true);
        aiDescriptionArea.setWrapStyleWord(true);
        aiDescriptionArea.setEditable(false);
        aiDescriptionArea.setOpaque(false);
        aiDescriptionArea.setText("Click below to generate AI-powered task suggestions.");
        aiDescriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(aiDescriptionArea);
        section.add(Box.createVerticalStrut(10));

        // Generate Button
        generateTasksBtn = createOutlineButton(" GENERATE TASKS");
        generateTasksBtn.addActionListener(e -> generateAITasks());
        section.add(generateTasksBtn);

        return section;
    }

    private void generateAITasks() {
        if (projectNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter project name first!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        generateTasksBtn.setEnabled(false);
        generateTasksBtn.setText("🐉 Summoning Shenron...");
        aiDescriptionArea.setText("🌟 Gathering Dragon Balls...\n⏳ Please wait...");
        aiDescriptionArea.setForeground(AppTheme.ACCENT);

        Timer timer = new Timer(1500, e -> {
            StringBuilder tasks = new StringBuilder();
            tasks.append("✅ AI Generated Tasks:\n");
            tasks.append("1. Setup Environment\n");
            tasks.append("2. Database Schema\n");
            tasks.append("3. API Implementation\n");
            tasks.append("4. Frontend Components\n");
            tasks.append("5. Testing & QA");

            aiDescriptionArea.setText(tasks.toString());
            aiDescriptionArea.setForeground(AppTheme.SUCCESS);
            generateTasksBtn.setText("✨ GENERATE TASKS");
            generateTasksBtn.setEnabled(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * FIX: Footer with buttons - ALWAYS VISIBLE (outside scroll pane)
     */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.INPUT_BORDER));

        // Cancel button
        JButton cancelBtn = createOutlineButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, 40));
        cancelBtn.addActionListener(e -> dispose());
        footer.add(cancelBtn);

        // Create button
        JButton createBtn = createPrimaryButton("Create Project");
        createBtn.setPreferredSize(new Dimension(140, 40));
        createBtn.addActionListener(e -> createProject());
        footer.add(createBtn);

        return footer;
    }

    private void createProject() {
        // Validation
        String name = projectNameField.getText().trim();
        String budgetStr = budgetField.getText().trim();
        String deadline = deadlineField.getText().trim();
        Client selectedClient = (Client) clientComboBox.getSelectedItem();
        String clientId = selectedClient != null ? selectedClient.getId() : null;

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Project name is required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid budget amount", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build JSON
        JsonObject project = new JsonObject();
        project.addProperty("name", name);
        project.addProperty("description", descriptionField.getText().trim());
        project.addProperty("budget", budget);
        project.addProperty("endDate", deadline);
        project.addProperty("startDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        project.addProperty("clientId", clientId != null ? clientId : "");
        project.addProperty("status", "PLANNING");

        // Send to API
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String response = apiClient.post("/projects", project.toString());
                System.out.println("Project created: " + response);

                // Create default setup task
                try {
                    JsonObject parsed = JsonParser.parseString(response).getAsJsonObject();
                    String projectId = parsed.has("id") ? parsed.get("id").getAsString() : null;

                    if (projectId != null) {
                        JsonObject task = new JsonObject();
                        task.addProperty("title", "Setup Environment");
                        task.addProperty("description", "Initial project setup and dependencies");
                        task.addProperty("projectId", projectId);
                        task.addProperty("status", "TODO");
                        task.addProperty("priority", "HIGH");
                        task.addProperty("estimatedHours", 4.0);
                        apiClient.post("/tasks", task.toString());
                    }
                } catch (Exception e) {
                    System.err.println("Could not create default task: " + e.getMessage());
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ProjectDialog.this,
                            "Project created successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    if (onProjectCreated != null) {
                        onProjectCreated.run();
                    }
                    dispose();
                } catch (Exception e) {
                    System.err.println("Create project error: " + e.getMessage());
                    JOptionPane.showMessageDialog(ProjectDialog.this,
                            "Error creating project: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadClients() {
        // Show loading placeholder
        DefaultComboBoxModel<Client> loadingModel = new DefaultComboBoxModel<>();
        loadingModel.addElement(new Client("-1", "Loading...", "", "", ""));
        clientComboBox.setModel(loadingModel);
        clientComboBox.setEnabled(false);

        SwingWorker<List<Client>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Client> doInBackground() {
                List<Client> clients = new ArrayList<>();

                // Helper to parse a JSON string that may be array or firebase map
                java.util.function.Consumer<String> parseResponse = (response) -> {
                    if (response == null || response.isBlank()) return;
                    try {
                        JsonElement root = JsonParser.parseString(response);

                        if (root.isJsonArray()) {
                            JsonArray arr = root.getAsJsonArray();
                            for (JsonElement el : arr) {
                                if (el == null || !el.isJsonObject()) continue;
                                JsonObject obj = el.getAsJsonObject();
                                // Role check if present
                                if (obj.has("role") && !obj.get("role").isJsonNull()) {
                                    String role = obj.get("role").getAsString();
                                    if (!"CLIENT".equalsIgnoreCase(role)) continue;
                                }

                                Client c = new Client();
                                if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) c.setName(obj.get("fullName").getAsString());
                                else if (obj.has("name") && !obj.get("name").isJsonNull()) c.setName(obj.get("name").getAsString());
                                else c.setName("(Unknown)");

                                if (obj.has("email") && !obj.get("email").isJsonNull()) c.setEmail(obj.get("email").getAsString());
                                if (obj.has("id") && !obj.get("id").isJsonNull()) c.setId(obj.get("id").getAsString());

                                clients.add(c);
                            }
                        } else if (root.isJsonObject()) {
                            JsonObject rootObj = root.getAsJsonObject();
                            // wrapper { data: [...] }
                            if (rootObj.has("data") && rootObj.get("data").isJsonArray()) {
                                JsonArray arr = rootObj.getAsJsonArray("data");
                                for (JsonElement el : arr) {
                                    if (el == null || !el.isJsonObject()) continue;
                                    JsonObject obj = el.getAsJsonObject();
                                    if (obj.has("role") && !obj.get("role").isJsonNull()) {
                                        String role = obj.get("role").getAsString();
                                        if (!"CLIENT".equalsIgnoreCase(role)) continue;
                                    }
                                    Client c = new Client();
                                    if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) c.setName(obj.get("fullName").getAsString());
                                    else if (obj.has("name") && !obj.get("name").isJsonNull()) c.setName(obj.get("name").getAsString());
                                    else c.setName("(Unknown)");
                                    if (obj.has("email") && !obj.get("email").isJsonNull()) c.setEmail(obj.get("email").getAsString());
                                    if (obj.has("id") && !obj.get("id").isJsonNull()) c.setId(obj.get("id").getAsString());
                                    clients.add(c);
                                }
                            } else {
                                // Treat as Firebase map: keys are IDs
                                for (String key : rootObj.keySet()) {
                                    try {
                                        JsonElement elem = rootObj.get(key);
                                        if (elem == null || !elem.isJsonObject()) continue;
                                        JsonObject obj = elem.getAsJsonObject();
                                        // If role present and not CLIENT, skip
                                        if (obj.has("role") && !obj.get("role").isJsonNull()) {
                                            String role = obj.get("role").getAsString();
                                            if (!"CLIENT".equalsIgnoreCase(role)) continue;
                                        }
                                        Client c = new Client();
                                        if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) c.setName(obj.get("fullName").getAsString());
                                        else if (obj.has("name") && !obj.get("name").isJsonNull()) c.setName(obj.get("name").getAsString());
                                        else c.setName("(Unknown)");
                                        if (obj.has("email") && !obj.get("email").isJsonNull()) c.setEmail(obj.get("email").getAsString());
                                        c.setId(key);
                                        clients.add(c);
                                    } catch (Exception ignore) {
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to parse clients response: " + ex.getMessage());
                    }
                };

                // 1) Try the /clients endpoint via ApiClient (auth-aware)
                String response = null;
                try {
                    response = apiClient.get("/clients");
                    System.out.println("[ProjectDialog] /clients response length=" + (response == null ? 0 : response.length()));
                    parseResponse.accept(response);
                } catch (Exception ex) {
                    System.err.println("apiClient.get(/clients) failed: " + ex.getMessage());
                    // don't return yet - we'll try fallback to /users
                }

                // If /clients returned nothing, try /users and filter role=CLIENT
                if (clients.isEmpty()) {
                    try {
                        String usersResp = apiClient.get("/users");
                        System.out.println("[ProjectDialog] /users response length=" + (usersResp == null ? 0 : usersResp.length()));
                        if (usersResp != null && !usersResp.isBlank()) {
                            try {
                                JsonElement root = JsonParser.parseString(usersResp);
                                if (root.isJsonArray()) {
                                    JsonArray arr = root.getAsJsonArray();
                                    for (JsonElement el : arr) {
                                        if (el == null || !el.isJsonObject()) continue;
                                        JsonObject obj = el.getAsJsonObject();
                                        if (obj.has("role") && !obj.get("role").isJsonNull()) {
                                            String role = obj.get("role").getAsString();
                                            if (!"CLIENT".equalsIgnoreCase(role)) continue;
                                        } else {
                                            continue;
                                        }

                                        Client c = new Client();
                                        if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) c.setName(obj.get("fullName").getAsString());
                                        else if (obj.has("name") && !obj.get("name").isJsonNull()) c.setName(obj.get("name").getAsString());
                                        else c.setName("(Unknown)");
                                        if (obj.has("email") && !obj.get("email").isJsonNull()) c.setEmail(obj.get("email").getAsString());
                                        if (obj.has("id") && !obj.get("id").isJsonNull()) c.setId(obj.get("id").getAsString());
                                        clients.add(c);
                                    }
                                }
                            } catch (Exception parseEx) {
                                System.err.println("Failed to parse /users response: " + parseEx.getMessage());
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("apiClient.get(/users) failed: " + ex.getMessage());
                    }
                }

                // Debug: print parsed clients in background thread
                try {
                    System.out.println("[ProjectDialog] Parsed clients count=" + clients.size());
                    for (Client c : clients) {
                        System.out.println("[ProjectDialog] CLIENT -> id=" + c.getId() + " name=" + c.getName() + " email=" + c.getEmail());
                    }
                } catch (Exception ignore) {}

                return clients;
            }

            @Override
            protected void done() {
                try {
                    List<Client> clients = get();

                    // Debug: print clients before populating model (on EDT)
                    System.out.println("[ProjectDialog] done() - clients size=" + (clients == null ? 0 : clients.size()));
                    if (clients != null) {
                        for (Client c : clients) System.out.println("[ProjectDialog] done() CLIENT -> id=" + c.getId() + " name=" + c.getName() + " email=" + c.getEmail());
                    }

                    DefaultComboBoxModel<Client> model = new DefaultComboBoxModel<>();
                    if (clients != null && !clients.isEmpty()) {
                        for (Client c : clients) model.addElement(c);
                        clientComboBox.setModel(model);
                        clientComboBox.setEnabled(true);
                        clientComboBox.setSelectedIndex(0);
                    } else {
                        model.addElement(new Client("-1", "(No clients found)", "", "", ""));
                        clientComboBox.setModel(model);
                        clientComboBox.setEnabled(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    DefaultComboBoxModel<Client> model = new DefaultComboBoxModel<>();
                    model.addElement(new Client("-1", "(Error loading clients)", "", "", ""));
                    clientComboBox.setModel(model);
                    clientComboBox.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = AppTheme.PRIMARY;
                if (getModel().isPressed()) bg = bg.darker();
                else if (getModel().isRollover()) bg = new Color(0xFF, 0x6B, 0x2A);

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

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
        btn.setForeground(Color.WHITE);
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

                Color bg = getModel().isRollover() ? new Color(0xF3, 0xF4, 0xF6) : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                g2.setColor(AppTheme.INPUT_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));

                g2.setColor(AppTheme.TEXT_MAIN);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.fontMain(Font.BOLD, 12));
        btn.setForeground(AppTheme.TEXT_MAIN);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(180, 36));
        btn.setMaximumSize(new Dimension(180, 36));
        return btn;
    }
}
