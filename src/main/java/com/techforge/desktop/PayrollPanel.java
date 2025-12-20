package com.techforge.desktop;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Import the standardized MomoPaymentDialog
import techforge.ui.MomoPaymentDialog;

public class PayrollPanel extends JPanel {
    private JTable payrollTable;
    private DefaultTableModel tableModel;
    private JButton btnRefresh;
    private JButton btnPayViaMomo;
    private JLabel lblStatus;
    private final String[] columns = {"Employee", "Role", "Hours", "Base Salary", "OT Rate", "Total", "Status"};
    private List<User> employees = new ArrayList<>();

    public PayrollPanel() {
        setLayout(new BorderLayout(15, 15));
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        payrollTable = new JTable(tableModel);
        payrollTable.setRowHeight(32);
        JScrollPane scrollPane = new JScrollPane(payrollTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("Refresh");
        btnPayViaMomo = new JButton("Pay via MoMo");
        lblStatus = new JLabel("");
        topPanel.add(btnRefresh);
        topPanel.add(btnPayViaMomo);
        topPanel.add(lblStatus);
        add(topPanel, BorderLayout.NORTH);

        btnRefresh.addActionListener(e -> loadPayrollData());
        btnPayViaMomo.addActionListener(e -> onPayViaMomo());

        loadPayrollData();
    }

    private void loadPayrollData() {
        tableModel.setRowCount(0);
        // Simulate fetching users from Firebase (replace with real service call)
        employees = UserService.getInstance().getAllEmployees();
        for (User user : employees) {
            double hours = user.getHourlyRateOT(); // For testing: treat as hours
            double baseSalary = user.getBaseSalary();
            double otRate = 10.0; // Hardcoded for test
            double total = hours * otRate;
            String status = hours > 0 ? "PENDING" : "NO_WORK";
            tableModel.addRow(new Object[] {
                user.getFullName(),
                user.getRole(),
                String.format("%.2f", hours),
                String.format("%.2f", baseSalary),
                "$10.00",
                String.format("%.2f", total),
                status
            });
        }
        lblStatus.setText("Loaded " + employees.size() + " employees.");
    }

    private int getTotalColumnIndex() {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equals("Total")) return i;
        }
        return 5; // fallback
    }

    private void onPayViaMomo() {
        double total = 0.0;
        for (int i = 0; i < payrollTable.getRowCount(); i++) {
            Object val = payrollTable.getValueAt(i, getTotalColumnIndex());
            if (val instanceof Number) {
                total += ((Number) val).doubleValue();
            } else if (val != null) {
                try {
                    total += Double.parseDouble(val.toString().replace(",", ""));
                } catch (Exception ignored) {}
            }
        }
        if (total <= 0) {
            JOptionPane.showMessageDialog(this, "No payroll to pay.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Use canonical MomoPaymentDialog constructor with callback to refresh
        new MomoPaymentDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this),
            total,
            () -> {
                loadPayrollData(); // Callback to refresh table
                JOptionPane.showMessageDialog(this, "Paid successfully!");
            }
        ).setVisible(true);
    }

    // Dummy User class for demonstration. Replace with your actual User class.
    public static class User {
        private String fullName;
        private String role;
        private double hourlyRateOT;
        private double baseSalary;
        public User(String fullName, String role, double hourlyRateOT, double baseSalary) {
            this.fullName = fullName;
            this.role = role;
            this.hourlyRateOT = hourlyRateOT;
            this.baseSalary = baseSalary;
        }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public double getHourlyRateOT() { return hourlyRateOT; }
        public double getBaseSalary() { return baseSalary; }
    }

    // Dummy UserService for demonstration. Replace with your actual service.
    public static class UserService {
        private static UserService instance;
        public static UserService getInstance() {
            if (instance == null) instance = new UserService();
            return instance;
        }
        public List<User> getAllEmployees() {
            // Replace with real Firebase fetch
            List<User> list = new ArrayList<>();
            list.add(new User("Goku", "EMPLOYEE", 10, 1000));
            list.add(new User("Vegeta", "EMPLOYEE", 0, 1200));
            list.add(new User("Bulma", "ADMIN", 0, 2000));
            return list;
        }
    }
}
