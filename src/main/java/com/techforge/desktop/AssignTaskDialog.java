package com.techforge.desktop;

// Required imports for date formatting and mask input
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * AssignTaskDialog - AI Scouter Assignment dialog with Status and EstimatedHours.
 *
 * FIX: Added estimatedHours field (UML requirement).
 * FIX: Callback (onTaskCreated) is executed AFTER successful API response.
 */
public class AssignTaskDialog extends JDialog {

    private final ApiClient apiClient;
    private final String projectId;
    private final Runnable onTaskCreated;

    // Form fields
    private JTextField taskNameField;
    private JTextArea descriptionArea;
    private JComboBox<String> assigneeComboBox;
    private DefaultComboBoxModel<String> assigneeModel;
    private JComboBox<String> statusComboBox;
    private JComboBox<String> priorityComboBox;
    private JTextField estimatedHoursField;  // UML requirement
    private JFormattedTextField startDateField;  // NEW: Start Date (dd/MM/yyyy)
    private JFormattedTextField dueDateField;    // NEW: Due Date (dd/MM/yyyy)

    // Scan state
    private JPanel scanPanel;
    private JButton scanButton;
    private JPanel resultPanel;
    private boolean isScanned = false;

    // Employee data
    private List<EmployeeInfo> allEmployees = new ArrayList<>();
    private Map<String, EmployeeInfo> employeeMap = new HashMap<>();

    // Selected employee
    private String selectedEmployeeId;
    private String selectedEmployeeName;
    private String selectedEmployeeEmail;

    public AssignTaskDialog(Frame parent, ApiClient apiClient, String projectId, Runnable onTaskCreated) {
        super(parent, "AI Scouter Assignment", true);
        this.apiClient = apiClient;
        this.projectId = projectId;
        this.onTaskCreated = onTaskCreated;

        initializeUI();

        // Load employees AFTER UI is built
        SwingUtilities.invokeLater(this::loadEmployeesImmediately);
    }

    private void loadEmployeesImmediately() {
        System.out.println("Loading employees for dropdown...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String response = apiClient.get("/users");
                    JsonArray users = JsonParser.parseString(response).getAsJsonArray();

                    SwingUtilities.invokeLater(() -> {
                        assigneeModel.removeAllElements();
                        assigneeModel.addElement("-- Select Assignee --");
                        employeeMap.clear();
                        allEmployees.clear();

                        int count = 0;
                        for (JsonElement elem : users) {
                            JsonObject user = elem.getAsJsonObject();
                            String role = getJsonString(user, "role");

                            if ("EMPLOYEE".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
                                EmployeeInfo emp = new EmployeeInfo();
                                emp.id = getJsonString(user, "id");
                                emp.email = getJsonString(user, "email");
                                emp.name = getJsonString(user, "fullName");
                                if (emp.name.isEmpty()) emp.name = emp.email;
                                emp.role = role;
                                emp.powerLevel = 9000 + new Random().nextInt(100);

                                String displayName = emp.name + " (" + emp.email + ")";
                                assigneeModel.addElement(displayName);
                                employeeMap.put(displayName, emp);
                                allEmployees.add(emp);
                                count++;
                            }
                        }
                        System.out.println("Loaded " + count + " employees into dropdown");
                    });
                } catch (Exception e) {
                    System.err.println("Error loading employees: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void initializeUI() {
        setSize(500, 680);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Header
        add(createHeader(), BorderLayout.NORTH);

        // Content wrapped in scroll pane
        JPanel content = createContent();
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Footer (always visible)
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.INPUT_BORDER));

        JLabel iconLabel = new JLabel(UIUtils.getScouterIcon(28));
        header.add(iconLabel);

        JLabel title = new JLabel("Create Task");
        title.setFont(AppTheme.fontHeading(20));
        title.setForeground(AppTheme.HEADER_BLUE);
        header.add(title);

        return header;
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Task Name
        content.add(createFormGroup("Task Name *", taskNameField = createStyledTextField()));
        taskNameField.setEditable(true);
        content.add(Box.createVerticalStrut(12));

        // Description
        content.add(createTextAreaGroup("Description"));
        content.add(Box.createVerticalStrut(12));

        // Assignee Dropdown
        content.add(createAssigneeGroup());
        content.add(Box.createVerticalStrut(12));

        // Status Dropdown
        content.add(createStatusGroup());
        content.add(Box.createVerticalStrut(12));

        // Priority Dropdown
        content.add(createPriorityGroup());
        content.add(Box.createVerticalStrut(12));

        // NEW: Estimated Hours (UML requirement)
        content.add(createEstimatedHoursGroup());
        content.add(Box.createVerticalStrut(12));

        // NEW: Start Date Field
        content.add(createDateGroup("Start Date (dd/MM/yyyy)", true));
        content.add(Box.createVerticalStrut(12));

        // NEW: Due Date Field
        content.add(createDateGroup("Due Date (dd/MM/yyyy)", false));
        content.add(Box.createVerticalStrut(15));

        // Scan Section
        scanPanel = createScanSection();
        content.add(scanPanel);
        content.add(Box.createVerticalStrut(10));

        return content;
    }

    private JPanel createFormGroup(String labelText, JTextField field) {
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

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        group.add(field);

        return group;
    }

    private JPanel createTextAreaGroup(String labelText) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, AppTheme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane scroll = new JScrollPane(descriptionArea);
        scroll.setBorder(null);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(scroll);

        return group;
    }

    private JPanel createAssigneeGroup() {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel label = new JLabel("Assign To");
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        assigneeModel = new DefaultComboBoxModel<>();
        assigneeModel.addElement("-- Select Assignee --");
        assigneeComboBox = new JComboBox<>(assigneeModel);
        assigneeComboBox.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        assigneeComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        assigneeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        assigneeComboBox.addActionListener(e -> {
            String selected = (String) assigneeComboBox.getSelectedItem();
            if (selected != null && !selected.startsWith("--")) {
                EmployeeInfo emp = employeeMap.get(selected);
                if (emp != null) {
                    selectedEmployeeId = emp.id;
                    selectedEmployeeName = emp.name;
                    selectedEmployeeEmail = emp.email;
                }
            }
        });
        group.add(assigneeComboBox);

        return group;
    }

    private JPanel createStatusGroup() {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel label = new JLabel("Status");
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        statusComboBox = new JComboBox<>(new String[]{"TODO", "DOING", "DONE"});
        statusComboBox.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        statusComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        statusComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusComboBox.setSelectedIndex(0);
        group.add(statusComboBox);

        return group;
    }

    private JPanel createPriorityGroup() {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel label = new JLabel("Priority");
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        priorityComboBox = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
        priorityComboBox.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        priorityComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        priorityComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        priorityComboBox.setSelectedItem("MEDIUM");
        group.add(priorityComboBox);

        return group;
    }

    /**
     * NEW: Estimated Hours field (UML requirement)
     */
    private JPanel createEstimatedHoursGroup() {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel label = new JLabel("Estimated Hours");
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.HEADER_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(6));

        estimatedHoursField = createStyledTextField();
        estimatedHoursField.setText("4.0");
        estimatedHoursField.setToolTipText("Enter estimated hours (e.g., 4.0, 8, 2.5)");
        estimatedHoursField.setAlignmentX(Component.LEFT_ALIGNMENT);
        estimatedHoursField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        group.add(estimatedHoursField);

        return group;
    }

    /**
     * NEW: Date field group for Start Date and Due Date
     */
    private JPanel createDateGroup(String labelText, boolean isStartDate) {
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

        // Create formatted text field for date
        JFormattedTextField dateField;
        try {
            MaskFormatter dateMask = new MaskFormatter("##/##/####");
            dateMask.setPlaceholderCharacter('_');
            dateField = new JFormattedTextField(dateMask);
        } catch (ParseException e) {
            dateField = new JFormattedTextField();
        }

        dateField.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, AppTheme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        dateField.setToolTipText("Format: dd/MM/yyyy (e.g., 25/12/2025)");
        dateField.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Set default date to today for start, today+7 for due
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Calendar cal = Calendar.getInstance();
        if (!isStartDate) {
            cal.add(Calendar.DAY_OF_MONTH, 7);  // Due date defaults to 7 days later
        }
        dateField.setText(sdf.format(cal.getTime()));

        if (isStartDate) {
            startDateField = dateField;
        } else {
            dueDateField = dateField;
        }

        group.add(dateField);
        return group;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, AppTheme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private JPanel createScanSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Scan button
        scanButton = new JButton("SCAN POWER LEVELS") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = AppTheme.PRIMARY;
                if (getModel().isPressed()) bg = bg.darker();
                else if (getModel().isRollover()) bg = new Color(0xFF, 0x6B, 0x2A);

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
        scanButton.setFont(AppTheme.fontMain(Font.BOLD, 13));
        scanButton.setPreferredSize(new Dimension(200, 42));
        scanButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        scanButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        scanButton.setContentAreaFilled(false);
        scanButton.setBorderPainted(false);
        scanButton.setFocusPainted(false);
        scanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        scanButton.addActionListener(e -> scanPowerLevels());

        section.add(scanButton);
        section.add(Box.createVerticalStrut(15));

        // Result panel
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);
        resultPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.setVisible(false);
        section.add(resultPanel);

        return section;
    }

    private void scanPowerLevels() {
        scanButton.setText("Scanning...");
        scanButton.setEnabled(false);

        SwingWorker<List<EmployeeInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EmployeeInfo> doInBackground() {
                List<EmployeeInfo> employees = new ArrayList<>();
                try {
                    String response = apiClient.get("/users");
                    JsonArray users = JsonParser.parseString(response).getAsJsonArray();

                    for (JsonElement elem : users) {
                        JsonObject user = elem.getAsJsonObject();
                        String role = getJsonString(user, "role");

                        if ("EMPLOYEE".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
                            EmployeeInfo emp = new EmployeeInfo();
                            emp.id = getJsonString(user, "id");
                            emp.email = getJsonString(user, "email");
                            emp.name = getJsonString(user, "fullName");
                            if (emp.name.isEmpty()) emp.name = emp.email;
                            emp.role = role;
                            emp.powerLevel = 9000 + new Random().nextInt(100);
                            employees.add(emp);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return employees;
            }

            @Override
            protected void done() {
                try {
                    List<EmployeeInfo> employees = get();
                    javax.swing.Timer delayTimer = new javax.swing.Timer(800, evt -> {
                        showScanResult(employees);
                        scanButton.setText("SCAN POWER LEVELS");
                        scanButton.setEnabled(true);
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                } catch (Exception e) {
                    scanButton.setText("SCAN POWER LEVELS");
                    scanButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void showScanResult(List<EmployeeInfo> employees) {
        isScanned = true;
        scanButton.setVisible(false);
        resultPanel.removeAll();

        if (employees.isEmpty()) {
            JLabel noResult = new JLabel("No team members found!");
            noResult.setFont(AppTheme.fontMain(Font.ITALIC, 14));
            noResult.setForeground(AppTheme.DANGER);
            noResult.setAlignmentX(Component.CENTER_ALIGNMENT);
            resultPanel.add(noResult);
            resultPanel.setVisible(true);
            resultPanel.revalidate();
            return;
        }

        // Update dropdown
        allEmployees = employees;
        assigneeModel.removeAllElements();
        employeeMap.clear();

        for (EmployeeInfo emp : employees) {
            String displayName = emp.name + " (" + emp.email + ")";
            assigneeModel.addElement(displayName);
            employeeMap.put(displayName, emp);
        }

        // Best match
        employees.sort((a, b) -> Integer.compare(b.powerLevel, a.powerLevel));
        EmployeeInfo bestMatch = employees.get(0);
        selectedEmployeeId = bestMatch.id;
        selectedEmployeeName = bestMatch.name;
        selectedEmployeeEmail = bestMatch.email;

        String bestMatchDisplay = bestMatch.name + " (" + bestMatch.email + ")";
        assigneeComboBox.setSelectedItem(bestMatchDisplay);

        // Success box
        JPanel successBox = createSuccessBox(bestMatch);
        resultPanel.add(successBox);
        resultPanel.setVisible(true);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel createSuccessBox(EmployeeInfo bestMatch) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xDC, 0xFC, 0xE7));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0x10, 0xB9, 0x81));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        box.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.setMaximumSize(new Dimension(380, 120));

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        headerRow.setOpaque(false);
        JLabel checkIcon = new JLabel(UIUtils.getCheckIcon(20));
        headerRow.add(checkIcon);
        JLabel successLabel = new JLabel("AI Recommendation: " + bestMatch.name);
        successLabel.setFont(AppTheme.fontMain(Font.BOLD, 14));
        successLabel.setForeground(AppTheme.SUCCESS);
        headerRow.add(successLabel);
        box.add(headerRow);
        box.add(Box.createVerticalStrut(6));

        JLabel powerLabel = new JLabel("Power Level: " + bestMatch.powerLevel + "+");
        powerLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        powerLabel.setForeground(AppTheme.PRIMARY);
        powerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(powerLabel);
        box.add(Box.createVerticalStrut(6));

        JLabel infoLabel = new JLabel("You can change assignee in dropdown above");
        infoLabel.setFont(AppTheme.fontMain(Font.ITALIC, 11));
        infoLabel.setForeground(AppTheme.TEXT_LIGHT);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(infoLabel);

        return box;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.INPUT_BORDER));

        JButton cancelBtn = createOutlineButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, 40));
        cancelBtn.addActionListener(e -> dispose());
        footer.add(cancelBtn);

        JButton createBtn = createPrimaryButton("Create Task");
        createBtn.setPreferredSize(new Dimension(130, 40));
        createBtn.addActionListener(e -> createTask());
        footer.add(createBtn);

        return footer;
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

                g2.setColor(AppTheme.BORDER);
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
        btn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Create task with validation and proper callback execution.
     */
    private void createTask() {
        // Validation
        String taskName = taskNameField.getText().trim();
        if (taskName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task name is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate estimatedHours (must be valid double)
        double estimatedHours;
        try {
            String hoursText = estimatedHoursField.getText().trim();
            if (hoursText.isEmpty()) {
                estimatedHours = 0.0;
            } else {
                estimatedHours = Double.parseDouble(hoursText);
                if (estimatedHours < 0) {
                    throw new NumberFormatException("Negative hours");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid estimated hours. Please enter a valid number (e.g., 4.0)",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            estimatedHoursField.requestFocus();
            return;
        }

        final double finalEstimatedHours = estimatedHours;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                JsonObject task = new JsonObject();
                task.addProperty("title", taskName);
                task.addProperty("description", descriptionArea.getText().trim());

                // Status from dropdown
                String selectedStatus = (String) statusComboBox.getSelectedItem();
                task.addProperty("status", selectedStatus != null ? selectedStatus : "TODO");

                // Priority from dropdown
                String selectedPriority = (String) priorityComboBox.getSelectedItem();
                task.addProperty("priority", selectedPriority != null ? selectedPriority : "MEDIUM");

                // Estimated Hours (UML requirement)
                task.addProperty("estimatedHours", finalEstimatedHours);

                // NEW: Start Date and Due Date
                String startDateText = startDateField.getText().trim().replace("_", "");
                String dueDateText = dueDateField.getText().trim().replace("_", "");

                if (!startDateText.isEmpty() && startDateText.length() == 10) {
                    task.addProperty("startDate", startDateText);
                }
                if (!dueDateText.isEmpty() && dueDateText.length() == 10) {
                    task.addProperty("dueDate", dueDateText);
                }

                // Assignee
                if (selectedEmployeeEmail != null && !selectedEmployeeEmail.isEmpty()) {
                    task.addProperty("assigneeEmail", selectedEmployeeEmail);
                }
                if (selectedEmployeeId != null && !selectedEmployeeId.isEmpty()) {
                    task.addProperty("assignedUserId", selectedEmployeeId);
                }

                // Project ID
                if (projectId != null && !projectId.isEmpty()) {
                    task.addProperty("projectId", projectId);
                }

                System.out.println("Creating task: " + task);
                apiClient.post("/tasks", task.toString());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions

                    String assigneeName = selectedEmployeeName != null ? selectedEmployeeName : "team";
                    JOptionPane.showMessageDialog(AssignTaskDialog.this,
                            "Task created successfully and assigned to " + assigneeName + "!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    // FIX: Execute callback AFTER successful API response
                    if (onTaskCreated != null) {
                        System.out.println("Executing onTaskCreated callback...");
                        onTaskCreated.run();
                    }

                    dispose();

                } catch (Exception e) {
                    System.err.println("Create task error: " + e.getMessage());
                    JOptionPane.showMessageDialog(AssignTaskDialog.this,
                            "Error creating task: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    // Employee info holder
    private static class EmployeeInfo {
        String id;
        String email;
        String name;
        String role;
        int powerLevel;
    }

    // Custom rounded border
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
    }
}

