package com.techforge.erp.service;

import com.google.firebase.database.*;
import com.techforge.erp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final DatabaseReference usersRef;

    // Local cache for users (can be cleared for force reload)
    private volatile List<User> cachedUsers = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30000; // 30 seconds cache TTL

    public UserService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.usersRef = root.child("users");
        logger.info("UserService initialized with Firebase path: LTUD10/users");
    }

    /**
     * Force reload all users from Firebase immediately.
     * Clears local cache and fetches fresh data.
     * This is a BLOCKING call - waits for Firebase to return data.
     * @return List of all users with fresh data from Firebase
     */
    public List<User> forceReloadUsers() {
        logger.info("forceReloadUsers: Clearing cache and fetching fresh data from Firebase...");

        // Clear cache
        cachedUsers = null;
        cacheTimestamp = 0;

        try {
            // Blocking call to get fresh data
            CompletableFuture<List<User>> future = getAllUsersFromFirebase();
            List<User> users = future.get(10, java.util.concurrent.TimeUnit.SECONDS); // 10 second timeout

            // Update cache
            cachedUsers = users;
            cacheTimestamp = System.currentTimeMillis();

            logger.info("forceReloadUsers: Successfully loaded {} users from Firebase", users.size());
            return users;

        } catch (Exception e) {
            logger.error("forceReloadUsers: Failed to reload users from Firebase", e);
            return new ArrayList<>();
        }
    }

    /**
     * Force reload all users from Firebase with a callback.
     * Clears local cache and fetches fresh data ASYNCHRONOUSLY.
     * The callback is executed on the EDT (Swing Event Dispatch Thread) when data is ready.
     *
     * STRICT DATA ACCURACY: If hourlyRateOT is 0 or null in Firebase, it stays 0.0.
     * No fake default values are applied.
     *
     * @param onLoaded Callback to execute when users are loaded (runs on EDT)
     */
    public void forceReloadUsers(Runnable onLoaded) {
        logger.info("forceReloadUsers(callback): Clearing cache and fetching fresh data from Firebase...");

        // Clear cache immediately
        cachedUsers = null;
        cacheTimestamp = 0;

        getAllUsersFromFirebase().thenAccept(users -> {
            // Update cache
            cachedUsers = users;
            cacheTimestamp = System.currentTimeMillis();

            logger.info("forceReloadUsers(callback): Successfully loaded {} users from Firebase", users.size());

            // Log each user's hourlyRateOT for debugging
            for (User u : users) {
                logger.debug("User {} ({}): hourlyRateOT={}, baseSalary={}",
                    u.getFullName(), u.getId(), u.getHourlyRateOT(), u.getBaseSalary());
            }

            // Execute callback on EDT (Swing thread safety)
            if (onLoaded != null) {
                javax.swing.SwingUtilities.invokeLater(onLoaded);
            }
        }).exceptionally(ex -> {
            logger.error("forceReloadUsers(callback): Failed to reload users", ex);
            // Still execute callback even on error (so UI doesn't hang)
            if (onLoaded != null) {
                javax.swing.SwingUtilities.invokeLater(onLoaded);
            }
            return null;
        });
    }

    /**
     * Get all employees (users with role EMPLOYEE).
     * Forces a fresh reload from Firebase.
     */
    public List<User> getEmployees() {
        List<User> allUsers = forceReloadUsers();
        return allUsers.stream()
            .filter(u -> u != null && "EMPLOYEE".equalsIgnoreCase(u.getRole()))
            .collect(java.util.stream.Collectors.toList());
    }

    public CompletableFuture<User> createUser(User user) {
        CompletableFuture<User> future = new CompletableFuture<>();
        try {
            String key = (user.getId() != null && !user.getId().isEmpty()) ? user.getId() : usersRef.push().getKey();
            if (key == null) {
                future.completeExceptionally(new IllegalStateException("Unable to generate key for user"));
                return future;
            }
            user.setId(key);
            usersRef.child(key).setValueAsync(user).addListener(() -> future.complete(user), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<User> getUserByEmail(String email) {
        CompletableFuture<User> future = new CompletableFuture<>();
        try {
            Query q = usersRef.orderByChild("email").equalTo(email).limitToFirst(1);
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            User u = child.getValue(User.class);
                            future.complete(u);
                            return;
                        }
                    }
                    future.complete(null);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new RuntimeException("Firebase cancelled: " + error.getMessage()));
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<User> getUserById(String id) {
        CompletableFuture<User> future = new CompletableFuture<>();

        logger.info("Starting Firebase fetch for userId={}", id);

        try {
            usersRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    logger.info("Firebase returned data for userId={}, exists={}", id, snapshot.exists());
                    if (snapshot.exists()) {
                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            u.setId(id); // Ensure ID is set
                        }
                        logger.info("Firebase returned user: id={}, role={}", id, u != null ? u.getRole() : "null");
                        future.complete(u);
                    } else {
                        logger.warn("Firebase: No user found with id={}", id);
                        future.complete(null);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("Firebase error for userId={}: {}", id, error.getMessage());
                    future.completeExceptionally(new RuntimeException("Firebase cancelled: " + error.getMessage()));
                }
            });
        } catch (Exception e) {
            logger.error("Exception during Firebase fetch for userId={}: {}", id, e.getMessage(), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<List<User>> getAllUsers() {
        // Check cache first (if not expired)
        if (cachedUsers != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
            logger.debug("getAllUsers: Returning cached data ({} users)", cachedUsers.size());
            return CompletableFuture.completedFuture(new ArrayList<>(cachedUsers));
        }

        return getAllUsersFromFirebase();
    }

    /**
     * Internal method to fetch ALL users directly from Firebase.
     * Always hits the database (no cache).
     */
    private CompletableFuture<List<User>> getAllUsersFromFirebase() {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        try {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<User> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                // Use robust parsing instead of automatic mapping
                                User u = convertSnapshotToUser(child);
                                if (u != null) {
                                    list.add(u);
                                    logger.debug("Loaded user: id={}, name={}, rate={}",
                                        u.getId(), u.getFullName(), u.getHourlyRateOT());
                                }
                            } catch (Exception e) {
                                // Log error and continue to next user (don't crash the whole list)
                                logger.error("Error parsing user record id={}: {}", child.getKey(), e.getMessage());
                                System.err.println("ERROR parsing user " + child.getKey() + ": " + e.getMessage());
                            }
                        }
                    }

                    // Update cache
                    cachedUsers = list;
                    cacheTimestamp = System.currentTimeMillis();

                    logger.info("getAllUsersFromFirebase: Loaded {} users from Firebase", list.size());
                    future.complete(list);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new RuntimeException("Firebase cancelled: " + error.getMessage()));
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Robust snapshot-to-User conversion with safe type handling.
     * Handles:
     * - Long vs Double for numeric fields (baseSalary, hourlyRateOT)
     * - Null safety for all string fields
     * - Data type mismatches that would crash automatic Firebase mapping
     */
    private User convertSnapshotToUser(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        User user = new User();

        // Set ID from snapshot key
        user.setId(snapshot.getKey());

        // String fields with null safety
        user.setUsername(getStringValue(snapshot, "username"));
        user.setEmail(getStringValue(snapshot, "email"));
        user.setPassword(getStringValue(snapshot, "password"));
        user.setFullName(getStringValue(snapshot, "fullName"));
        user.setPhone(getStringValue(snapshot, "phone"));
        user.setRole(getStringValue(snapshot, "role"));
        user.setSalaryType(getStringValue(snapshot, "salaryType"));

        // Numeric fields with robust Long/Double handling
        user.setBaseSalary(getDoubleValue(snapshot, "baseSalary"));
        user.setHourlyRateOT(getDoubleValue(snapshot, "hourlyRateOT"));

        // OTP fields (if present)
        user.setOtp(getStringValue(snapshot, "otp"));
        user.setOtpExpiry(getStringValue(snapshot, "otpExpiry"));

        return user;
    }

    /**
     * Safely extract a String value from a DataSnapshot child.
     */
    private String getStringValue(DataSnapshot snapshot, String childName) {
        Object val = snapshot.child(childName).getValue();
        if (val == null) {
            return null;
        }
        return val.toString();
    }

    /**
     * Safely extract a Double value from a DataSnapshot child.
     * Handles Long (integers), Double (decimals), and String representations.
     * Firebase numbers can come in any of these formats depending on how they were stored.
     *
     * STRICT DATA ACCURACY:
     * - If the field is NULL in Firebase -> returns 0.0 (safe default for calculations)
     * - If the field is explicitly 0 in Firebase -> returns 0.0
     * - If the field has a valid number -> returns that number
     * - NO fake default values like 10.0 are ever applied
     */
    private Double getDoubleValue(DataSnapshot snapshot, String childName) {
        Object val = snapshot.child(childName).getValue();
        if (val == null) {
            // STRICT: Return 0.0 for null - no fake defaults
            logger.debug("Field '{}' is null for user {}, returning 0.0", childName, snapshot.getKey());
            return 0.0;
        }

        // Handle Number types (Long, Double, Integer, Float, etc.)
        if (val instanceof Number) {
            double result = ((Number) val).doubleValue();
            logger.debug("Field '{}' = {} (type: {}) for user {}",
                childName, result, val.getClass().getSimpleName(), snapshot.getKey());
            return result;
        }

        // Handle String representation of numbers
        if (val instanceof String) {
            String strVal = ((String) val).trim();
            if (strVal.isEmpty()) {
                logger.debug("Field '{}' is empty string for user {}, returning 0.0", childName, snapshot.getKey());
                return 0.0;
            }
            try {
                double result = Double.parseDouble(strVal);
                logger.debug("Field '{}' parsed from String = {} for user {}", childName, result, snapshot.getKey());
                return result;
            } catch (NumberFormatException e) {
                logger.warn("Could not parse String '{}' as Double for field '{}' in user {}",
                    strVal, childName, snapshot.getKey());
                return 0.0;
            }
        }

        // Last resort: try toString() and parse
        try {
            double result = Double.parseDouble(val.toString());
            logger.debug("Field '{}' parsed via toString = {} for user {}", childName, result, snapshot.getKey());
            return result;
        } catch (NumberFormatException e) {
            logger.warn("Could not parse {} as Double for user {}: {}", childName, snapshot.getKey(), val);
            return 0.0;
        }
    }

    public CompletableFuture<User> updateUser(User user) {
        CompletableFuture<User> future = new CompletableFuture<>();
        try {
            if (user.getId() == null || user.getId().isEmpty()) {
                future.completeExceptionally(new IllegalArgumentException("User ID is required for update"));
                return future;
            }

            logger.info("Updating user: id={}", user.getId());
            usersRef.child(user.getId()).setValueAsync(user).addListener(() -> {
                logger.info("User updated successfully: id={}", user.getId());
                future.complete(user);
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            future.completeExceptionally(e);
        }
        return future;
    }
}
