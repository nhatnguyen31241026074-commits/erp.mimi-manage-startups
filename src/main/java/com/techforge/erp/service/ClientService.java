package com.techforge.erp.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techforge.erp.model.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ClientService {

    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private final DatabaseReference clientsRef;

    public ClientService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.clientsRef = root.child("clients");
    }

    public List<Client> getAllClients() {
        try {
            CompletableFuture<List<Client>> future = new CompletableFuture<>();
            clientsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Client> list = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                Client c = child.getValue(Client.class);
                                if (c != null) {
                                    c.setId(child.getKey());
                                    list.add(c);
                                }
                            } catch (Exception e) {
                                logger.warn("Error parsing client record {}: {}", child.getKey(), e.getMessage());
                            }
                        }
                    }
                    future.complete(list);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new RuntimeException("Firebase cancelled: " + error.getMessage()));
                }
            });

            return future.get();
        } catch (Exception e) {
            logger.error("Failed to load clients from Firebase", e);
            return new ArrayList<>();
        }
    }
}

