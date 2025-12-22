package com.techforge.erp.controller;

import com.techforge.erp.model.Client;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final DatabaseReference clientsRef;

    public ClientController() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.clientsRef = root.child("clients");
    }

    @GetMapping
    public ResponseEntity<Object> getAllClients() {
        List<Client> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            clientsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        if (snapshot != null && snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                try {
                                    Client c = child.getValue(Client.class);
                                    if (c != null) {
                                        c.setId(child.getKey());
                                        result.add(c);
                                    }
                                } catch (Exception ex) {
                                    // skip malformed
                                }
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching clients: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Object> createClient(@RequestBody Client client) {
        if (client == null) return ResponseEntity.badRequest().body("client is required");
        try {
            String key = clientsRef.push().getKey();
            if (key == null) return ResponseEntity.status(500).body("Failed to generate client id");
            client.setId(key);
            clientsRef.child(key).setValueAsync(client).addListener(() -> {}, Runnable::run);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating client: " + e.getMessage());
        }
    }
}
