package com.techforge.desktop;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;

// Import Backend
import com.techforge.TechForgeApplication;
import org.springframework.boot.builder.SpringApplicationBuilder; // <--- MỚI THÊM CÁI NÀY

// Import Frontend Login Screen
import com.techforge.desktop.LoginFrame;

public class UnifiedLauncher {

    public static void main(String[] args) {

        // --- QUAN TRỌNG: ÉP SPRING BOOT KHÔNG ĐƯỢC TẮT MÀN HÌNH ---
        System.setProperty("java.awt.headless", "false");

        // 1. CHẠY BACKEND (SPRING BOOT) Ở LUỒNG RIÊNG
        Thread backendThread = new Thread(() -> {
            try {
                System.out.println("[UnifiedLauncher] Đang khởi động Backend...");

                // Thay vì gọi main() thường, ta dùng Builder để ép tắt headless mode lần nữa cho chắc
                new SpringApplicationBuilder(TechForgeApplication.class)
                        .headless(false) // <--- DÒNG QUAN TRỌNG NHẤT
                        .run(args);

            } catch (Throwable t) {
                t.printStackTrace();
                System.err.println("[UnifiedLauncher] Lỗi khởi động Backend: " + t.getMessage());
            }
        }, "TechForge-Backend-Thread");

        backendThread.setDaemon(false);
        backendThread.start();

        // 2. NGỦ 3 GIÂY ĐỂ ĐỢI BACKEND LÊN
        try {
            System.out.println("[UnifiedLauncher] Đang đợi 3 giây để Server khởi tạo...");
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. KHỞI ĐỘNG GIAO DIỆN (FRONTEND)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Không load được theme, dùng mặc định.");
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // MỞ MÀN HÌNH ĐĂNG NHẬP
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
                System.out.println("[UnifiedLauncher] Giao diện đã lên!");
            } catch (Throwable uiErr) {
                uiErr.printStackTrace();
                JOptionPane.showMessageDialog(null, "Lỗi hiển thị: " + uiErr.getMessage());
            }
        });
    }
}