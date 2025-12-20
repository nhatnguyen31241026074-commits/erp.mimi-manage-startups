package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Forgot Password Dialog - Two-step OTP verification flow.
 * Step 1: Enter email -> Send OTP
 * Step 2: Enter OTP + New Password -> Reset
 */
public class ForgotPasswordDialog extends JDialog {

    private final ApiClient apiClient;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    // Step 1 components
    private JTextField emailField;
    private JButton sendOtpButton;
    private JLabel step1StatusLabel;

    // Step 2 components
    private JTextField otpField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton resetButton;
    private JLabel step2StatusLabel;
    private JLabel emailDisplayLabel;

    private String currentEmail;

    public ForgotPasswordDialog(JFrame parent) {
        super(parent, "Reset Password", true);
        this.apiClient = new ApiClient();
        initializeUI();
    }

    private void initializeUI() {
        setSize(450, 550);  // Increased height for better visibility
        setLocationRelativeTo(getParent());
        setResizable(false);

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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content with CardLayout for step switching - wrapped in JScrollPane for overflow
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        contentPanel.add(createStep1Panel(), "STEP1");
        contentPanel.add(createStep2Panel(), "STEP2");

        // Wrap content in JScrollPane for overflow handling
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Icon
        JLabel icon = new JLabel("🔐");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(icon);
        panel.add(Box.createVerticalStrut(10));

        // Title
        JLabel title = new JLabel("Reset Password");
        title.setFont(AppTheme.fontHeading(24));
        title.setForeground(AppTheme.SECONDARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(5));

        // Subtitle
        JLabel subtitle = new JLabel("We'll send you a verification code");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_LIGHT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle);

        return panel;
    }

    private JPanel createStep1Panel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Email field
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(AppTheme.fontMain(Font.BOLD, 13));
        emailLabel.setForeground(AppTheme.SECONDARY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailLabel);
        panel.add(Box.createVerticalStrut(8));

        emailField = new JTextField();
        emailField.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        emailField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppTheme.BORDER, 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSendOtp();
                }
            }
        });
        panel.add(emailField);
        panel.add(Box.createVerticalStrut(15));

        // Status label
        step1StatusLabel = new JLabel(" ");
        step1StatusLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        step1StatusLabel.setForeground(AppTheme.DANGER);
        step1StatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(step1StatusLabel);
        panel.add(Box.createVerticalStrut(15));

        // Send OTP button
        sendOtpButton = createStyledButton("Send OTP Code", AppTheme.PRIMARY);
        sendOtpButton.addActionListener(e -> handleSendOtp());
        panel.add(sendOtpButton);

        return panel;
    }

    private JPanel createStep2Panel() {
        // Main panel with BorderLayout to ensure button at bottom
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setOpaque(false);

        // Form content panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        // Email display
        emailDisplayLabel = new JLabel("OTP sent to: ");
        emailDisplayLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        emailDisplayLabel.setForeground(AppTheme.SUCCESS);
        emailDisplayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(emailDisplayLabel);
        formPanel.add(Box.createVerticalStrut(15));

        // OTP field
        JLabel otpLabel = new JLabel("Enter OTP Code");
        otpLabel.setFont(AppTheme.fontMain(Font.BOLD, 13));
        otpLabel.setForeground(AppTheme.SECONDARY);
        otpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(otpLabel);
        formPanel.add(Box.createVerticalStrut(8));

        otpField = new JTextField();
        otpField.setFont(AppTheme.fontMono(Font.BOLD, 20));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        otpField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppTheme.PRIMARY, 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        otpField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(otpField);
        formPanel.add(Box.createVerticalStrut(15));

        // New Password
        JLabel pwdLabel = new JLabel("New Password");
        pwdLabel.setFont(AppTheme.fontMain(Font.BOLD, 13));
        pwdLabel.setForeground(AppTheme.SECONDARY);
        pwdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(pwdLabel);
        formPanel.add(Box.createVerticalStrut(8));

        newPasswordField = new JPasswordField();
        newPasswordField.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        newPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        newPasswordField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppTheme.BORDER, 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        newPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(newPasswordField);
        formPanel.add(Box.createVerticalStrut(12));

        // Confirm Password
        JLabel confirmLabel = new JLabel("Confirm Password");
        confirmLabel.setFont(AppTheme.fontMain(Font.BOLD, 13));
        confirmLabel.setForeground(AppTheme.SECONDARY);
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(confirmLabel);
        formPanel.add(Box.createVerticalStrut(8));

        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(AppTheme.BORDER, 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(confirmPasswordField);
        formPanel.add(Box.createVerticalStrut(10));

        // Status label
        step2StatusLabel = new JLabel(" ");
        step2StatusLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        step2StatusLabel.setForeground(AppTheme.DANGER);
        step2StatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(step2StatusLabel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel at bottom - ALWAYS VISIBLE
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Confirm Reset Button (Orange, prominent)
        resetButton = createStyledButton("✓ Confirm Reset", AppTheme.PRIMARY);
        resetButton.addActionListener(e -> handleResetPassword());
        buttonPanel.add(resetButton, BorderLayout.CENTER);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JLabel backLink = new JLabel("← Back to Login");
        backLink.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        backLink.setForeground(AppTheme.TEXT_LIGHT);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                backLink.setForeground(AppTheme.PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                backLink.setForeground(AppTheme.TEXT_LIGHT);
            }
        });
        panel.add(backLink);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isPressed() ? bgColor.darker() :
                           getModel().isRollover() ? bgColor.brighter() : bgColor;

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
        button.setFont(AppTheme.fontMain(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void handleSendOtp() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showStep1Error("Please enter your email address.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showStep1Error("Please enter a valid email address.");
            return;
        }

        sendOtpButton.setEnabled(false);
        sendOtpButton.setText("Sending...");
        step1StatusLabel.setText(" ");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                JsonObject request = new JsonObject();
                request.addProperty("email", email);
                return apiClient.post("/auth/forgot-password", request.toString());
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    JsonObject result = JsonParser.parseString(response).getAsJsonObject();

                    if (result.has("message")) {
                        currentEmail = email;
                        emailDisplayLabel.setText("✓ OTP sent to: " + email);
                        cardLayout.show(contentPanel, "STEP2");
                        otpField.requestFocus();
                    } else if (result.has("error")) {
                        showStep1Error(result.get("error").getAsString());
                    }
                } catch (Exception e) {
                    showStep1Error("Connection error. Please try again.");
                    e.printStackTrace();
                } finally {
                    sendOtpButton.setEnabled(true);
                    sendOtpButton.setText("Send OTP Code");
                }
            }
        };
        worker.execute();
    }

    private void handleResetPassword() {
        String otp = otpField.getText().trim();
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Validation
        if (otp.isEmpty()) {
            showStep2Error("Please enter the OTP code.");
            return;
        }
        if (otp.length() != 6) {
            showStep2Error("OTP must be 6 digits.");
            return;
        }
        if (newPassword.isEmpty()) {
            showStep2Error("Please enter a new password.");
            return;
        }
        if (newPassword.length() < 3) {
            showStep2Error("Password must be at least 3 characters.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showStep2Error("Passwords do not match.");
            return;
        }

        resetButton.setEnabled(false);
        resetButton.setText("Resetting...");
        step2StatusLabel.setText(" ");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                JsonObject request = new JsonObject();
                request.addProperty("email", currentEmail);
                request.addProperty("otp", otp);
                request.addProperty("newPassword", newPassword);
                return apiClient.post("/auth/reset-password", request.toString());
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    JsonObject result = JsonParser.parseString(response).getAsJsonObject();

                    if (result.has("message") && !result.has("error")) {
                        // SUCCESS! Show success toast with custom icon
                        showSuccessToast("Password Reset Successful!",
                            "Your password has been updated.\nYou can now login with your new password.");
                        dispose();
                    } else if (result.has("error")) {
                        String errorMsg = result.get("error").getAsString();
                        // Check if it's an OTP error (invalid/expired)
                        if (errorMsg.toLowerCase().contains("otp") ||
                            errorMsg.toLowerCase().contains("invalid") ||
                            errorMsg.toLowerCase().contains("expired") ||
                            errorMsg.toLowerCase().contains("code")) {
                            // Show funny Vietnamese error message as requested
                            showOtpErrorDialog();
                        } else {
                            showStep2Error(errorMsg);
                        }
                    } else {
                        // Unknown response but no error - treat as success
                        showSuccessToast("Password Reset!", "Your password has been updated.");
                        dispose();
                    }
                } catch (Exception e) {
                    // Network/parsing error - show funny OTP error message
                    String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (errorMessage.contains("otp") || errorMessage.contains("invalid") ||
                        errorMessage.contains("400") || errorMessage.contains("401")) {
                        showOtpErrorDialog();
                    } else {
                        showStep2Error("Connection error. Please try again.");
                    }
                    e.printStackTrace();
                } finally {
                    resetButton.setEnabled(true);
                    resetButton.setText("✓ Confirm Reset");
                }
            }
        };
        worker.execute();
    }

    /**
     * Shows a success toast notification.
     */
    private void showSuccessToast(String title, String message) {
        // Create a custom success dialog with green styling
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0xD1, 0xFA, 0xE5)); // Light green background
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.SUCCESS, 2),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel iconLabel = new JLabel("✅");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.fontMain(Font.BOLD, 16));
        titleLabel.setForeground(AppTheme.SUCCESS.darker());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));

        JLabel msgLabel = new JLabel("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
        msgLabel.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        msgLabel.setForeground(AppTheme.TEXT_MAIN);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msgLabel);

        JOptionPane.showMessageDialog(
            this,
            panel,
            "Success",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    /**
     * Shows the custom OTP error dialog with funny Vietnamese message.
     */
    private void showOtpErrorDialog() {
        // Create a custom error panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0xFE, 0xE2, 0xE2)); // Light red background
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.DANGER, 2),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));

        JLabel iconLabel = new JLabel("❌");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));

        // THE REQUESTED FUNNY ERROR MESSAGE
        JLabel msgLabel = new JLabel("Lỗi OTP rồi BẠN ỚI!");
        msgLabel.setFont(AppTheme.fontMain(Font.BOLD, 18));
        msgLabel.setForeground(AppTheme.DANGER);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msgLabel);
        panel.add(Box.createVerticalStrut(5));

        JLabel hintLabel = new JLabel("Please check your OTP code and try again.");
        hintLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        hintLabel.setForeground(AppTheme.TEXT_LIGHT);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hintLabel);

        JOptionPane.showMessageDialog(
            this,
            panel,
            "OTP Error",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showStep1Error(String message) {
        step1StatusLabel.setText("⚠ " + message);
        step1StatusLabel.setForeground(AppTheme.DANGER);
    }

    private void showStep2Error(String message) {
        step2StatusLabel.setText("⚠ " + message);
        step2StatusLabel.setForeground(AppTheme.DANGER);
    }
}

