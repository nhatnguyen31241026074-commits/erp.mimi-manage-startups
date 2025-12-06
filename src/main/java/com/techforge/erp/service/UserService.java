package com.techforge.erp.service;

import com.google.firebase.database.*;
import com.techforge.erp.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    private final DatabaseReference usersRef;

    public UserService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.usersRef = root.child("users");
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

