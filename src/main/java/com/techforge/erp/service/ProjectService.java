package com.techforge.erp.service;

import com.google.firebase.database.*;
import com.techforge.erp.model.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ProjectService {

    private final DatabaseReference projectsRef;

    public ProjectService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.projectsRef = root.child("projects");
    }

    public CompletableFuture<Project> createProject(Project project) {
        CompletableFuture<Project> future = new CompletableFuture<>();
        try {
            String key = (project.getId() != null && !project.getId().isEmpty()) ? project.getId() : projectsRef.push().getKey();
            if (key == null) {
                future.completeExceptionally(new IllegalStateException("Unable to generate key for project"));
                return future;
            }
            project.setId(key);
            projectsRef.child(key).setValueAsync(project).addListener(() -> future.complete(project), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Project> getProjectById(String id) {
        CompletableFuture<Project> future = new CompletableFuture<>();
        try {
            projectsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Project p = snapshot.getValue(Project.class);
                        future.complete(p);
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

    public CompletableFuture<List<Project>> getAllProjects() {
        CompletableFuture<List<Project>> future = new CompletableFuture<>();
        try {
            projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Project> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Project p = child.getValue(Project.class);
                            if (p != null) list.add(p);
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

    public CompletableFuture<Void> updateProject(Project project) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (project.getId() == null || project.getId().isEmpty()) {
                future.completeExceptionally(new IllegalArgumentException("Project id is required for update"));
                return future;
            }
            projectsRef.child(project.getId()).setValueAsync(project).addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Void> deleteProject(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            projectsRef.child(id).removeValueAsync().addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
