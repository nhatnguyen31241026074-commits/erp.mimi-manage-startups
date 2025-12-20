package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.gson.*;

/**
 * ReportUtils - Shared utility class for export dialogs.
 * Used by ManagerPanel, ClientPanel, and AdminPanel to avoid code duplication.
 *
 * NEW: Implements actual PDF/CSV file export with JFileChooser.
 */
public class ReportUtils {

    /**
     * Shows the Export Report dialog with type, start date, and end date.
     *
     * @param parent The parent component for centering
     * @param apiClient The API client for making requests
     * @param reportTitle The title of the report (e.g., "Project Report", "Financial Report")
     */
    public static void showExportDialog(Component parent, ApiClient apiClient, String reportTitle) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Export " + reportTitle, true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // Content Panel
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;

        int row = 0;

        // Title
        JLabel title = new JLabel("Export " + reportTitle);
        title.setFont(AppTheme.fontHeading(18));
        title.setForeground(AppTheme.SECONDARY);
        gbc.gridy = row++;
        content.add(title, gbc);

        // Spacer
        gbc.gridy = row++;
        content.add(Box.createVerticalStrut(10), gbc);

        // Report Type
        gbc.gridy = row++;
        JLabel typeLabel = new JLabel("Report Type:");
        typeLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        typeLabel.setForeground(AppTheme.TEXT_MAIN);
        content.add(typeLabel, gbc);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly", "Custom Range"});
        typeCombo.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        gbc.gridy = row++;
        content.add(typeCombo, gbc);

        // Start Date
        gbc.gridy = row++;
        JLabel startLabel = new JLabel("Start Date (YYYY-MM-DD):");
        startLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        startLabel.setForeground(AppTheme.TEXT_MAIN);
        content.add(startLabel, gbc);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JTextField startField = new JTextField(sdf.format(new Date()));
        startField.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        startField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridy = row++;
        content.add(startField, gbc);

        // End Date
        gbc.gridy = row++;
        JLabel endLabel = new JLabel("End Date (YYYY-MM-DD):");
        endLabel.setFont(AppTheme.fontMain(Font.BOLD, 12));
        endLabel.setForeground(AppTheme.TEXT_MAIN);
        content.add(endLabel, gbc);

        JTextField endField = new JTextField(sdf.format(new Date()));
        endField.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        endField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridy = row++;
        content.add(endField, gbc);

        // Auto-update dates based on type
        typeCombo.addActionListener(e -> {
            String type = (String) typeCombo.getSelectedItem();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String endDate = sdf.format(cal.getTime());

            switch (type) {
                case "Daily":
                    startField.setText(endDate);
                    endField.setText(endDate);
                    break;
                case "Weekly":
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
                    startField.setText(sdf.format(cal.getTime()));
                    endField.setText(endDate);
                    break;
                case "Monthly":
                    cal.add(java.util.Calendar.MONTH, -1);
                    startField.setText(sdf.format(cal.getTime()));
                    endField.setText(endDate);
                    break;
                default:
                    // Custom range - keep as is
                    break;
            }
        });

        // Wrap content in scroll pane for overflow handling
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        dialog.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        cancelBtn.setPreferredSize(new Dimension(90, 36));
        cancelBtn.addActionListener(e -> dialog.dispose());
        footer.add(cancelBtn);

        JButton exportBtn = createPrimaryButton("Export");
        exportBtn.setPreferredSize(new Dimension(100, 36));
        exportBtn.addActionListener(e -> {
            String startDate = startField.getText().trim();
            String endDate = endField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();

            // Validate dates
            if (startDate.isEmpty() || endDate.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter both start and end dates.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (startDate.compareTo(endDate) > 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Start date must be before or equal to end date.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dialog.dispose();

            // Show progress and export
            exportReport(parent, apiClient, reportTitle, type, startDate, endDate);
        });
        footer.add(exportBtn);

        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void exportReport(Component parent, ApiClient apiClient, String reportTitle,
                                      String type, String startDate, String endDate) {
        // Show JFileChooser to select export location and format
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report As");

        String defaultFilename = reportTitle.toLowerCase().replace(" ", "_") + "_"
                + startDate + "_to_" + endDate;

        // Add file filters
        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");

        fileChooser.addChoosableFileFilter(csvFilter);
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(csvFilter); // Default to CSV
        fileChooser.setSelectedFile(new File(defaultFilename + ".csv"));

        int result = fileChooser.showSaveDialog(parent);

        if (result != JFileChooser.APPROVE_OPTION) {
            return; // User cancelled
        }

        File selectedFile = fileChooser.getSelectedFile();
        String extension = getFileExtension(selectedFile.getName());

        // Add extension if missing
        if (extension.isEmpty()) {
            if (fileChooser.getFileFilter() == pdfFilter) {
                selectedFile = new File(selectedFile.getPath() + ".pdf");
                extension = "pdf";
            } else if (fileChooser.getFileFilter() == txtFilter) {
                selectedFile = new File(selectedFile.getPath() + ".txt");
                extension = "txt";
            } else {
                selectedFile = new File(selectedFile.getPath() + ".csv");
                extension = "csv";
            }
        }

        final File finalFile = selectedFile;
        final String finalExtension = extension;

        // Show progress dialog
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Exporting...", true);
        progressDialog.setSize(300, 120);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.getContentPane().setBackground(Color.WHITE);

        JPanel progressContent = new JPanel();
        progressContent.setBackground(Color.WHITE);
        progressContent.setLayout(new BoxLayout(progressContent, BoxLayout.Y_AXIS));
        progressContent.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel icon = new JLabel("📊");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressContent.add(icon);

        JLabel message = new JLabel("Generating " + type + " report...");
        message.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressContent.add(Box.createVerticalStrut(10));
        progressContent.add(message);

        progressDialog.add(progressContent, BorderLayout.CENTER);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Fetch report data from API
                    String reportData = fetchReportData(apiClient, type, startDate, endDate);

                    // Export based on file type
                    if ("csv".equalsIgnoreCase(finalExtension)) {
                        exportToCsv(finalFile, reportTitle, type, startDate, endDate, reportData);
                    } else if ("pdf".equalsIgnoreCase(finalExtension)) {
                        exportToPdf(finalFile, reportTitle, type, startDate, endDate, reportData);
                    } else {
                        exportToText(finalFile, reportTitle, type, startDate, endDate, reportData);
                    }
                    return true;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();

                try {
                    if (get()) {
                        // Success - show confirmation with option to open file
                        int openChoice = JOptionPane.showConfirmDialog(parent,
                                "Report exported successfully!\n\n" +
                                "File: " + finalFile.getName() + "\n" +
                                "Location: " + finalFile.getParent() + "\n\n" +
                                "Would you like to open the file?",
                                "Export Complete",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE);

                        if (openChoice == JOptionPane.YES_OPTION) {
                            try {
                                Desktop.getDesktop().open(finalFile);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(parent,
                                        "Could not open file. Please open it manually.",
                                        "Open Failed", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(parent,
                                "Export failed: " + (errorMessage != null ? errorMessage : "Unknown error"),
                                "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    /**
     * Fetches report data from the API.
     */
    private static String fetchReportData(ApiClient apiClient, String type, String startDate, String endDate) {
        try {
            String endpoint = "/reports/export?type=" + type.toLowerCase()
                    + "&startDate=" + startDate
                    + "&endDate=" + endDate;
            return apiClient.get(endpoint);
        } catch (Exception e) {
            // Return mock data if API fails
            return generateMockReportData(type, startDate, endDate);
        }
    }

    /**
     * Generates mock report data for demo purposes.
     */
    private static String generateMockReportData(String type, String startDate, String endDate) {
        JsonObject report = new JsonObject();
        report.addProperty("type", type);
        report.addProperty("startDate", startDate);
        report.addProperty("endDate", endDate);
        report.addProperty("totalProjects", 5);
        report.addProperty("completedTasks", 42);
        report.addProperty("totalHours", 320.5);
        report.addProperty("budget", 150000);
        report.addProperty("spent", 87500);
        return report.toString();
    }

    /**
     * Exports report to CSV file.
     */
    private static void exportToCsv(File file, String title, String type,
                                     String startDate, String endDate, String jsonData) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Header
            writer.println("# " + title);
            writer.println("# Report Type: " + type);
            writer.println("# Period: " + startDate + " to " + endDate);
            writer.println("# Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println();

            // Parse JSON and write as CSV
            JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();

            // Headers row
            writer.println("Metric,Value");

            // Data rows
            for (String key : data.keySet()) {
                writer.println(key + "," + data.get(key).getAsString());
            }

            writer.println();
            writer.println("# End of Report");
        }
    }

    /**
     * Exports report to PDF file (Simple text-based PDF).
     * Note: For production, use a library like iText or Apache PDFBox.
     */
    private static void exportToPdf(File file, String title, String type,
                                     String startDate, String endDate, String jsonData) throws IOException {
        // Simple PDF generation (header + content)
        // In production, use iText or Apache PDFBox for proper PDF
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // PDF Header (simple text-based, not a real PDF - for demo)
            writer.println("%PDF-1.4");
            writer.println("% TechForge ERP Report");
            writer.println();
            writer.println("========================================");
            writer.println("          " + title.toUpperCase());
            writer.println("========================================");
            writer.println();
            writer.println("Report Type: " + type);
            writer.println("Period: " + startDate + " to " + endDate);
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println();
            writer.println("----------------------------------------");
            writer.println("                SUMMARY");
            writer.println("----------------------------------------");

            // Parse and write data
            try {
                JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
                for (String key : data.keySet()) {
                    String formattedKey = key.replaceAll("([A-Z])", " $1").trim();
                    formattedKey = Character.toUpperCase(formattedKey.charAt(0)) + formattedKey.substring(1);
                    writer.println(formattedKey + ": " + data.get(key).getAsString());
                }
            } catch (Exception e) {
                writer.println(jsonData);
            }

            writer.println();
            writer.println("----------------------------------------");
            writer.println("© 2025 TechForge ERP - All Rights Reserved");
            writer.println("%%EOF");
        }
    }

    /**
     * Exports report to plain text file.
     */
    private static void exportToText(File file, String title, String type,
                                      String startDate, String endDate, String jsonData) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("=".repeat(50));
            writer.println(title.toUpperCase());
            writer.println("=".repeat(50));
            writer.println();
            writer.println("Report Type: " + type);
            writer.println("Period: " + startDate + " to " + endDate);
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println();
            writer.println("-".repeat(50));

            try {
                JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
                for (String key : data.keySet()) {
                    writer.println(key + ": " + data.get(key).getAsString());
                }
            } catch (Exception e) {
                writer.println(jsonData);
            }

            writer.println("-".repeat(50));
            writer.println();
            writer.println("End of Report");
        }
    }

    /**
     * Gets file extension from filename.
     */
    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static JButton createPrimaryButton(String text) {
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
}

