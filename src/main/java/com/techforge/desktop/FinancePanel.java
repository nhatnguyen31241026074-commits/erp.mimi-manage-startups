package com.techforge.desktop;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * FinancePanel - displays transaction/payroll history fetched from backend using HttpURLConnection
 */
public class FinancePanel extends JPanel {

    private final DefaultTableModel transactionModel;
    private final JTable transactionTable;
    private final Timer refreshTimer;

    public FinancePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel title = new JLabel("Transaction History");
        title.setFont(AppTheme.fontHeading(18));
        title.setForeground(AppTheme.SECONDARY);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        transactionModel = new DefaultTableModel(new Object[]{"Transaction ID", "Employee Name", "Type", "Amount", "Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(transactionModel);
        JScrollPane scroll = new JScrollPane(transactionTable);
        add(scroll, BorderLayout.CENTER);

        // Load transactions on init
        loadTransactions();

        // Refresh every 10 seconds to simulate realtime sync
        refreshTimer = new Timer(10_000, e -> loadTransactions());
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    private void loadTransactions() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<JsonObject> rows = new ArrayList<>();
            private Map<String, String> userNames = new HashMap<>();

            @Override
            protected Void doInBackground() {
                // 1) Fetch all users to map userId -> fullName (single call)
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    java.net.URL url = java.net.URI.create("http://localhost:8080/api/v1/users").toURL();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);

                        JsonElement parsed = JsonParser.parseString(sb.toString());
                        if (parsed.isJsonArray()) {
                            JsonArray arr = parsed.getAsJsonArray();
                            for (JsonElement el : arr) {
                                try {
                                    JsonObject obj = el.getAsJsonObject();
                                    if (obj.has("id") && !obj.get("id").isJsonNull()) {
                                        String id = obj.get("id").getAsString();
                                        String name = null;
                                        if (obj.has("fullName") && !obj.get("fullName").isJsonNull()) name = obj.get("fullName").getAsString();
                                        if (name == null || name.isEmpty()) {
                                            if (obj.has("username") && !obj.get("username").isJsonNull()) name = obj.get("username").getAsString();
                                        }
                                        if (name == null) name = "(Unknown)";
                                        userNames.put(id, name);
                                    }
                                } catch (Exception ignore) {}
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore - userNames map may be empty
                } finally {
                    try { if (reader != null) reader.close(); } catch (Exception ignored) {}
                    if (conn != null) conn.disconnect();
                }

                // 2) Fetch transactions (payrolls) from finance endpoint
                try {
                    java.net.URL url = java.net.URI.create("http://localhost:8080/api/v1/finance/transactions").toURL();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);

                        JsonElement parsed = JsonParser.parseString(sb.toString());
                        if (parsed.isJsonArray()) {
                            JsonArray arr = parsed.getAsJsonArray();
                            for (JsonElement el : arr) {
                                if (el.isJsonObject()) rows.add(el.getAsJsonObject());
                            }
                        } else if (parsed.isJsonObject()) {
                            rows.add(parsed.getAsJsonObject());
                        }
                    }
                } catch (Exception e) {
                    // ignore - rows may remain empty
                } finally {
                    try { if (reader != null) reader.close(); } catch (Exception ignored) {}
                    if (conn != null) conn.disconnect();
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    transactionModel.setRowCount(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

                    for (JsonObject tx : rows) {
                        String txId = "";
                        if (tx.has("transactionId") && !tx.get("transactionId").isJsonNull()) txId = tx.get("transactionId").getAsString();
                        else if (tx.has("id") && !tx.get("id").isJsonNull()) txId = tx.get("id").getAsString();

                        String userId = tx.has("userId") && !tx.get("userId").isJsonNull() ? tx.get("userId").getAsString() : null;
                        String empName = "";
                        if (tx.has("employeeName") && !tx.get("employeeName").isJsonNull()) empName = tx.get("employeeName").getAsString();
                        else if (userId != null && userNames.containsKey(userId)) empName = userNames.get(userId);
                        else if (userId != null) empName = userId;

                        String type = "Payroll";

                        String amount = "";
                        if (tx.has("totalPay") && !tx.get("totalPay").isJsonNull()) {
                            amount = "$" + String.format("%,.2f", tx.get("totalPay").getAsDouble());
                        } else if (tx.has("amount") && !tx.get("amount").isJsonNull()) {
                            amount = "$" + String.format("%,.2f", tx.get("amount").getAsDouble());
                        }

                        String date = sdf.format(new Date());
                        if (tx.has("month") && tx.has("year") && !tx.get("month").isJsonNull() && !tx.get("year").isJsonNull()) {
                            int m = tx.get("month").getAsInt();
                            int y = tx.get("year").getAsInt();
                            date = String.format("%02d/%d", m, y);
                        } else if (tx.has("date") && !tx.get("date").isJsonNull()) {
                            date = tx.get("date").getAsString();
                        }

                        String status = "";
                        if (tx.has("isPaid") && !tx.get("isPaid").isJsonNull()) {
                            boolean paid = tx.get("isPaid").getAsBoolean();
                            status = paid ? "PAID" : "PENDING";
                        } else if (tx.has("status") && !tx.get("status").isJsonNull()) {
                            status = tx.get("status").getAsString();
                        }

                        transactionModel.addRow(new Object[]{txId, empName, type, amount, date, status});
                    }
                } catch (Exception e) {
                    System.err.println("Error populating transactions table: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }
}
