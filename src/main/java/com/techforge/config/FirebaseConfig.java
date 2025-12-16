package com.techforge.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.database-url}")
    private String databaseUrl;

    @Value("${firebase.config.path:serviceAccountKey.json}")
    private String configPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing Firebase...");
                logger.info("Database URL: {}", databaseUrl);
                logger.info("Config path: {}", configPath);

                // Load service account from classpath
                Resource resource = new ClassPathResource(configPath);
                if (!resource.exists()) {
                    logger.error("Firebase config file not found: {}", configPath);
                    throw new IOException("Firebase config file not found: " + configPath);
                }

                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully!");

                // Test the connection
                testConnection();
            } else {
                logger.info("Firebase already initialized.");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    /**
     * Tests Firebase Realtime Database connection by attempting to read the root node.
     * This helps catch configuration issues early at startup.
     */
    private void testConnection() {
        logger.info("Testing Firebase connection...");

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        try {
            FirebaseDatabase.getInstance()
                .getReference(".info/connected")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean connected = snapshot.getValue(Boolean.class);
                        if (Boolean.TRUE.equals(connected)) {
                            logger.info("Firebase connection test PASSED - Database is reachable!");
                            success[0] = true;
                        } else {
                            logger.warn("Firebase connection test: Database not yet connected (this may be normal on startup)");
                            success[0] = true; // Still consider it a success - connection is being established
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        errorMessage[0] = error.getMessage();
                        logger.error("Firebase connection test FAILED: {}", error.getMessage());
                        latch.countDown();
                    }
                });

            // Wait up to 10 seconds for connection test
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (!completed) {
                logger.error("Firebase connection test TIMED OUT after 10 seconds!");
                logger.error("   Possible causes:");
                logger.error("   1. Incorrect database URL: {}", databaseUrl);
                logger.error("   2. Network/firewall blocking connection");
                logger.error("   3. Service account doesn't have database access");
            } else if (errorMessage[0] != null) {
                logger.error("Firebase error: {}", errorMessage[0]);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Firebase connection test interrupted", e);
        }
    }
}
