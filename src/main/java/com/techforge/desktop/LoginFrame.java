package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Login Frame - Replicates the web login UI in Swing.
 * Matches the .login-wrapper and .login-card CSS classes.
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginButton;
    private final ApiClient apiClient;

    public LoginFrame() {
        this.apiClient = new ApiClient();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("TechForge ERP - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true); // Allow resizing for small screens

        // Main panel with gradient background
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new GridBagLayout());
        setContentPane(mainPanel);

        // Login card (white box) wrapped in scroll pane
        JPanel loginCard = createLoginCard();

        // Create scroll pane for small screens
        JScrollPane scrollPane = new JScrollPane(loginCard);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Set preferred size for the scroll pane
        scrollPane.setPreferredSize(new Dimension(440, 620));

        mainPanel.add(scrollPane);
    }

    private JPanel createLoginCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 580));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Logo with Dragon icon - using Unicode dragon that works on Windows
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Dragon icon using emoji that renders properly
        JLabel dragonIcon = new JLabel("\uD83D\uDC09"); // 🐉 Dragon emoji
        dragonIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        logoPanel.add(dragonIcon);

        JLabel logoText = new JLabel("TechForge");
        logoText.setFont(AppTheme.fontHeading(36));
        logoText.setForeground(AppTheme.PRIMARY);
        logoPanel.add(logoText);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(5));

        // Subtitle
        JLabel subtitle = new JLabel("Enterprise Resource Planning - Saiyan Edition");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_LIGHT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        // Error message (hidden by default)
        errorLabel = new JLabel("Invalid credentials. Try again.");
        errorLabel.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        errorLabel.setForeground(AppTheme.DANGER);
        errorLabel.setOpaque(true);
        errorLabel.setBackground(new Color(254, 226, 226));
        errorLabel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(15));

        // Username field
        card.add(createFieldPanel("Username / Email", false));
        card.add(Box.createVerticalStrut(15));

        // Password field
        card.add(createFieldPanel("Password", true));
        card.add(Box.createVerticalStrut(25));

        // Login button
        loginButton = createStyledButton("LOGIN →", AppTheme.PRIMARY);
        loginButton.addActionListener(e -> handleLogin());
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));

        // Forgot Password link
        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        forgotPanel.setOpaque(false);
        forgotPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel forgotLink = new JLabel("Forgot Password?");
        forgotLink.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        forgotLink.setForeground(AppTheme.PRIMARY);
        forgotLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showForgotPasswordDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                forgotLink.setText("<html><u>Forgot Password?</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                forgotLink.setText("Forgot Password?");
            }
        });
        forgotPanel.add(forgotLink);
        card.add(forgotPanel);
        card.add(Box.createVerticalStrut(15));

        // Sign Up link
        JPanel signUpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        signUpPanel.setOpaque(false);
        signUpPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        noAccountLabel.setForeground(AppTheme.TEXT_LIGHT);
        signUpPanel.add(noAccountLabel);

        JLabel signUpLink = new JLabel("Sign Up");
        signUpLink.setFont(AppTheme.fontMain(Font.BOLD, 13));
        signUpLink.setForeground(AppTheme.PRIMARY);
        signUpLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signUpLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegisterFrame();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                signUpLink.setText("<html><u>Sign Up</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                signUpLink.setText("Sign Up");
            }
        });
        signUpPanel.add(signUpLink);

        card.add(signUpPanel);
        card.add(Box.createVerticalStrut(15));

        // Terms & Privacy Policy link
        JLabel termsLink = new JLabel("Terms & Privacy Policy");
        termsLink.setFont(AppTheme.fontMain(Font.PLAIN, 11));
        termsLink.setForeground(AppTheme.TEXT_LIGHT);
        termsLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        termsLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        termsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showTermsDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                termsLink.setForeground(AppTheme.PRIMARY);
                termsLink.setText("<html><u>Terms & Privacy Policy</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                termsLink.setForeground(AppTheme.TEXT_LIGHT);
                termsLink.setText("Terms & Privacy Policy");
            }
        });
        card.add(termsLink);
        card.add(Box.createVerticalStrut(15));

        // Footer - Demo credentials
        JPanel footerPanel = createFooterPanel();
        card.add(footerPanel);

        return card;
    }

    private JPanel createFieldPanel(String labelText, boolean isPassword) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(340, 80));

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 13));
        label.setForeground(AppTheme.TEXT_MAIN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));

        JTextField field;
        if (isPassword) {
            passwordField = new JPasswordField();
            field = passwordField;
        } else {
            usernameField = new JTextField();
            field = usernameField;
        }

        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(320, 45));
        field.setMaximumSize(new Dimension(320, 45));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER, 2, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(AppTheme.PRIMARY, 2, true),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(AppTheme.BORDER, 2, true),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15)
                ));
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });

        panel.add(field);
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }

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

        button.setFont(AppTheme.fontMain(Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(320, 48));
        button.setMaximumSize(new Dimension(320, 48));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel demoBox = new JPanel();
        demoBox.setLayout(new BoxLayout(demoBox, BoxLayout.Y_AXIS));
        demoBox.setBackground(new Color(243, 244, 246));
        demoBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        demoBox.setMaximumSize(new Dimension(320, 100));
        demoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel demoTitle = new JLabel("Demo Credentials:");
        demoTitle.setFont(AppTheme.fontMain(Font.BOLD, 12));
        demoTitle.setForeground(AppTheme.TEXT_MAIN);
        demoBox.add(demoTitle);
        demoBox.add(Box.createVerticalStrut(5));

        JLabel demoLine1 = new JLabel("<html>Email: <b>vegeta@saiyan.com</b> (Manager)</html>");
        demoLine1.setFont(AppTheme.fontMain(Font.PLAIN, 11));
        demoLine1.setForeground(AppTheme.TEXT_LIGHT);
        demoBox.add(demoLine1);

        JLabel demoLine2 = new JLabel("<html>Email: <b>bulma@capsule.corp</b> (Admin)</html>");
        demoLine2.setFont(AppTheme.fontMain(Font.PLAIN, 11));
        demoLine2.setForeground(AppTheme.TEXT_LIGHT);
        demoBox.add(demoLine2);

        JLabel demoLine3 = new JLabel("<html>Password: <b>123</b></html>");
        demoLine3.setFont(AppTheme.fontMain(Font.PLAIN, 11));
        demoLine3.setForeground(AppTheme.TEXT_LIGHT);
        demoBox.add(demoLine3);

        footer.add(demoBox);
        return footer;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return apiClient.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();

                    if (result.has("userId")) {
                        String userName = result.has("user") && result.getAsJsonObject("user").has("fullName")
                                ? result.getAsJsonObject("user").get("fullName").getAsString()
                                : username;

                        // Get user role and ID for notifications
                        String userRole = result.has("role") ? result.get("role").getAsString() : "EMPLOYEE";
                        String userId = result.has("userId") ? result.get("userId").getAsString() : "";

                        SwingUtilities.invokeLater(() -> {
                            MainDashboardFrame dashboard = new MainDashboardFrame(userName, apiClient);
                            dashboard.setVisible(true);
                            LoginFrame.this.dispose();

                            // Employee Notification: Show pending tasks count after login
                            if ("EMPLOYEE".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole)) {
                                checkAndNotifyPendingTasks(userId, userName);
                            }
                        });
                    } else {
                        showError("Login failed. Please check your credentials.");
                    }
                } catch (Exception e) {
                    showError("Connection error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("LOGIN →");
                }
            }
        };

        worker.execute();
    }

    /**
     * Check for pending tasks assigned to the user and show notification
     */
    private void checkAndNotifyPendingTasks(String userId, String userName) {
        SwingWorker<Integer, Void> taskWorker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                try {
                    String tasksResponse = apiClient.get("/tasks");
                    JsonArray tasks = JsonParser.parseString(tasksResponse).getAsJsonArray();

                    int todoCount = 0;
                    for (com.google.gson.JsonElement element : tasks) {
                        JsonObject task = element.getAsJsonObject();
                        String assignedUserId = task.has("assignedUserId") && !task.get("assignedUserId").isJsonNull()
                            ? task.get("assignedUserId").getAsString() : "";
                        String status = task.has("status") && !task.get("status").isJsonNull()
                            ? task.get("status").getAsString() : "";

                        // Count TODO tasks assigned to this user
                        if (userId.equals(assignedUserId) && "TODO".equalsIgnoreCase(status)) {
                            todoCount++;
                        }
                    }
                    return todoCount;
                } catch (Exception e) {
                    System.err.println("Error checking pending tasks: " + e.getMessage());
                    return 0;
                }
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    if (count > 0) {
                        JOptionPane.showMessageDialog(null,
                            "Welcome " + userName + "!\n\n" +
                            "You have " + count + " new task" + (count > 1 ? "s" : "") + " assigned!\n\n" +
                            "Check your Execution Deck to get started.",
                            "Task Notification",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    // Silently ignore notification errors
                }
            }
        };
        taskWorker.execute();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        final int originalX = getX();
        Timer timer = new Timer(50, null);
        final int[] count = {0};
        timer.addActionListener(e -> {
            count[0]++;
            if (count[0] <= 6) {
                setLocation(originalX + (count[0] % 2 == 0 ? 10 : -10), getY());
            } else {
                setLocation(originalX, getY());
                timer.stop();
            }
        });
        timer.start();
    }

    private void openRegisterFrame() {
        SwingUtilities.invokeLater(() -> {
            RegisterFrame registerFrame = new RegisterFrame();
            registerFrame.setVisible(true);
            LoginFrame.this.dispose();
        });
    }

    private void showForgotPasswordDialog() {
        ForgotPasswordDialog dialog = new ForgotPasswordDialog(this);
        dialog.setVisible(true);
    }

    private void showTermsDialog() {
        JOptionPane.showMessageDialog(this,
                "TECHFORGE ERP - Terms of Service\n\n" +
                "By using TechForge ERP, you agree to:\n\n" +
                "• Maintain confidentiality of your credentials\n" +
                "• Use the system for authorized purposes only\n" +
                "• Accept our data privacy practices\n\n" +
                "For full terms, please visit Sign Up page.\n\n" +
                "© 2025 TechForge. All rights reserved.",
                "Terms & Privacy Policy",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GradientPaint gradient = new GradientPaint(
                    0, 0, AppTheme.GRADIENT_START,
                    getWidth(), getHeight(), AppTheme.GRADIENT_END
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}

