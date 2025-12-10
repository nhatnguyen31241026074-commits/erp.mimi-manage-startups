package com.techforge.erp.service;

import com.google.firebase.database.*;
import com.techforge.erp.model.User;
import com.techforge.erp.model.WorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class WorkLogService {

    private final DatabaseReference worklogsRef;
    private final UserService userService;

    @Autowired
    public WorkLogService(UserService userService) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.worklogsRef = root.child("worklogs");
        this.userService = userService;
    }

    public CompletableFuture<WorkLog> createWorkLog(WorkLog workLog) {
        CompletableFuture<WorkLog> future = new CompletableFuture<>();
        try {
            if (workLog.getUserId() == null || workLog.getUserId().isEmpty()) {
                future.completeExceptionally(new IllegalArgumentException("userId is required"));
                return future;
            }
            // fetch user to snapshot salary
            userService.getUserById(workLog.getUserId()).thenAccept(user -> {
                if (user == null) {
                    future.completeExceptionally(new IllegalStateException("User not found for id: " + workLog.getUserId()));
                    return;
                }
                // snapshot salary
                Double base = user.getBaseSalary();
                Double ot = user.getHourlyRateOT();
                workLog.setBaseSalarySnapshot(base);
                workLog.setHourlyRateOTSnapshot(ot);

                try {
                    String key = (workLog.getId() != null && !workLog.getId().isEmpty()) ? workLog.getId() : worklogsRef.push().getKey();
                    if (key == null) {
                        future.completeExceptionally(new IllegalStateException("Unable to generate key for worklog"));
                        return;
                    }
                    workLog.setId(key);
                    worklogsRef.child(key).setValueAsync(workLog).addListener(() -> future.complete(workLog), Runnable::run);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<WorkLog> getWorkLogById(String id) {
        CompletableFuture<WorkLog> future = new CompletableFuture<>();
        try {
            worklogsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        WorkLog w = snapshot.getValue(WorkLog.class);
                        future.complete(w);
                    } else {
                        future.complete(null);
                    }
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

    public CompletableFuture<List<WorkLog>> getAllWorkLogs() {
        CompletableFuture<List<WorkLog>> future = new CompletableFuture<>();
        try {
            worklogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<WorkLog> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            WorkLog w = child.getValue(WorkLog.class);
                            if (w != null) list.add(w);
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

    public CompletableFuture<Void> updateWorkLog(WorkLog workLog) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (workLog.getId() == null || workLog.getId().isEmpty()) {
                future.completeExceptionally(new IllegalArgumentException("WorkLog id is required for update"));
                return future;
            }
            worklogsRef.child(workLog.getId()).setValueAsync(workLog).addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Void> deleteWorkLog(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            worklogsRef.child(id).removeValueAsync().addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}

