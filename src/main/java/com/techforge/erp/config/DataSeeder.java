package com.techforge.erp.config;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techforge.erp.model.Client;
import com.techforge.erp.model.User;
import com.techforge.erp.service.ClientService;
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

    @Autowired
    private ClientService clientService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== DataSeeder: Checking if demo data needs to be seeded ===");

        try {
            // Check if vegeta already exists
            User existingUser = userService.getUserByEmail("vegeta@saiyan.com").join();

            if (existingUser != null) {
                logger.info("Demo data already exists. Skipping seeding.");
                logger.info("Found user: {} ({})", existingUser.getUsername(), existingUser.getRole());
                // Still ensure clients exist; if not, seed them
                try {
                    List<Client> existingClients = clientService.getAllClients();
                    if (existingClients == null || existingClients.isEmpty()) {
                        seedDemoClients();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not verify existing clients: {}", ex.getMessage());
                }
                return;
            }

            logger.info("No demo data found. Creating demo users...");
            seedDemoUsers();
            // ensure demo clients exist
            try { seedDemoClients(); } catch (Exception ex) { logger.warn("Failed to seed demo clients: {}", ex.getMessage()); }
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

    private void seedDemoClients() {
        logger.info("Seeding demo clients...");
        try {
            DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
            DatabaseReference clientsRef = root.child("clients");

            Client c1 = new Client();
            c1.setName("Capsule Corp");
            c1.setEmail("contact@capsule.corp");

            String key1 = clientsRef.push().getKey();
            if (key1 != null) {
                c1.setId(key1);
                clientsRef.child(key1).setValueAsync(c1).addListener(() -> logger.info("Seeded client: {} (id={})", c1.getName(), key1), Runnable::run);
            }

            Client c2 = new Client();
            c2.setName("Red Ribbon Army");
            c2.setEmail("contact@redribbon.com");

            String key2 = clientsRef.push().getKey();
            if (key2 != null) {
                c2.setId(key2);
                clientsRef.child(key2).setValueAsync(c2).addListener(() -> logger.info("Seeded client: {} (id={})", c2.getName(), key2), Runnable::run);
            }
        } catch (Exception e) {
            logger.error("Failed to seed demo clients: {}", e.getMessage(), e);
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
