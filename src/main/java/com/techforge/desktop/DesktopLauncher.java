package com.techforge.desktop;

import javax.swing.*;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Desktop Application Entry Point.
 * Launches the TechForge ERP Swing application.
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        // Set Look and Feel before creating any Swing components
        try {
            // CRITICAL: Properly activate FlatLaf
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Apply AppTheme design system (colors, fonts, rounded corners)
            AppTheme.setup();

            System.out.println("FlatLaf Light theme activated successfully!");

        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf. Falling back to system L&F.");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Enable anti-aliasing for text
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Launch application on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);

                System.out.println("TechForge ERP Desktop Application started.");
                System.out.println("Backend API: http://localhost:8080/api/v1");
                System.out.println("Make sure the Spring Boot server is running!");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

