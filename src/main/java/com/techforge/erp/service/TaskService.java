package com.techforge.erp.service;

import com.google.firebase.database.*;
import com.techforge.erp.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TaskService {

    private final DatabaseReference tasksRef;

    public TaskService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.tasksRef = root.child("tasks");
    }

    public CompletableFuture<Task> createTask(Task task) {
        CompletableFuture<Task> future = new CompletableFuture<>();
        try {
            String key = (task.getId() != null && !task.getId().isEmpty()) ? task.getId() : tasksRef.push().getKey();
            if (key == null) {
                future.completeExceptionally(new IllegalStateException("Unable to generate key for task"));
                return future;
            }
            task.setId(key);
            tasksRef.child(key).setValueAsync(task).addListener(() -> future.complete(task), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Task> getTaskById(String id) {
        CompletableFuture<Task> future = new CompletableFuture<>();
        try {
            tasksRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Task t = snapshot.getValue(Task.class);
                        future.complete(t);
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

    public CompletableFuture<List<Task>> getAllTasks() {
        CompletableFuture<List<Task>> future = new CompletableFuture<>();
        try {
            tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Task> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Task t = child.getValue(Task.class);
                            if (t != null) list.add(t);
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

    public CompletableFuture<Void> updateTask(Task task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (task.getId() == null || task.getId().isEmpty()) {
                future.completeExceptionally(new IllegalArgumentException("Task id is required for update"));
                return future;
            }
            tasksRef.child(task.getId()).setValueAsync(task).addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Void> deleteTask(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            tasksRef.child(id).removeValueAsync().addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}

