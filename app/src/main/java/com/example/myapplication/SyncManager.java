package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncManager {
    private DatabaseHelper dbHelper;
    private DatabaseReference firebaseRef;
    private Context context;
    private static final String TAG = "SyncManager";

    // Listeners pentru notificări despre progres
    public interface SyncListener {
        void onSyncStarted();
        void onSyncProgress(String message);
        void onSyncComplete(boolean success, String message);
    }

    private SyncListener syncListener;

    public SyncManager(Context context) {
        this.context = context;
        // Fixed: Use singleton instance
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.firebaseRef = FirebaseDatabase.getInstance().getReference();
    }

    public void setSyncListener(SyncListener listener) {
        this.syncListener = listener;
    }

    // Sync SQLite to Firebase
    public void syncToFirebase() {
        if (syncListener != null) {
            syncListener.onSyncStarted();
        }

        List<SyncOperation> operations = dbHelper.getUnsyncedOperations();

        Log.d(TAG, "Syncing " + operations.size() + " operations to Firebase");

        if (syncListener != null) {
            syncListener.onSyncProgress("Sincronizare " + operations.size() + " operații către Firebase...");
        }

        if (operations.isEmpty()) {
            if (syncListener != null) {
                syncListener.onSyncComplete(true, "Nu există operații de sincronizat");
            }
            return;
        }

        final AtomicInteger completedOperations = new AtomicInteger(0);
        final AtomicInteger failedOperations = new AtomicInteger(0);
        final int totalOperations = operations.size();

        for (SyncOperation op : operations) {
            switch (op.operation) {
                case "INSERT":
                case "UPDATE":
                    if (op.tableName.equals("spitale")) {
                        Spital spital = dbHelper.jsonToSpital(op.data);
                        if (spital != null) {
                            firebaseRef.child("spitale").child(op.recordId).setValue(spital)
                                    .addOnSuccessListener(aVoid -> {
                                        dbHelper.markOperationAsSynced(op.id);
                                        Log.d(TAG, "Synced to Firebase: " + op.recordId);

                                        int completed = completedOperations.incrementAndGet();
                                        if (syncListener != null) {
                                            syncListener.onSyncProgress("Sincronizat " + completed + "/" + totalOperations);
                                        }

                                        if (completed + failedOperations.get() == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completed + " succese, " + failedOperations.get() + " eșecuri");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to sync: " + e.getMessage());
                                        int failed = failedOperations.incrementAndGet();

                                        if (completedOperations.get() + failed == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completedOperations.get() + " succese, " + failed + " eșecuri");
                                            }
                                        }
                                    });
                        }
                    } else if (op.tableName.equals("medici")) {
                        Medic medic = dbHelper.jsonToMedic(op.data);
                        if (medic != null) {
                            firebaseRef.child("medici").child(op.recordId).setValue(medic)
                                    .addOnSuccessListener(aVoid -> {
                                        dbHelper.markOperationAsSynced(op.id);
                                        Log.d(TAG, "Synced medic to Firebase: " + op.recordId);

                                        int completed = completedOperations.incrementAndGet();
                                        if (syncListener != null) {
                                            syncListener.onSyncProgress("Sincronizat " + completed + "/" + totalOperations);
                                        }

                                        if (completed + failedOperations.get() == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completed + " succese, " + failedOperations.get() + " eșecuri");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to sync medic: " + e.getMessage());
                                        int failed = failedOperations.incrementAndGet();

                                        if (completedOperations.get() + failed == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completedOperations.get() + " succese, " + failed + " eșecuri");
                                            }
                                        }
                                    });
                        }
                    } else if (op.tableName.equals("users")) {
                        HelperClass user = dbHelper.jsonToUser(op.data);
                        if (user != null) {
                            // Atenție: pentru securitate, normalmente nu sincronizăm parolele
                            // Această metodă este doar pentru demonstrație
                            firebaseRef.child("users").child(op.recordId).setValue(user)
                                    .addOnSuccessListener(aVoid -> {
                                        dbHelper.markOperationAsSynced(op.id);
                                        Log.d(TAG, "Synced user to Firebase: " + op.recordId);

                                        int completed = completedOperations.incrementAndGet();
                                        if (syncListener != null) {
                                            syncListener.onSyncProgress("Sincronizat " + completed + "/" + totalOperations);
                                        }

                                        if (completed + failedOperations.get() == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completed + " succese, " + failedOperations.get() + " eșecuri");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to sync user: " + e.getMessage());
                                        int failed = failedOperations.incrementAndGet();

                                        if (completedOperations.get() + failed == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completedOperations.get() + " succese, " + failed + " eșecuri");
                                            }
                                        }
                                    });
                        }
                    } else if (op.tableName.equals(DatabaseHelper.TABLE_PROGRAMARI)) {
                        Map<String, Object> programare = dbHelper.jsonToProgramareMap(op.data);
                        if (programare != null) {
                            firebaseRef.child("programari").child(op.recordId).setValue(programare)
                                    .addOnSuccessListener(aVoid -> {
                                        dbHelper.markOperationAsSynced(op.id);
                                        Log.d(TAG, "Synced programare to Firebase: " + op.recordId);

                                        int completed = completedOperations.incrementAndGet();
                                        if (syncListener != null) {
                                            syncListener.onSyncProgress("Sincronizat " + completed + "/" + totalOperations);
                                        }

                                        if (completed + failedOperations.get() == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completed + " succese, " + failedOperations.get() + " eșecuri");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to sync programare: " + e.getMessage());
                                        int failed = failedOperations.incrementAndGet();

                                        if (completedOperations.get() + failed == totalOperations) {
                                            if (syncListener != null) {
                                                syncListener.onSyncComplete(failedOperations.get() == 0,
                                                        "Sincronizare completă: " + completedOperations.get() + " succese, " + failed + " eșecuri");
                                            }
                                        }
                                    });
                        }
                    }
                    break;

                case "DELETE":
                    firebaseRef.child(op.tableName).child(op.recordId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                dbHelper.markOperationAsSynced(op.id);
                                Log.d(TAG, "Deleted from Firebase: " + op.recordId);

                                int completed = completedOperations.incrementAndGet();
                                if (syncListener != null) {
                                    syncListener.onSyncProgress("Sincronizat " + completed + "/" + totalOperations);
                                }

                                if (completed + failedOperations.get() == totalOperations) {
                                    if (syncListener != null) {
                                        syncListener.onSyncComplete(failedOperations.get() == 0,
                                                "Sincronizare completă: " + completed + " succese, " + failedOperations.get() + " eșecuri");
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete: " + e.getMessage());
                                int failed = failedOperations.incrementAndGet();

                                if (completedOperations.get() + failed == totalOperations) {
                                    if (syncListener != null) {
                                        syncListener.onSyncComplete(failedOperations.get() == 0,
                                                "Sincronizare completă: " + completedOperations.get() + " succese, " + failed + " eșecuri");
                                    }
                                }
                            });
                    break;
            }
        }
    }

    public void syncUsersFromFirebase() {
        firebaseRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    HelperClass user = snapshot.getValue(HelperClass.class);
                    if (user != null) {
                        // Setăm username-ul ca id
                        user.setUsername(snapshot.getKey());
                        dbHelper.insertOrUpdateUser(user, true); // true = from Firebase
                        count++;
                    }
                }
                Log.d(TAG, "Synced " + count + " users from Firebase to SQLite");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing users from Firebase: " + databaseError.getMessage());
            }
        });
    }

    // Sync Firebase to SQLite
    public void syncFromFirebase() {
        if (syncListener != null) {
            syncListener.onSyncStarted();
            syncListener.onSyncProgress("Preluare date din Firebase...");
        }

        final AtomicInteger completedTasks = new AtomicInteger(0);
        final AtomicInteger[] counts = new AtomicInteger[]{new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)};

        // Sync Spitale
        firebaseRef.child("spitale").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Spital spital = snapshot.getValue(Spital.class);
                    if (spital != null) {
                        spital.setId(snapshot.getKey());
                        dbHelper.insertOrUpdateSpital(spital, true); // true = from Firebase
                        count++;
                    }
                }
                counts[0].set(count);
                Log.d(TAG, "Synced " + count + " spitale from Firebase to SQLite");

                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(true,
                                "Sincronizare completă: " + counts[0].get() + " spitale, " + counts[1].get() + " medici, " + counts[2].get() + " programari");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing spitale from Firebase: " + databaseError.getMessage());
                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(false,
                                "Eroare la sincronizare: " + databaseError.getMessage());
                    }
                }
            }
        });

        // Sync Medici
        firebaseRef.child("medici").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        dbHelper.insertOrUpdateMedic(medic, true); // true = from Firebase
                        count++;
                    }
                }
                counts[1].set(count);
                Log.d(TAG, "Synced " + count + " medici from Firebase to SQLite");

                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(true,
                                "Sincronizare completă: " + counts[0].get() + " spitale, " + counts[1].get() + " medici, " + counts[2].get() + " programari");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing medici from Firebase: " + databaseError.getMessage());
                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(false,
                                "Eroare la sincronizare: " + databaseError.getMessage());
                    }
                }
            }
        });

        // Sync Programari
        firebaseRef.child("programari").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> programare = (Map<String, Object>) snapshot.getValue();
                    if (programare != null) {
                        programare.put("id", snapshot.getKey());
                        dbHelper.insertOrUpdateProgramare(programare, true); // true = from Firebase
                        count++;
                    }
                }
                counts[2].set(count);
                Log.d(TAG, "Synced " + count + " programari from Firebase to SQLite");

                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(true,
                                "Sincronizare completă: " + counts[0].get() + " spitale, " + counts[1].get() + " medici, " + counts[2].get() + " programari");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing programari from Firebase: " + databaseError.getMessage());
                if (completedTasks.incrementAndGet() == 3) {
                    if (syncListener != null) {
                        syncListener.onSyncComplete(false,
                                "Eroare la sincronizare: " + databaseError.getMessage());
                    }
                }
            }
        });
    }

    // Sincronizează programările de la Firebase la SQLite
    public void syncProgramariFromFirebase() {
        firebaseRef.child("programari").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> programare = (Map<String, Object>) snapshot.getValue();
                    if (programare != null) {
                        programare.put("id", snapshot.getKey());
                        dbHelper.insertOrUpdateProgramare(programare, true); // true = from Firebase
                        count++;
                    }
                }
                Log.d(TAG, "Synced " + count + " programari from Firebase to SQLite");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing programari from Firebase: " + databaseError.getMessage());
            }
        });
    }

    // Sincronizează programările pentru un anumit utilizator
    public void syncProgramariForUser(String userId) {
        firebaseRef.child("programari").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> programare = (Map<String, Object>) snapshot.getValue();
                            if (programare != null) {
                                programare.put("id", snapshot.getKey());
                                dbHelper.insertOrUpdateProgramare(programare, true);
                                count++;
                            }
                        }
                        Log.d(TAG, "Synced " + count + " programari for user: " + userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error syncing programari for user: " + databaseError.getMessage());
                    }
                });
    }

    // Sincronizează programările pentru un anumit medic într-o anumită dată
    public void syncProgramariForDateAndDoctor(String medicId, String data, String spital) {
        firebaseRef.child("programari").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> programare = (Map<String, Object>) snapshot.getValue();
                    if (programare != null &&
                            medicId.equals(programare.get("medicId")) &&
                            data.equals(programare.get("data")) &&
                            spital.equals(programare.get("spital"))) {

                        programare.put("id", snapshot.getKey());
                        dbHelper.insertOrUpdateProgramare(programare, true);
                        count++;
                    }
                }
                Log.d(TAG, "Synced " + count + " programari for medic " + medicId + " on " + data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing programari for medic/date: " + databaseError.getMessage());
            }
        });
    }

    // Full bidirectional sync
    public void doFullSync() {
        Log.d(TAG, "Starting full sync");

        if (syncListener != null) {
            syncListener.onSyncStarted();
            syncListener.onSyncProgress("Sincronizare bidirecțională în curs...");
        }

        // First sync from Firebase to make sure we have latest data
        syncFromFirebase();

        // Then sync to Firebase any local changes
        syncToFirebase();
    }

    // Sync specific spital and its medici
    public void syncSpitalWithMedici(String spitalId) {
        // Sync specific spital
        firebaseRef.child("spitale").child(spitalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Spital spital = dataSnapshot.getValue(Spital.class);
                if (spital != null) {
                    spital.setId(dataSnapshot.getKey());
                    dbHelper.insertOrUpdateSpital(spital, true);

                    // Sync medici for this spital
                    syncMediciForSpital(spital.getNume());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing spital: " + databaseError.getMessage());
            }
        });
    }

    // Sync medici for specific spital
    private void syncMediciForSpital(String spitalNume) {
        firebaseRef.child("medici").orderByChild("spital").equalTo(spitalNume)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Medic medic = snapshot.getValue(Medic.class);
                            if (medic != null) {
                                medic.setId(snapshot.getKey());
                                dbHelper.insertOrUpdateMedic(medic, true);
                                count++;
                            }
                        }
                        Log.d(TAG, "Synced " + count + " medici for spital: " + spitalNume);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error syncing medici for spital: " + databaseError.getMessage());
                    }
                });
    }

    // Metode suplimentare pentru gestionarea sincronizării
    public boolean hasPendingSyncOperations() {
        return !dbHelper.getUnsyncedOperations().isEmpty();
    }

    public void cleanup() {
        // Remove any Firebase listeners if any are active
        if (firebaseRef != null) {
            // If you have any active listeners, remove them here
            // For example:
            // firebaseRef.removeEventListener(someListener);
        }

        // Set references to null to help with garbage collection
        context = null;
        firebaseRef = null;
        dbHelper = null;

        Log.d(TAG, "OptimizedSyncManager cleaned up");
    }
    public void clearSyncLog() {
        // Fixed: Comment out the missing method
        // The method clearSyncedOperations() doesn't exist in DatabaseHelper
        // You would need to add it to DatabaseHelper if you want this functionality
        // dbHelper.clearSyncedOperations();
        Log.d(TAG, "Note: clearSyncedOperations() method is not implemented in DatabaseHelper");
    }
}