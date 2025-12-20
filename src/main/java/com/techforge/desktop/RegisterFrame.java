package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import com.google.gson.JsonObject;

/**
 * Register Frame - Modern registration form with polished UI.
 */
public class RegisterFrame extends JFrame {

    private JTextField fullNameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField secretCodeField;
    private JCheckBox termsCheckbox;  // NEW: T&C Checkbox
    private JLabel errorLabel;
    private JButton registerButton;
    private final ApiClient apiClient;

    public RegisterFrame() {
        this.apiClient = new ApiClient();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("TechForge ERP - Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true); // Allow resizing for small screens

        // Gradient background panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                // keep antialiasing only (remove conflicting KEY_RENDERING)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                GradientPaint gradient = new GradientPaint(
                        0, 0, AppTheme.SECONDARY,
                        getWidth(), getHeight(), new Color(0x03, 0x0A, 0x30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());

        // Register card - DO NOT set fixed preferredSize to allow scrolling
        JPanel registerCard = createRegisterCard();

        // Wrap the card in a scroll pane for small screens
        JScrollPane scrollPane = new JScrollPane(registerCard);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Use GridBagConstraints to control scroll pane size within the background
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;  // Changed to BOTH to allow proper sizing
        gbc.anchor = GridBagConstraints.CENTER;

        // Set max width for the scroll pane - allow height to expand
        scrollPane.setPreferredSize(new Dimension(500, 700));
        scrollPane.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        backgroundPanel.add(scrollPane, gbc);
        setContentPane(backgroundPanel);
    }

    private JPanel createRegisterCard() {
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
        // Remove fixed preferredSize - let BoxLayout calculate it based on content
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(35, 45, 35, 45));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dragonIcon = new JLabel("🐉");
        dragonIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        logoPanel.add(dragonIcon);

        JLabel logo = new JLabel("TechForge");
        logo.setFont(AppTheme.fontHeading(32));
        logo.setForeground(AppTheme.PRIMARY);
        logoPanel.add(logo);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(8));

        // Subtitle
        JLabel subtitle = new JLabel("Create Your Account");
        subtitle.setFont(AppTheme.fontMain(Font.BOLD, 16));
        subtitle.setForeground(AppTheme.TEXT_MAIN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(25));

        // Error label
        errorLabel = new JLabel("");
        errorLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.DANGER);
        errorLabel.setOpaque(true);
        errorLabel.setBackground(new Color(254, 226, 226));
        errorLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setMaximumSize(new Dimension(380, 40));
        errorLabel.setVisible(false);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        // Form fields
        fullNameField = createFormField(card, "Full Name");
        emailField = createFormField(card, "Email Address");
        passwordField = createPasswordField(card, "Password");
        confirmPasswordField = createPasswordField(card, "Confirm Password");
        secretCodeField = createFormField(card, "Secret Company Code (Optional)");

        card.add(Box.createVerticalStrut(10));

        // Terms & Conditions section
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        termsPanel.setOpaque(false);
        termsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        termsPanel.setMaximumSize(new Dimension(380, 30));

        termsCheckbox = new JCheckBox();
        termsCheckbox.setOpaque(false);
        termsCheckbox.setFocusPainted(false);
        termsPanel.add(termsCheckbox);

        JLabel termsLabel = new JLabel("I agree to the ");
        termsLabel.setFont(AppTheme.fontMain(Font.PLAIN, 12));
        termsLabel.setForeground(AppTheme.TEXT_MAIN);
        termsPanel.add(termsLabel);

        JLabel termsLink = new JLabel("Terms & Privacy Policy");
        termsLink.setFont(AppTheme.fontMain(Font.BOLD, 12));
        termsLink.setForeground(AppTheme.PRIMARY);
        termsLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Use hover underline only (no movement)
        termsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showTermsAndConditions();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                termsLink.setText("<html><u>Terms & Privacy Policy</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                termsLink.setText("Terms & Privacy Policy");
            }
        });
        termsPanel.add(termsLink);

        card.add(termsPanel);
        card.add(Box.createVerticalStrut(15));

        // Register button - full width orange
        registerButton = new JButton("CREATE ACCOUNT") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // set anti-aliasing only
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor = getModel().isPressed() ? AppTheme.PRIMARY.darker() :
                        getModel().isRollover() ? AppTheme.PRIMARY.brighter() : AppTheme.PRIMARY;

                g2.setColor(bgColor);
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
        registerButton.setFont(AppTheme.fontMain(Font.BOLD, 15));
        registerButton.setForeground(Color.WHITE);
        registerButton.setMaximumSize(new Dimension(380, 50));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> handleRegister());
        card.add(registerButton);

        card.add(Box.createVerticalStrut(20));

        // Login link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        linkPanel.setOpaque(false);
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel alreadyLabel = new JLabel("Already have an account?");
        alreadyLabel.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        alreadyLabel.setForeground(AppTheme.TEXT_LIGHT);
        linkPanel.add(alreadyLabel);

        JLabel loginLink = new JLabel("Sign In");
        loginLink.setFont(AppTheme.fontMain(Font.BOLD, 13));
        loginLink.setForeground(AppTheme.PRIMARY);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                loginLink.setText("<html><u>Sign In</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                loginLink.setText("Sign In");
            }
        });
        linkPanel.add(loginLink);

        card.add(linkPanel);
        card.add(Box.createVerticalStrut(20));

        // Secret code hints
        card.add(createSecretCodeHints());

        // Title with icon fix
        JLabel lblTitle = new JLabel("⚖️ Terms of Service & Privacy Policy");
        lblTitle.setFont(AppTheme.fontMain(Font.BOLD, 16));
        lblTitle.setForeground(AppTheme.PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));

        return card;
    }

    private JTextField createFormField(JPanel parent, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 12));
        label.setForeground(AppTheme.SECONDARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(380, 20));
        parent.add(label);
        parent.add(Box.createVerticalStrut(8));

        JTextField field = new JTextField();
        styleInputField(field);
        parent.add(field);
        parent.add(Box.createVerticalStrut(15));

        return field;
    }

    private JPasswordField createPasswordField(JPanel parent, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.fontMain(Font.BOLD, 12));
        label.setForeground(AppTheme.SECONDARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(380, 20));
        parent.add(label);
        parent.add(Box.createVerticalStrut(8));

        JPasswordField field = new JPasswordField();
        styleInputField(field);
        parent.add(field);
        parent.add(Box.createVerticalStrut(15));

        return field;
    }

    private void styleInputField(JTextField field) {
        field.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(380, 48));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.PRIMARY, 2, true),
                        BorderFactory.createEmptyBorder(11, 14, 11, 14)
                    ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                    ));
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRegister();
                }
            }
        });
    }

    private JPanel createSecretCodeHints() {
        JPanel hintBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // set anti-aliasing only
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        hintBox.setOpaque(false);
        hintBox.setLayout(new BoxLayout(hintBox, BoxLayout.Y_AXIS));
        hintBox.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        hintBox.setMaximumSize(new Dimension(380, 90));
        hintBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintTitle = new JLabel("Secret Codes (for testing):");
        hintTitle.setFont(AppTheme.fontMain(Font.BOLD, 11));
        hintTitle.setForeground(AppTheme.TEXT_MAIN);
        hintTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintBox.add(hintTitle);
        hintBox.add(Box.createVerticalStrut(5));

        String[] hints = {
            "KAME_HOUSE → Employee",
            "SAIYAN_GOD → Manager",
            "CAPSULE_CORP → Admin",
            "FRIEZA_FORCE → Client",
            "(Empty) → Default Employee"
        };
        for (String hint : hints) {
            JLabel hintLabel = new JLabel("• " + hint);
            hintLabel.setFont(AppTheme.fontMain(Font.PLAIN, 10));
            hintLabel.setForeground(AppTheme.TEXT_LIGHT);
            hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            hintBox.add(hintLabel);
        }

        return hintBox;
    }

    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String secretCode = secretCodeField.getText().trim(); // Get secret code value

        // Validation
        if (fullName.isEmpty()) {
            showError("Please enter your full name.");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (password.length() < 3) {
            showError("Password must be at least 3 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        // NEW: T&C Validation - Required before registration
        if (!termsCheckbox.isSelected()) {
            showError("You must agree to the Terms & Privacy Policy.");
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Creating Account...");
        errorLabel.setVisible(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                JsonObject registerData = new JsonObject();
                registerData.addProperty("fullName", fullName);
                registerData.addProperty("email", email);
                registerData.addProperty("password", password);
                registerData.addProperty("username", email.split("@")[0]);
                if (!secretCode.isEmpty()) {
                    registerData.addProperty("secretCode", secretCode);
                }

                // NEW: Determine role from secret code and include in payload
                String role = "EMPLOYEE"; // Default role
                switch (secretCode) {
                    case "KAME_HOUSE":
                        role = "EMPLOYEE";
                        break;
                    case "SAIYAN_GOD":
                        role = "MANAGER";
                        break;
                    case "CAPSULE_CORP":
                        role = "ADMIN";
                        break;
                    case "FRIEZA_FORCE":
                        role = "CLIENT";
                        break;
                }
                registerData.addProperty("role", role);

                return apiClient.post("/auth/register", registerData.toString());
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    JsonObject result = apiClient.getGson().fromJson(response, JsonObject.class);

                    if (result.has("error")) {
                        showError(result.get("error").getAsString());
                    } else if (result.has("message")) {
                        String role = result.has("role") ? result.get("role").getAsString() : "EMPLOYEE";
                        JOptionPane.showMessageDialog(
                                RegisterFrame.this,
                                "Account created successfully!\nYour role: " + role,
                                "Registration Successful",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        new LoginFrame().setVisible(true);
                        dispose();
                    }
                } catch (Exception e) {
                    showError("Registration failed. Please try again.");
                } finally {
                    registerButton.setEnabled(true);
                    registerButton.setText("CREATE ACCOUNT");
                }
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));  // Bold for professional look
        errorLabel.setForeground(AppTheme.DANGER);              // Professional red
        errorLabel.setVisible(true);

        // Shake animation
        final int originalX = getX();
        Timer timer = new Timer(50, null);
        final int[] count = {0};
        timer.addActionListener(e -> {
            count[0]++;
            if (count[0] <= 6) {
                setLocation(originalX + (count[0] % 2 == 0 ? 8 : -8), getY());
            } else {
                setLocation(originalX, getY());
                timer.stop();
            }
        });
        timer.start();
    }

    /**
     * Shows the Terms & Conditions and Privacy Policy dialog.
     */
    private void showTermsAndConditions() {
        JDialog dialog = new JDialog(this, "Terms & Privacy Policy", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.SECONDARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("⚖️ Terms of Service & Privacy Policy");
        titleLabel.setFont(AppTheme.fontHeading(18));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        dialog.add(header, BorderLayout.NORTH);

        // Content
        String termsContent = """
            TECHFORGE ERP - TERMS OF SERVICE
            ================================
            
            1. ACCEPTANCE OF TERMS
            By accessing and using TechForge ERP, you agree to be bound by these Terms of Service.
            
            2. USE LICENSE
            TechForge grants you a non-exclusive, non-transferable license to use this software for 
            enterprise resource planning purposes within your organization.
            
            3. USER RESPONSIBILITIES
            - You must maintain the confidentiality of your account credentials
            - You are responsible for all activities under your account
            - You must not attempt to gain unauthorized access to any part of the system
            
            4. DATA PRIVACY
            - We collect only necessary data for system operation
            - Your data is encrypted and stored securely in Firebase
            - We do not sell or share your data with third parties
            - You can request deletion of your data at any time
            
            5. PAYROLL & FINANCIAL DATA
            - Financial calculations are provided as-is
            - Users should verify all payroll calculations independently
            - TechForge is not liable for financial discrepancies
            
            6. AI FEATURES
            - AI recommendations are suggestions only
            - Final decisions should be made by authorized personnel
            
            7. LIMITATION OF LIABILITY
            TechForge shall not be liable for any indirect, incidental, or consequential damages.
            
            8. MODIFICATIONS
            We reserve the right to modify these terms at any time. Continued use constitutes acceptance.
            
            Last Updated: December 2025
            """;

        JTextArea textArea = new JTextArea(termsContent);
        textArea.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        textArea.setForeground(AppTheme.TEXT_MAIN);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Footer with Accept button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JButton acceptBtn = new JButton("I Understand") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? AppTheme.PRIMARY.brighter() : AppTheme.PRIMARY);
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
        acceptBtn.setFont(AppTheme.fontMain(Font.BOLD, 14));
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setPreferredSize(new Dimension(150, 40));
        acceptBtn.setBorderPainted(false);
        acceptBtn.setContentAreaFilled(false);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        acceptBtn.addActionListener(e -> dialog.dispose());
        footer.add(acceptBtn);

        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}

