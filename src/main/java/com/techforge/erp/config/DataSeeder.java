package com.techforge.erp.config;

import com.techforge.erp.model.User;
import com.techforge.erp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Data Seeder - Automatically creates demo users on application startup.
 * Only seeds data if the database is empty (checks for "vegeta" user).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== DataSeeder: Checking if demo data needs to be seeded ===");

        try {
            // Check if vegeta already exists
            User existingUser = userService.getUserByEmail("vegeta@saiyan.com").join();

            if (existingUser != null) {
                logger.info("Demo data already exists. Skipping seeding.");
                logger.info("Found user: {} ({})", existingUser.getUsername(), existingUser.getRole());
                return;
            }

            logger.info("No demo data found. Creating demo users...");
            seedDemoUsers();
            logger.info("=== DataSeeder: Demo data seeding complete! ===");

        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }

    private void seedDemoUsers() {
        List<User> demoUsers = Arrays.asList(
                createUser("vegeta", "vegeta@saiyan.com", "123", "Vegeta Prince", "MANAGER", 8000.0, 75.0),
                createUser("goku", "goku@saiyan.com", "123", "Son Goku", "EMPLOYEE", 5000.0, 50.0),
                createUser("bulma", "bulma@capsule.corp", "123", "Bulma Brief", "ADMIN", 10000.0, 100.0),
                createUser("frieza", "frieza@empire.com", "123", "Lord Frieza", "CLIENT", 0.0, 0.0)
        );

        for (User user : demoUsers) {
            try {
                User saved = userService.createUser(user).join();
                logger.info("Created user: {} ({}) - ID: {}", saved.getUsername(), saved.getRole(), saved.getId());
            } catch (Exception e) {
                logger.error("Failed to create user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    private User createUser(String username, String email, String password, String fullName,
                            String role, Double baseSalary, Double hourlyRateOT) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setRole(role);
        user.setBaseSalary(baseSalary);
        user.setHourlyRateOT(hourlyRateOT);
        user.setSalaryType("monthly");
        return user;
    }
}

