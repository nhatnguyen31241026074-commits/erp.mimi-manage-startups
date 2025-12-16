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

    public UserService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.usersRef = root.child("users");
        logger.info("UserService initialized with Firebase path: LTUD10/users");
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
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        try {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<User> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            User u = child.getValue(User.class);
                            if (u != null) list.add(u);
                        }
                    }
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
}
