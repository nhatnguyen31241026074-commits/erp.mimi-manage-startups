package com.techforge.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * MomoPaymentDialog - Premium MoMo Payment Gateway Simulator.
 * Features:
 * - Real QR Code fetched from QR Server API
 * - Professional MoMo branding (Pink/Magenta #A50064)
 * - "Pay on Web" button to open actual MoMo test gateway
 * - Proper VND currency formatting
 */
public class MomoPaymentDialog extends JDialog {
    private final Runnable onPaymentSuccess;
    private final double amount;

    public MomoPaymentDialog(Frame owner, double amount, Runnable onPaymentSuccess) {
        super(owner, "MoMo Payment Gateway", true);
        this.amount = amount;
        this.onPaymentSuccess = onPaymentSuccess;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel momoLogo = new JLabel("MoMo Payment", SwingConstants.CENTER);
        momoLogo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        momoLogo.setForeground(new Color(0xA50064));
        momoLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(momoLogo);
        contentPanel.add(Box.createVerticalStrut(20));

        // Amount
        JLabel amountLabel = new JLabel(String.format("%s VND", NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(amount)));
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        amountLabel.setForeground(new Color(0xA50064));
        amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(amountLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Order ID (mock)
        String orderId = "ORD-" + System.currentTimeMillis();
        JLabel orderIdLabel = new JLabel("Order ID: " + orderId);
        orderIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        orderIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(orderIdLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Description
        JLabel descLabel = new JLabel("Payroll Payment via MoMo");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // QR Code
        JLabel qrLabel = new JLabel();
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(qrLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Pay URL (static for demo)
        String payUrl = "https://test-payment.momo.vn/v2/gateway/pay?t=TU9NT3wzYjZiZjNjMC1iOGU2LTRhNmMtOWE3NC0xMTFlZTFmYjRkNzQx&s=6f442e3cebd27a50a71bc742a69f5906017e017f7cbd605f0c42c99cf2ec2b72";
        try {
            String qrApi = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + URLEncoder.encode(payUrl, "UTF-8");
            BufferedImage image = ImageIO.read(URI.create(qrApi).toURL());
            qrLabel.setIcon(new ImageIcon(image));
        } catch (IOException ex) {
            qrLabel.setText("[QR Code Error]");
        }

        // Open Web Button
        JButton btnOpenWeb = new JButton("🔗 Open MoMo Gateway");
        btnOpenWeb.setBackground(new Color(0xA50064));
        btnOpenWeb.setForeground(Color.WHITE);
        btnOpenWeb.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnOpenWeb.setFocusPainted(false);
        btnOpenWeb.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenWeb.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(payUrl));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open browser.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        contentPanel.add(btnOpenWeb);
        contentPanel.add(Box.createVerticalStrut(20));

        // Confirm Button
        JButton btnConfirm = new JButton("I Have Paid");
        btnConfirm.setBackground(new Color(0xA50064));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnConfirm.setFocusPainted(false);
        btnConfirm.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConfirm.addActionListener(e -> {
            dispose();
            if (onPaymentSuccess != null) {
                onPaymentSuccess.run();
            }
        });
        contentPanel.add(btnConfirm);
        contentPanel.add(Box.createVerticalStrut(10));

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }
}
