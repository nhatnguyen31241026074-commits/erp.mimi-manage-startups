package techforge.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class MomoPaymentDialog extends JDialog {
    private final Runnable onPaymentSuccess;
    private final double amount;

    public MomoPaymentDialog(Frame owner, double amount, Runnable onPaymentSuccess) {
        super(owner, "MoMo Payment Gateway", true);
        this.amount = amount;
        this.onPaymentSuccess = onPaymentSuccess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(owner);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("MoMo Payment");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAmount = new JLabel(String.format("%,.0f VND", amount));
        lblAmount.setFont(new Font("Arial", Font.PLAIN, 18));
        lblAmount.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblAmount.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JButton btnOpenWeb = new JButton("Open MoMo Web");
        btnOpenWeb.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenWeb.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://momo.vn/"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to open browser.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnConfirm = new JButton("I Have Paid");
        btnConfirm.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConfirm.setBackground(new Color(0x4CAF50));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setFont(new Font("Arial", Font.BOLD, 14));
        btnConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnConfirm.addActionListener(e -> {
            dispose();
            if (onPaymentSuccess != null) {
                onPaymentSuccess.run();
            }
        });

        contentPanel.add(lblTitle);
        contentPanel.add(lblAmount);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(btnOpenWeb);
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(btnConfirm);

        setContentPane(new JScrollPane(contentPanel));
    }
}

