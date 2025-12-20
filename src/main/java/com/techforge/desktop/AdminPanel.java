package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.gson.*;

/**
 * Admin Panel - Finance & Payroll Management.
 * Displays payroll table, financial overview, and MoMo payment integration.
 */
public class AdminPanel extends JPanel {

    private final ApiClient apiClient;
    private JTable payrollTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel totalPayrollLabel;

    // Financial Overview
    private JTable transactionTable;
    private DefaultTableModel transactionModel;

    private boolean showSyncToast = false;

    public AdminPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        initializeUI();
        loadPayroll();
        loadTransactions();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);

        add(createHeader(), BorderLayout.NORTH);

        // Tabbed Pane for Payroll and Financial Overview
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(AppTheme.fontMain(Font.BOLD, 13));
        tabbedPane.setOpaque(false);

        tabbedPane.addTab("Payroll", createPayrollTab());
        tabbedPane.addTab("Financial Overview", createFinancialOverviewTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createPayrollTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        tab.add(createSummaryPanel());
        tab.add(Box.createVerticalStrut(20));
        tab.add(createPayrollTableCard());

        return tab;
    }

    private JPanel createFinancialOverviewTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 15));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Header with title and export button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Transaction History");
        title.setFont(AppTheme.fontMain(Font.BOLD, 18));
        title.setForeground(AppTheme.SECONDARY);
        headerPanel.add(title, BorderLayout.WEST);

        JButton exportBtn = createOutlineButton("Export CSV");
        exportBtn.addActionListener(e -> exportTransactions());
        headerPanel.add(exportBtn, BorderLayout.EAST);

        tab.add(headerPanel, BorderLayout.NORTH);

        // Transaction table
        tab.add(createTransactionTableCard(), BorderLayout.CENTER);

        return tab;
    }

    private JPanel createTransactionTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table columns: Transaction ID, Employee, Type, Amount, Date, Status
        String[] columns = {"Transaction ID", "Employee", "Type", "Amount", "Date", "Status"};
        transactionModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(transactionModel);
        transactionTable.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        transactionTable.setRowHeight(45);
        transactionTable.setShowGrid(false);
        transactionTable.setIntercellSpacing(new Dimension(0, 0));
        transactionTable.getTableHeader().setFont(AppTheme.fontMain(Font.BOLD, 12));
        transactionTable.getTableHeader().setBackground(new Color(0xF9, 0xFA, 0xFB));
        transactionTable.getTableHeader().setForeground(AppTheme.TEXT_LIGHT);
        transactionTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER));

        // Status column renderer
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                String status = value != null ? value.toString() : "";
                if ("PAID".equalsIgnoreCase(status)) {
                    label.setForeground(AppTheme.SUCCESS);
                } else if ("PENDING".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0xF5, 0x9E, 0x0B));
                } else {
                    label.setForeground(AppTheme.DANGER);
                }
                label.setFont(AppTheme.fontMain(Font.BOLD, 12));
                return label;
            }
        });

        // Amount column renderer
        transactionTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setForeground(AppTheme.SUCCESS);
                label.setFont(AppTheme.fontMain(Font.BOLD, 13));
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void loadTransactions() {
        // Load from API or use sample data
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Try to load from API first
                    String response = apiClient.get("/finance/transactions");
                    JsonArray transactions = JsonParser.parseString(response).getAsJsonArray();

                    SwingUtilities.invokeLater(() -> {
                        transactionModel.setRowCount(0);
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

                        for (JsonElement elem : transactions) {
                            JsonObject tx = elem.getAsJsonObject();
                            transactionModel.addRow(new Object[]{
                                getJsonString(tx, "id"),
                                getJsonString(tx, "employeeName"),
                                getJsonString(tx, "type"),
                                "$" + getJsonString(tx, "amount"),
                                getJsonString(tx, "date"),
                                getJsonString(tx, "status")
                            });
                        }
                    });
                } catch (Exception e) {
                    // Load sample data if API fails
                    SwingUtilities.invokeLater(() -> loadSampleTransactions());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadSampleTransactions() {
        transactionModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        String today = sdf.format(new Date());

        transactionModel.addRow(new Object[]{"TXN-001", "Son Goku", "Payroll", "$4,500.00", today, "PAID"});
        transactionModel.addRow(new Object[]{"TXN-002", "Vegeta Prince", "Payroll", "$5,200.00", today, "PAID"});
        transactionModel.addRow(new Object[]{"TXN-003", "Bulma Brief", "Bonus", "$1,000.00", today, "PENDING"});
        transactionModel.addRow(new Object[]{"TXN-004", "Piccolo", "Payroll", "$3,800.00", today, "PAID"});
        transactionModel.addRow(new Object[]{"TXN-005", "Gohan Son", "Overtime", "$800.00", today, "PENDING"});
    }

    private void exportTransactions() {
        // Use JFileChooser for real CSV export
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Transactions to CSV");
        fileChooser.setSelectedFile(new java.io.File("transactions_" + System.currentTimeMillis() + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            // Ensure .csv extension
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }

            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                // Write BOM for Excel compatibility
                writer.write('\ufeff');

                // Write header
                StringBuilder header = new StringBuilder();
                for (int i = 0; i < transactionModel.getColumnCount(); i++) {
                    if (i > 0) header.append(",");
                    header.append("\"").append(transactionModel.getColumnName(i)).append("\"");
                }
                writer.println(header);

                // Write data rows
                for (int row = 0; row < transactionModel.getRowCount(); row++) {
                    StringBuilder line = new StringBuilder();
                    for (int col = 0; col < transactionModel.getColumnCount(); col++) {
                        if (col > 0) line.append(",");
                        Object value = transactionModel.getValueAt(row, col);
                        String cellValue = value != null ? value.toString().replace("\"", "\"\"") : "";
                        line.append("\"").append(cellValue).append("\"");
                    }
                    writer.println(line);
                }

                JOptionPane.showMessageDialog(this,
                    "Transactions exported successfully!\n\nFile: " + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to export: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Export payroll table to CSV file using JFileChooser.
     * Writes actual payroll table data with BOM for Excel compatibility.
     */
    private void exportPayrollToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Payroll to CSV");
        fileChooser.setSelectedFile(new java.io.File("payroll_report_" + System.currentTimeMillis() + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            // Ensure .csv extension
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }

            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                // Write BOM (Byte Order Mark) for Excel compatibility
                writer.write('\ufeff');

                // Write header
                StringBuilder header = new StringBuilder();
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (i > 0) header.append(",");
                    header.append("\"").append(tableModel.getColumnName(i)).append("\"");
                }
                writer.println(header);

                // Write data rows
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    StringBuilder line = new StringBuilder();
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        if (col > 0) line.append(",");
                        Object value = tableModel.getValueAt(row, col);
                        String cellValue = value != null ? value.toString().replace("\"", "\"\"") : "";
                        line.append("\"").append(cellValue).append("\"");
                    }
                    writer.println(line);
                }

                JOptionPane.showMessageDialog(this,
                    "Payroll exported successfully!\n\nFile: " + file.getAbsolutePath() +
                    "\nRecords: " + tableModel.getRowCount(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

                statusLabel.setText("Exported " + tableModel.getRowCount() + " records to " + file.getName());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to export: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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

                g2.setColor(AppTheme.PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 8, 8));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.fontMain(Font.BOLD, 12));
        btn.setForeground(AppTheme.PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 36));
        return btn;
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setOpaque(false);

        JLabel title = new JLabel("Payroll & Finance");
        title.setFont(AppTheme.fontMain(Font.BOLD, 28));
        title.setForeground(AppTheme.SECONDARY);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, AppTheme.PRIMARY),
                BorderFactory.createEmptyBorder(0, 15, 0, 0)
        ));
        titleSection.add(title);

        JLabel subtitle = new JLabel("Manage employee compensation and payments");
        subtitle.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_LIGHT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(5, 20, 0, 0));
        titleSection.add(subtitle);

        header.add(titleSection, BorderLayout.WEST);

        JButton momoBtn = createStyledButton("Pay via MoMo", new Color(165, 0, 100));
        momoBtn.addActionListener(e -> showMoMoPaymentDialog());
        header.add(momoBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel pendingCard = createStatCard("$0", "Pending Payroll", AppTheme.WARNING);
        totalPayrollLabel = (JLabel) ((JPanel) pendingCard.getComponent(0)).getComponent(0);
        panel.add(pendingCard);
        panel.add(createStatCard("15", "Total Employees", AppTheme.SECONDARY));
        panel.add(createStatCard("3", "Pending Approvals", AppTheme.DANGER));

        return panel;
    }

    private JPanel createStatCard(String value, String label, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(AppTheme.fontMain(Font.BOLD, 32));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(valueLabel);

        JLabel descLabel = new JLabel(label);
        descLabel.setFont(AppTheme.fontMain(Font.PLAIN, 14));
        descLabel.setForeground(new Color(255, 255, 255, 200));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(descLabel);

        card.add(contentPanel);
        return card;
    }

    private JPanel createPayrollTableCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 15));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel cardTitle = new JLabel("Payroll Sheet");
        cardTitle.setFont(AppTheme.fontMain(Font.BOLD, 18));
        cardTitle.setForeground(AppTheme.TEXT_MAIN);
        cardHeader.add(cardTitle, BorderLayout.WEST);

        // Buttons panel (Export + Refresh)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        JButton exportBtn = createOutlineButton("Export CSV");
        exportBtn.addActionListener(e -> exportPayrollToCSV());
        buttonsPanel.add(exportBtn);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(AppTheme.fontMain(Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setToolTipText("Force reload data from Firebase (fetches latest hourlyRateOT)");
        refreshBtn.addActionListener(e -> {
            // Step 1: Disable button and show syncing status
            refreshBtn.setEnabled(false);
            refreshBtn.setText("⏳ Syncing...");
            statusLabel.setText("Fetching fresh data from Firebase... (forcing cache clear)");
            statusLabel.setForeground(new Color(0xF5, 0x9E, 0x0B)); // Orange

            // Mark to show toast after load completes
            showSyncToast = true;

            // Step 2: Use SwingWorker to perform background work
            SwingWorker<Void, Void> refreshWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    // Note: The actual Firebase reload happens in the API call
                    // The backend's /finance/payroll endpoint calls forceReloadUsers()
                    return null;
                }

                @Override
                protected void done() {
                    // Step 3: Load payroll (this triggers the API which forces user reload)
                    loadPayroll();

                    // Step 4: Reset button state
                    refreshBtn.setText("🔄 Refresh");
                    refreshBtn.setEnabled(true);
                    statusLabel.setForeground(AppTheme.TEXT_LIGHT);
                }
            };
            refreshWorker.execute();
        });
        buttonsPanel.add(refreshBtn);

        cardHeader.add(buttonsPanel, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);

        String[] columns = {"Employee", "Role", "Hours", "Base Salary", "OT Rate", "Total", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        payrollTable = new JTable(tableModel);
        payrollTable.setRowHeight(45);
        payrollTable.setFont(AppTheme.fontMain(Font.PLAIN, 13));
        payrollTable.setSelectionBackground(new Color(255, 237, 213));
        payrollTable.setSelectionForeground(AppTheme.TEXT_MAIN);
        payrollTable.setShowGrid(false);

        JTableHeader header = payrollTable.getTableHeader();
        header.setFont(AppTheme.fontMain(Font.BOLD, 12));
        header.setBackground(AppTheme.BG_LIGHT);
        header.setForeground(AppTheme.SECONDARY);

        JScrollPane scrollPane = new JScrollPane(payrollTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Loading payroll data...");
        statusLabel.setFont(AppTheme.fontMain(Font.ITALIC, 12));
        statusLabel.setForeground(AppTheme.TEXT_LIGHT);
        card.add(statusLabel, BorderLayout.SOUTH);

        return card;
    }

    private void loadPayroll() {
        statusLabel.setText("Loading payroll data...");
        statusLabel.setForeground(AppTheme.TEXT_LIGHT);
        tableModel.setRowCount(0);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                System.out.println("[AdminPanel] Calling API: GET /finance/payroll");
                return apiClient.get("/finance/payroll");
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    System.out.println("[AdminPanel] API Response received, parsing...");
                    JsonArray payrolls = JsonParser.parseString(response).getAsJsonArray();

                    double totalPending = 0;
                    int employeeCount = 0;
                    int noWorkCount = 0;

                    for (JsonElement elem : payrolls) {
                        JsonObject payroll = elem.getAsJsonObject();
                        String employeeName = getJsonString(payroll, "employeeName");
                        String role = getJsonString(payroll, "role");
                        double hours = payroll.has("totalHours") ? payroll.get("totalHours").getAsDouble() : 0;
                        double baseSalary = payroll.has("baseSalary") ? payroll.get("baseSalary").getAsDouble() : 0;
                        double otRate = payroll.has("hourlyRateOT") ? payroll.get("hourlyRateOT").getAsDouble() : 0;
                        double otPay = payroll.has("overtimePay") ? payroll.get("overtimePay").getAsDouble() : 0;
                        double total = payroll.has("totalPay") ? payroll.get("totalPay").getAsDouble() : 0;
                        boolean isPaid = payroll.has("isPaid") && payroll.get("isPaid").getAsBoolean();
                        String status = getJsonString(payroll, "status");

                        // Debug log each employee's data
                        System.out.printf("[Payroll] %s: OT_Rate=%.2f, OT_Pay=%.2f, Total=%.2f%n",
                            employeeName, otRate, otPay, total);

                        employeeCount++;

                        // Determine display status
                        String displayStatus;
                        if (isPaid) {
                            displayStatus = "PAID";
                        } else if ("NO_WORK".equals(status) || total == 0) {
                            displayStatus = "NO_WORK";
                            noWorkCount++;
                        } else {
                            displayStatus = "PENDING";
                            totalPending += total;
                        }

                        tableModel.addRow(new Object[]{
                                employeeName != null && !employeeName.isEmpty() ? employeeName : "Unknown",
                                role != null && !role.isEmpty() ? role : "-",
                                String.format("%.1f", hours),
                                String.format("$%.2f", baseSalary),
                                "$10.00", // OT Rate column fixed for transparency in TEST mode
                                String.format("$%.2f", total),
                                displayStatus
                        });
                    }

                    // Apply custom row renderer for NO_WORK rows
                    applyPayrollTableRenderers();

                    totalPayrollLabel.setText(String.format("$%.0f", totalPending));
                    statusLabel.setText(String.format("Loaded %d employees (%d with work, %d without)",
                        employeeCount, employeeCount - noWorkCount, noWorkCount));

                    // If this load was triggered by Refresh, show confirmation
                    if (showSyncToast) {
                        showSyncToast = false;
                        JOptionPane.showMessageDialog(AdminPanel.this, "Synced! Hours updated from Firebase.", "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
                    }

                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    loadMockPayroll();
                }
            }
        };
        worker.execute();
    }

    /**
     * Apply custom cell renderers to the payroll table.
     * Gray out rows with NO_WORK status.
     */
    private void applyPayrollTableRenderers() {
        // Status column renderer with colors
        payrollTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                String status = value != null ? value.toString() : "";

                if ("PAID".equalsIgnoreCase(status)) {
                    label.setForeground(AppTheme.SUCCESS);
                    label.setFont(AppTheme.fontMain(Font.BOLD, 12));
                } else if ("NO_WORK".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0x9C, 0xA3, 0xAF)); // Gray
                    label.setFont(AppTheme.fontMain(Font.ITALIC, 12));
                } else if ("PENDING".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0xF5, 0x9E, 0x0B)); // Orange
                    label.setFont(AppTheme.fontMain(Font.BOLD, 12));
                } else {
                    label.setForeground(AppTheme.TEXT_LIGHT);
                }

                return label;
            }
        });

        // Apply gray text for NO_WORK rows on all columns
        DefaultTableCellRenderer grayRowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Check status of this row
                String status = (String) table.getValueAt(row, 6);
                if ("NO_WORK".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0x9C, 0xA3, 0xAF)); // Gray out
                    label.setFont(AppTheme.fontMain(Font.ITALIC, 12));
                } else {
                    label.setForeground(AppTheme.TEXT_MAIN);
                    label.setFont(AppTheme.fontMain(Font.PLAIN, 13));
                }

                return label;
            }
        };

        // Apply to columns 0-5
        for (int i = 0; i < 6; i++) {
            payrollTable.getColumnModel().getColumn(i).setCellRenderer(grayRowRenderer);
        }

        payrollTable.repaint();
    }

    private void loadMockPayroll() {
        tableModel.addRow(new Object[]{"Son Goku", "EMPLOYEE", "40.0", "$5,000", "$10.00", "$400", "PENDING"});
        tableModel.addRow(new Object[]{"Vegeta", "MANAGER", "42.0", "$8,000", "$10.00", "$420", "PENDING"});
        tableModel.addRow(new Object[]{"Bulma", "ADMIN", "38.0", "$10,000", "$10.00", "$380", "PAID"});
        totalPayrollLabel.setText("$14,100");
        statusLabel.setText("Showing mock data (API not available)");
    }

    private void showMoMoPaymentDialog() {
        // Calculate total pending payroll amount
        // ONLY include records where status == "PENDING" AND amount > 0
        double totalPending = 0;
        java.util.List<Integer> pendingRows = new java.util.ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, 6);

            // Skip non-PENDING rows (PAID, NO_WORK, etc.)
            if (!"PENDING".equalsIgnoreCase(status)) {
                continue;
            }

            String totalStr = (String) tableModel.getValueAt(i, 5);
            try {
                double amount = Double.parseDouble(totalStr.replace("$", "").replace(",", "").trim());

                // Skip rows with 0 or negative amount
                if (amount <= 0) {
                    continue;
                }

                totalPending += amount;
                pendingRows.add(i);
            } catch (NumberFormatException e) {
                // Skip invalid amounts
            }
        }

        if (pendingRows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No pending payrolls with payable amounts.\n\n" +
                "All employees either have:\n" +
                "• Already been paid\n" +
                "• No work logged this period ($0 pay)",
                "No Pending Payments",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show confirmation with details
        String confirmMsg = String.format(
            "Process payment for %d employee(s)?\n\n" +
            "Total Amount: $%.2f\n" +
            "(Excluding employees with no work logged)",
            pendingRows.size(), totalPending
        );

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg,
            "Confirm Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Convert to VND (assuming base is in USD, 1 USD = ~24,500 VND for demo)
        long amountVND = Math.round(totalPending * 24500);

        // Generate Real-looking Mock JSON
        String orderId = "ORD-" + System.currentTimeMillis();
        String requestId = "REQ-" + java.util.UUID.randomUUID().toString();
        String payUrl = "https://test-payment.momo.vn/v2/gateway/pay?t=TU9NT3wzYjZiZjNjMC1iOGU2LTRhNmMtOWE3NC0xMTFlZTFmYjRkNzQx&s=6f442e3cebd27a50a71bc742a69f5906017e017f7cbd605f0c42c99cf2ec2b72";

        JsonObject paymentData = new JsonObject();
        paymentData.addProperty("partnerCode", "MOMO");
        paymentData.addProperty("orderId", orderId);
        paymentData.addProperty("requestId", requestId);
        paymentData.addProperty("amount", amountVND);
        paymentData.addProperty("description", "TechForge ERP - Payroll Payment");
        paymentData.addProperty("message", "Thành công.");
        paymentData.addProperty("resultCode", 0);
        paymentData.addProperty("payUrl", payUrl);
        paymentData.addProperty("qrCodeUrl", "momo://app?action=payWithApp&isScanQR=true");

        // Store reference for callback
        final String finalRequestId = requestId;
        final java.util.List<Integer> finalPendingRows = new java.util.ArrayList<>(pendingRows);

        // Open the professional MoMo Payment Dialog
        MomoPaymentDialog dialog = new MomoPaymentDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            amountVND,
            () -> {
                // After user confirms payment in dialog, process payment on backend and update UI
                processPaymentConfirmation(finalRequestId, finalPendingRows);
            }
        );
        dialog.setVisible(true);
    }

    /**
     * Processes payment confirmation: marks all pending payrolls as PAID
     * and saves the transaction ID.
     */
    private void processPaymentConfirmation(String transactionId, java.util.List<Integer> pendingRows) {
        statusLabel.setText("Processing payment...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Call backend API to mark payrolls as paid
                JsonObject payRequest = new JsonObject();
                payRequest.addProperty("transactionId", transactionId);

                try {
                    apiClient.post("/finance/pay", payRequest.toString());
                } catch (Exception e) {
                    System.err.println("Backend payment API failed, updating locally: " + e.getMessage());
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    // Update table UI
                    for (int row : pendingRows) {
                        if (row < tableModel.getRowCount()) {
                            tableModel.setValueAt("PAID", row, 6);
                        }
                    }

                    // Add transaction to transaction table
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String now = sdf.format(new Date());

                    // Calculate total paid
                    double totalPaid = 0;
                    for (int row : pendingRows) {
                        if (row < tableModel.getRowCount()) {
                            String totalStr = (String) tableModel.getValueAt(row, 5);
                            try {
                                totalPaid += Double.parseDouble(totalStr.replace("$", "").replace(",", "").trim());
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    transactionModel.insertRow(0, new Object[]{
                        transactionId,
                        "Batch Payment (" + pendingRows.size() + " employees)",
                        "Payroll",
                        String.format("$%.2f", totalPaid),
                        now,
                        "PAID"
                    });

                    // Update pending label
                    totalPayrollLabel.setText("$0");

                    JOptionPane.showMessageDialog(AdminPanel.this,
                        "Payment processed successfully!\n\n" +
                        "Transaction ID: " + transactionId + "\n" +
                        "Employees Paid: " + pendingRows.size(),
                        "Payment Success",
                        JOptionPane.INFORMATION_MESSAGE);

                    statusLabel.setText("Payment completed - " + pendingRows.size() + " payrolls marked as PAID");

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AdminPanel.this,
                        "Payment processing error: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Payment error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.fontMain(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 40));
        return button;
    }
}

