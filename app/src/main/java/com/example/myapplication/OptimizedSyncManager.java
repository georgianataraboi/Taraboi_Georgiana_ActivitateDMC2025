package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class OptimizedSyncManager {
    private static final String TAG = "OptimizedSyncManager";
    private Context context;
    private DatabaseHelper dbHelper;
    private DatabaseReference firebaseRef;

    // Correct DataCallback interface with all required methods
    public interface DataCallback {
        void onSuccess(Object data);
        void onError(String error);
        void onDataAvailable(String data); // This method must be implemented
    }

    public OptimizedSyncManager(Context context) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.firebaseRef = FirebaseDatabase.getInstance().getReference();
    }

    // Add this interface and method to OptimizedSyncManager:

    // Add this interface in OptimizedSyncManager class
    public interface AvailabilityCallback {
        void onLocalDataAvailable(List<Map<String, Object>> programari);
        void onUpdatedDataAvailable(List<Map<String, Object>> programari);
    }

    // Add this method to OptimizedSyncManager class
    public void checkAvailability(String medicId, String date, String spital, final AvailabilityCallback callback) {
        // First, get data from SQLite
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Map<String, Object>> programari = dbHelper.getProgramariForDate(medicId, date, spital);

                // Return local data immediately
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onLocalDataAvailable(programari);
                        }
                    }
                });

                // Then sync from Firebase in background
                syncProgramariForDateAndDoctor(medicId, date, spital, programari, callback);
            }
        }).start();
    }

    // Helper method to sync programari
    private void syncProgramariForDateAndDoctor(String medicId, String date, String spital,
                                                List<Map<String, Object>> localProgramari,
                                                final AvailabilityCallback callback) {
        firebaseRef.child("programari").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Map<String, Object>> programari = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> programare = (Map<String, Object>) snapshot.getValue();
                    if (programare != null &&
                            medicId.equals(programare.get("medicId")) &&
                            date.equals(programare.get("data")) &&
                            spital.equals(programare.get("spital"))) {

                        programare.put("id", snapshot.getKey());
                        programari.add(programare);

                        // Save to SQLite
                        dbHelper.insertOrUpdateProgramare(programare, true);
                    }
                }

                // Return updated data
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onUpdatedDataAvailable(programari);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing programari: " + databaseError.getMessage());
                // Just keep the local data if Firebase fails
            }
        });
    }

    // Also add the saveProgramare method that's used in ProgramareActivity:
    public interface SaveCallback {
        void onSavedLocally();
        void onSavedToFirebase(boolean success);
        void onWillRetryLater();
        void onError(String error);
    }

    public void saveProgramare(Map<String, Object> programare, final SaveCallback callback) {
        // Save to SQLite first
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long result = dbHelper.insertOrUpdateProgramare(programare, false);

                    if (result > 0) {
                        // Notify that it's saved locally
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onSavedLocally();
                                }
                            }
                        });

                        // Try to sync to Firebase
                        syncProgramareToFirebase(programare, callback);
                    } else {
                        // Local save failed
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onError("Failed to save locally");
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError(e.getMessage());
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void syncProgramareToFirebase(Map<String, Object> programare, final SaveCallback callback) {
        String programareId = (String) programare.get("id");
        if (programareId != null) {
            firebaseRef.child("programari").child(programareId).setValue(programare)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) {
                            callback.onSavedToFirebase(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to sync to Firebase: " + e.getMessage());
                        if (callback != null) {
                            callback.onSavedToFirebase(false);
                            callback.onWillRetryLater();
                        }
                    });
        }
    }
    // Load medici for a specific spital
    public void loadMediciForSpital(String spital, final DataCallback callback) {
        // First, load from SQLite
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get medici from SQLite
                List<Medic> mediciFromSQLite = dbHelper.getMediciForSpital(spital);

                // Return SQLite data immediately on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(mediciFromSQLite);
                        }
                    }
                });

                // Then sync from Firebase in background
                syncMediciForSpitalFromFirebase(spital, new DataCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        // Firebase data loaded successfully
                        if (data instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Medic> mediciFromFirebase = (List<Medic>) data;

                            // Return Firebase data on main thread
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onSuccess(mediciFromFirebase);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Log Firebase sync failure - user already has SQLite data
                        Log.e(TAG, "Failed to sync medici for spital from Firebase: " + error);
                    }

                    @Override
                    public void onDataAvailable(String data) {
                        // Implementation for required abstract method
                        Log.d(TAG, "Additional data available: " + data);
                    }
                });
            }
        }).start();
    }

    // Helper method to sync medici for specific spital from Firebase
    private void syncMediciForSpitalFromFirebase(String spital, final DataCallback callback) {
        firebaseRef.child("medici").orderByChild("spital").equalTo(spital)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Medic> medici = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Medic medic = snapshot.getValue(Medic.class);
                            if (medic != null) {
                                medic.setId(snapshot.getKey());
                                medici.add(medic);

                                // Save to SQLite for offline access
                                dbHelper.insertOrUpdateMedic(medic, true);
                            }
                        }

                        if (callback != null) {
                            callback.onSuccess(medici);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (callback != null) {
                            callback.onError(databaseError.getMessage());
                        }
                    }
                });
    }

    // Load all medici (if needed)
    public void loadAllMedici(final DataCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get all medici from SQLite
                List<Medic> mediciFromSQLite = dbHelper.getAllMedici();

                // Return SQLite data immediately
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(mediciFromSQLite);
                        }
                    }
                });

                // Then sync from Firebase
                syncAllMediciFromFirebase(callback);
            }
        }).start();
    }

    // Helper method to sync all medici from Firebase
    private void syncAllMediciFromFirebase(final DataCallback callback) {
        firebaseRef.child("medici").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Medic> medici = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        medici.add(medic);

                        // Save to SQLite
                        dbHelper.insertOrUpdateMedic(medic, true);
                    }
                }

                // Return Firebase data on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(medici);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Return error on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onError(databaseError.getMessage());
                        }
                    }
                });
            }
        });
    }

    // Load spitale
    public void loadSpitale(final DataCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get spitale from SQLite
                List<Spital> spitaleFromSQLite = dbHelper.getAllSpitale();

                // Return SQLite data immediately
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(spitaleFromSQLite);
                        }
                    }
                });

                // Then sync from Firebase
                syncSpitaleFromFirebase(callback);
            }
        }).start();
    }


    // Helper method to sync spitale from Firebase
    private void syncSpitaleFromFirebase(final DataCallback callback) {
        firebaseRef.child("spitale").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Spital> spitale = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Spital spital = snapshot.getValue(Spital.class);
                    if (spital != null) {
                        spital.setId(snapshot.getKey());
                        spitale.add(spital);

                        // Save to SQLite
                        dbHelper.insertOrUpdateSpital(spital, true);
                    }
                }

                // Return Firebase data on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(spitale);
                        }
                    }
                });
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

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Return error on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onError(databaseError.getMessage());
                        }
                    }
                });
            }
        });
    }
}