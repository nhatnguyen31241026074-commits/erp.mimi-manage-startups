package com.techforge;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techforge.erp.model.Project;
import com.techforge.erp.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class TechForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechForgeApplication.class, args);
    }

    // Bean này sẽ tự chạy sau khi App khởi động xong
    @Bean
    public CommandLineRunner testRealtimeDatabaseConnection() {
        return args -> {
            System.out.println("⏳ Đang chuẩn bị tạo dữ liệu mẫu...");

            // Đợi 3 giây để Firebase khởi tạo xong
            Thread.sleep(3000);

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference();

            if (ref == null) {
                System.err.println("❌ Lỗi: Không lấy được Realtime Database Reference!");
                return;
            }

            // --- 1. Tạo User ảo ---
            User admin = new User();
            String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
            admin.setId(userId);
            admin.setUsername("admin_test");
            admin.setEmail("admin@techforge.com");
            admin.setFullName("Nguyen Van Admin");
            admin.setRole("CEO");
            admin.setHourlyRate(500000);
            admin.setCreatedAt(new Date());

            try {
                ApiFuture<Void> userFuture = ref.child("LTUD10").child("users").child(userId).setValueAsync(admin);
                userFuture.get();
                System.out.println("✅ Đã tạo User với id: " + userId);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("❌ Lỗi khi tạo User: " + e.getMessage());
            }

            // --- 2. Tạo Project ảo ---
            Project prj = new Project();
            String prjId = "prj_" + UUID.randomUUID().toString().substring(0, 8);
            prj.setId(prjId);
            prj.setName("TechForge ERP System");
            prj.setDescription("Dự án quản lý nội bộ");
            prj.setBudget(100000000L);
            prj.setStatus("RUNNING");
            prj.setStartDate(new Date());

            try {
                ApiFuture<Void> prjFuture = ref.child("LTUD10").child("projects").child(prjId).setValueAsync(prj);
                prjFuture.get();
                System.out.println("✅ Đã tạo Project với id: " + prjId);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("❌ Lỗi khi tạo Project: " + e.getMessage());
            }

            System.out.println("🎉 DỮ LIỆU MẪU ĐÃ ĐƯỢC TẠO THÀNH CÔNG!");
        };
    }
}