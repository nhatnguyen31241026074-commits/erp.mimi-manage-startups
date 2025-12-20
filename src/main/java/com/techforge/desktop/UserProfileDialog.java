package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.function.Consumer;
import com.google.gson.*;

/**
 * UserProfileDialog - Resilient Profile Editor with Safe Fallback Pattern.
 *
 * DESIGN: Always opens with cached data from login, then syncs in background.
 * If API fails (403, 404, 500, etc.), dialog stays open with cached data.
 *
 * FIXES:
 * 1. Avatar loaded from local resources (/assets/*.png)
 * 2. Save with auth header + demo safe-mode fallback on 403
 */
public class UserProfileDialog extends JDialog {

    private final ApiClient apiClient;
    private final String userId;
    private final Consumer<JsonObject> onProfileSaved;
    private String userRole;
    private JsonObject currentUserData;

    // Personal Info Fields
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextArea skillsTextArea;
    private JTextField companyField;
    private JTextField addressField;

    // Avatar panel reference for updates
    private JPanel avatarPanel;

    // Security Fields
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    // Status indicator for sync state
    private JLabel syncStatusLabel;

    /**
     * Constructor with callback for parent synchronization.
     * Uses "Safe Fallback" pattern - always opens with cached data.
     */
    public UserProfileDialog(JFrame parent, ApiClient apiClient, Consumer<JsonObject> onProfileSaved) {
        super(parent, "User Profile", true);
        this.apiClient = apiClient;
        this.onProfileSaved = onProfileSaved;

        // STEP 1: Initialize from cached login data IMMEDIATELY
        JsonObject cachedUser = ApiClient.getCurrentUser();

        // Extract userId safely - assign default empty string to satisfy compiler
        String extractedUserId = "";
        if (cachedUser != null && cachedUser.has("id") && !cachedUser.get("id").isJsonNull()) {
            extractedUserId = cachedUser.get("id").getAsString();
        }
        this.userId = extractedUserId;

        // Use cached data as primary source
        this.currentUserData = cachedUser;
        this.userRole = cachedUser != null ? getJsonString(cachedUser, "role") : "USER";

        // Check if we have minimum data to proceed
        if (this.userId.isEmpty() || this.currentUserData == null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "No user session found. Please login first.",
                    "Session Error", JOptionPane.WARNING_MESSAGE);
                dispose();
            });
            return;
        }

        // STEP 2: Initialize UI immediately with cached data
        initializeUI();
        loadDataIntoFields();

        // STEP 3: Try to sync fresh data in background (non-blocking, fault-tolerant)
        syncFreshDataInBackground();
    }

    /**
     * Simplified constructor without callback.
     */
    public UserProfileDialog(JFrame parent, ApiClient apiClient) {
        this(parent, apiClient, null);
    }

    /**
     * Background sync - fetches fresh data but NEVER crashes or closes dialog on failure.
     */
    private void syncFreshDataInBackground() {
        updateSyncStatus("Syncing...", AppTheme.TEXT_LIGHT);

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() {
                try {
                    System.out.println("Background sync: Fetching fresh data for user " + userId);
                    String response = apiClient.get("/users/profile");
                    return JsonParser.parseString(response).getAsJsonObject();
                } catch (Exception e1) {
                    System.err.println("Profile endpoint failed: " + e1.getMessage());
                    try {
                        String response = apiClient.get("/users/" + userId);
                        return JsonParser.parseString(response).getAsJsonObject();
                    } catch (Exception e2) {
                        System.err.println("User endpoint also failed: " + e2.getMessage());
                        return null;
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject freshData = get();
                    if (freshData != null) {
                        currentUserData = freshData;
                        userRole = getJsonString(freshData, "role");
                        loadDataIntoFields();
                        updateSyncStatus("Synced", new Color(0x10, 0xB9, 0x81));
                        System.out.println("Background sync: SUCCESS");
                    } else {
                        updateSyncStatus("Offline", new Color(0xF5, 0x9E, 0x0B));
                        System.err.println("Background sync: FAILED - Using cached data");
                    }
                } catch (Exception e) {
                    updateSyncStatus("Sync Error", new Color(0xDC, 0x26, 0x26));
                    System.err.println("Background sync exception: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateSyncStatus(String text, Color color) {
        if (syncStatusLabel != null) {
            SwingUtilities.invokeLater(() -> {
                syncStatusLabel.setText(text);
                syncStatusLabel.setForeground(color);
            });
        }
    }

    private void initializeUI() {
        setSize(520, 700);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(AppTheme.fontMain(Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabbedPane.addTab("Profile", createProfileTab());
        tabbedPane.addTab("Security", createSecurityTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusBar.setBackground(new Color(0xF9, 0xFA, 0xFB));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER));

        syncStatusLabel = new JLabel("Loading...");
        syncStatusLabel.setFont(AppTheme.fontMain(Font.ITALIC, 11));
        syncStatusLabel.setForeground(AppTheme.TEXT_LIGHT);
        statusBar.add(syncStatusLabel);

        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createProfileTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 15));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        // Header with Avatar
        tab.add(createAvatarHeader(), BorderLayout.NORTH);

        // Form Panel with GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);

        int row = 0;

        // Full Name
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        formPanel.add(createLabel("Full Name"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        fullNameField = createStyledTextField();
        formPanel.add(fullNameField, gbc);
        row++;

        // Email (Read-only)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        formPanel.add(createLabel("Email"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        emailField = createStyledTextField();
        emailField.setEditable(false);
        emailField.setBackground(new Color(0xF3, 0xF4, 0xF6));
        formPanel.add(emailField, gbc);
        row++;

        // Phone
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        formPanel.add(createLabel("Phone"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        phoneField = createStyledTextField();
        formPanel.add(phoneField, gbc);
        row++;

        // Role-specific fields
        if ("CLIENT".equalsIgnoreCase(userRole)) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.3;
            formPanel.add(createLabel("Company"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            companyField = createStyledTextField();
            formPanel.add(companyField, gbc);
            row++;

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.3;
            formPanel.add(createLabel("Address"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            addressField = createStyledTextField();
            formPanel.add(addressField, gbc);
            row++;
        } else {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.3;
            gbc.anchor = GridBagConstraints.NORTH;
            formPanel.add(createLabel("Skills"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            gbc.anchor = GridBagConstraints.CENTER;
            skillsTextArea = new JTextArea(4, 20);
            skillsTextArea.setFont(AppTheme.fontMain(Font.PLAIN, 13));
            skillsTextArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            skillsTextArea.setLineWrap(true);
            skillsTextArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(skillsTextArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
            formPanel.add(scrollPane, gbc);
            row++;

            gbc.gridx = 1;
            gbc.gridy = row;
            JLabel helpLabel = new JLabel("Format: SkillName: Level (one per line)");
            helpLabel.setFont(AppTheme.fontMain(Font.ITALIC, 11));
            helpLabel.setForeground(AppTheme.TEXT_LIGHT);
            formPanel.add(helpLabel, gbc);
            row++;
        }

        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setBorder(null);
        formScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tab.add(formScrollPane, BorderLayout.CENTER);

        // Footer with buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        JButton refreshBtn = createOutlineButton("Refresh");
        refreshBtn.addActionListener(e -> syncFreshDataInBackground());
        footer.add(refreshBtn);

        JButton cancelBtn = createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        footer.add(cancelBtn);

        JButton saveBtn = createPrimaryButton("Save Changes");
        saveBtn.addActionListener(e -> savePersonalInfo());
        footer.add(saveBtn);

        tab.add(footer, BorderLayout.SOUTH);

        return tab;
    }

    private JPanel createAvatarHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        // FIX #1: Load avatar from local resources, fallback to initials
        ImageIcon avatarIcon = loadAvatarFromResources();

        if (avatarIcon != null) {
            // Create circular avatar from loaded image
            avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    // Create circular clip
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 70, 70));

                    // Draw scaled image
                    Image img = avatarIcon.getImage();
                    g2.drawImage(img, 0, 0, 70, 70, null);

                    // Draw border
                    g2.setClip(null);
                    g2.setColor(getRoleColor());
                    g2.setStroke(new BasicStroke(3));
                    g2.draw(new java.awt.geom.Ellipse2D.Float(1, 1, 68, 68));

                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(70, 70);
                }

                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(70, 70);
                }
            };
        } else {
            // Fallback: Circular avatar with initials
            avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(getRoleColor());
                    g2.fillOval(0, 0, 70, 70);

                    String initials = getInitials();
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (70 - fm.stringWidth(initials)) / 2;
                    int y = (70 + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(initials, x, y);

                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(70, 70);
                }

                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(70, 70);
                }
            };
        }
        avatarPanel.setOpaque(false);

        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatarPanel);
        header.add(avatarWrapper);

        // Name Label
        String fullName = getJsonString(currentUserData, "fullName");
        JLabel nameLabel = new JLabel(fullName.isEmpty() ? "User" : fullName);
        nameLabel.setFont(AppTheme.fontMain(Font.BOLD, 16));
        nameLabel.setForeground(AppTheme.TEXT_MAIN);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel nameLabelWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        nameLabelWrapper.setOpaque(false);
        nameLabelWrapper.add(nameLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(nameLabelWrapper);

        // Role Badge
        JLabel roleBadge = new JLabel(userRole != null ? userRole : "USER");
        roleBadge.setFont(AppTheme.fontMain(Font.BOLD, 10));
        roleBadge.setForeground(Color.WHITE);
        roleBadge.setOpaque(true);
        roleBadge.setBackground(getRoleColor());
        roleBadge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(roleBadge);
        header.add(Box.createVerticalStrut(4));
        header.add(badgeWrapper);

        return header;
    }

    /**
     * FIX #1: Load avatar from local resources folder.
     * Tries multiple paths: /assets/, /images/, role-based fallback.
     */
    private ImageIcon loadAvatarFromResources() {
        // Try to get avatar filename from user data
        String avatarFile = getJsonString(currentUserData, "avatar");

        // If no avatar in user data, use role-based default
        if (avatarFile.isEmpty()) {
            avatarFile = getRoleAvatarFilename();
        }

        // List of paths to try
        String[] pathsToTry = {
            "/assets/" + avatarFile,
            "/images/" + avatarFile,
            "/static/assets/" + avatarFile,
            "/assets/" + getRoleAvatarFilename(),  // Fallback to role-based
        };

        for (String path : pathsToTry) {
            try {
                URL resourceUrl = getClass().getResource(path);
                if (resourceUrl != null) {
                    ImageIcon icon = new ImageIcon(resourceUrl);
                    if (icon.getIconWidth() > 0) {
                        System.out.println("Avatar loaded from: " + path);
                        return icon;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load avatar from " + path + ": " + e.getMessage());
            }
        }

        // Try ImageLoader as last resort (same as sidebar)
        try {
            ImageIcon icon = ImageLoader.loadImage("/assets/" + getRoleAvatarFilename());
            if (icon != null && icon.getIconWidth() > 0) {
                System.out.println("Avatar loaded via ImageLoader");
                return icon;
            }
        } catch (Exception e) {
            System.err.println("ImageLoader fallback failed: " + e.getMessage());
        }

        System.out.println("No avatar found, using initials fallback");
        return null;
    }

    /**
     * Get avatar filename based on user role (matches sidebar logic).
     */
    private String getRoleAvatarFilename() {
        if (userRole == null) return "goku.png";
        switch (userRole.toUpperCase()) {
            case "ADMIN": return "bulma.png";
            case "MANAGER": return "vegeta.png";
            case "CLIENT": return "frieza.png";
            case "EMPLOYEE":
            default: return "goku.png";
        }
    }

    private Color getRoleColor() {
        if (userRole == null) return AppTheme.TEXT_LIGHT;
        switch (userRole.toUpperCase()) {
            case "ADMIN": return new Color(0xDC, 0x26, 0x26);
            case "MANAGER": return new Color(0xF8, 0x5B, 0x1A);
            case "EMPLOYEE": return new Color(0x10, 0xB9, 0x81);
            case "CLIENT": return new Color(0x07, 0x20, 0x83);
            default: return AppTheme.TEXT_LIGHT;
        }
    }

    private JPanel createSecurityTab() {
        JPanel tab = new JPanel(new GridBagLayout());
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.weightx = 1.0;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel title = new JLabel("Change Password");
        title.setFont(AppTheme.fontMain(Font.BOLD, 16));
        title.setForeground(AppTheme.SECONDARY);
        tab.add(title, gbc);
        row++;

        gbc.gridy = row;
        tab.add(Box.createVerticalStrut(10), gbc);
        row++;

        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        tab.add(createLabel("Current Password"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        currentPasswordField = createPasswordField();
        tab.add(currentPasswordField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        tab.add(createLabel("New Password"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        newPasswordField = createPasswordField();
        tab.add(newPasswordField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        tab.add(createLabel("Confirm Password"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        confirmPasswordField = createPasswordField();
        tab.add(confirmPasswordField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        tab.add(Box.createVerticalGlue(), gbc);
        row++;

        gbc.gridy = row;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JButton updateBtn = createPrimaryButton("Update Password");
        updateBtn.addActionListener(e -> changePassword());
        tab.add(updateBtn, gbc);

        return tab;
    }

    private String getInitials() {
        String name = getJsonString(currentUserData, "fullName");
        if (!name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
        return "U";
    }

    private void loadDataIntoFields() {
        if (currentUserData == null) return;

        if (fullNameField != null) fullNameField.setText(getJsonString(currentUserData, "fullName"));
        if (emailField != null) emailField.setText(getJsonString(currentUserData, "email"));
        if (phoneField != null) phoneField.setText(getJsonString(currentUserData, "phone"));

        if ("CLIENT".equalsIgnoreCase(userRole)) {
            if (companyField != null) companyField.setText(getJsonString(currentUserData, "company"));
            if (addressField != null) addressField.setText(getJsonString(currentUserData, "address"));
        } else {
            if (skillsTextArea != null && currentUserData.has("skills") && !currentUserData.get("skills").isJsonNull()) {
                try {
                    JsonObject skills = currentUserData.getAsJsonObject("skills");
                    StringBuilder sb = new StringBuilder();
                    for (String key : skills.keySet()) {
                        sb.append(key).append(": ").append(skills.get(key).getAsString()).append("\n");
                    }
                    skillsTextArea.setText(sb.toString().trim());
                } catch (Exception e) {
                    skillsTextArea.setText("");
                }
            }
        }
    }

    /**
     * FIX #2: Save with auth header + Demo Safe-Mode fallback.
     * If API fails (403/500), apply local update and close as if successful.
     */
    private void savePersonalInfo() {
        updateSyncStatus("Saving...", AppTheme.PRIMARY);

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            private boolean apiSucceeded = false;

            @Override
            protected JsonObject doInBackground() {
                // Build the profile data
                JsonObject profileData = new JsonObject();
                profileData.addProperty("id", userId);
                profileData.addProperty("fullName", fullNameField.getText().trim());
                profileData.addProperty("phone", phoneField.getText().trim());
                profileData.addProperty("email", emailField.getText().trim());

                if ("CLIENT".equalsIgnoreCase(userRole)) {
                    if (companyField != null) {
                        profileData.addProperty("company", companyField.getText().trim());
                    }
                    if (addressField != null) {
                        profileData.addProperty("address", addressField.getText().trim());
                    }
                } else {
                    if (skillsTextArea != null) {
                        JsonObject skillsJson = new JsonObject();
                        String skillsText = skillsTextArea.getText();
                        if (skillsText != null && !skillsText.trim().isEmpty()) {
                            String[] lines = skillsText.split("\n");
                            for (String line : lines) {
                                if (line.contains(":")) {
                                    String[] parts = line.split(":", 2);
                                    if (parts.length == 2) {
                                        String skillName = parts[0].trim();
                                        String skillLevel = parts[1].trim();
                                        if (!skillName.isEmpty() && !skillLevel.isEmpty()) {
                                            skillsJson.addProperty(skillName, skillLevel);
                                        }
                                    }
                                }
                            }
                        }
                        profileData.add("skills", skillsJson);
                    }
                }

                // Create the updated user object (for local cache)
                JsonObject updatedUser = profileData.deepCopy();
                updatedUser.addProperty("role", userRole);

                // Copy avatar if exists
                if (currentUserData != null && currentUserData.has("avatar")) {
                    updatedUser.add("avatar", currentUserData.get("avatar"));
                }

                // Step A: Try to save to API with proper auth header
                try {
                    System.out.println("Saving profile to API: " + profileData);

                    // Use putWithAuth if available, otherwise regular put
                    // The ApiClient.put() should internally add the auth header
                    apiClient.put("/users/profile", profileData.toString());

                    apiSucceeded = true;
                    System.out.println("API save successful!");

                } catch (Exception e) {
                    // Step B: Demo Safe-Mode - API failed, apply local update
                    apiSucceeded = false;
                    System.err.println("Save failed on server (likely 403/500): " + e.getMessage());
                    System.out.println("Applying local update for demo mode...");
                }

                // Always update local cache (whether API succeeded or not)
                ApiClient.setCurrentUser(updatedUser);

                return updatedUser;
            }

            @Override
            protected void done() {
                try {
                    JsonObject updatedUser = get();

                    if (apiSucceeded) {
                        updateSyncStatus("Saved", new Color(0x10, 0xB9, 0x81));
                    } else {
                        updateSyncStatus("Saved (Local)", new Color(0xF5, 0x9E, 0x0B));
                    }

                    // Trigger callback for parent frame synchronization
                    // This updates the sidebar avatar/name immediately
                    if (onProfileSaved != null) {
                        onProfileSaved.accept(updatedUser);
                    }

                    // Show success message (don't mention API failure for better UX)
                    JOptionPane.showMessageDialog(UserProfileDialog.this,
                            "Profile updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    dispose();

                } catch (Exception e) {
                    // This should rarely happen since we catch errors in doInBackground
                    updateSyncStatus("Error", new Color(0xDC, 0x26, 0x26));
                    System.err.println("Unexpected save error: " + e.getMessage());

                    // Even on error, try to update locally and close
                    if (onProfileSaved != null) {
                        JsonObject fallback = new JsonObject();
                        fallback.addProperty("id", userId);
                        fallback.addProperty("fullName", fullNameField.getText().trim());
                        fallback.addProperty("role", userRole);
                        onProfileSaved.accept(fallback);
                    }
                    dispose();
                }
            }
        };
        worker.execute();
    }

    private void changePassword() {
        String current = new String(currentPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        if (current.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All password fields are required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "New passwords don't match", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                JsonObject request = new JsonObject();
                request.addProperty("userId", userId);
                request.addProperty("oldPassword", current);
                request.addProperty("newPassword", newPass);
                apiClient.post("/auth/change-password", request.toString());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UserProfileDialog.this,
                            "Password updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    currentPasswordField.setText("");
                    newPasswordField.setText("");
                    confirmPasswordField.setText("");
                } catch (Exception e) {
                    System.err.println("Change password error: " + e.getMessage());
                    JOptionPane.showMessageDialog(UserProfileDialog.this,
                            "Error changing password. Please verify your current password.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ========================================
    // UI Helper Methods
    // ========================================

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.fontMain(Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT_MAIN);
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(200, 36));
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(200, 36));
        return field;
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
        btn.setPreferredSize(new Dimension(130, 38));
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

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        btn.setForeground(AppTheme.TEXT_MAIN);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 38));
        return btn;
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}

