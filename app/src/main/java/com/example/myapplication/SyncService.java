package com.example.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final int SYNC_INTERVAL_MINUTES = 30; // Sincronizare la fiecare 30 de minute

    private ScheduledExecutorService scheduler;
    private SyncManager syncManager;
    private DatabaseHelper dbHelper;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        syncManager = new SyncManager(this);
        dbHelper = DatabaseHelper.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        // Configurează listener pentru evenimente de sincronizare
        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                Log.d(TAG, "Background sync started");
            }

            @Override
            public void onSyncProgress(String message) {
                Log.d(TAG, "Sync progress: " + message);
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                Log.d(TAG, "Background sync completed: " + (success ? "Success" : "Failed"));

                // Notifică utilizatorul doar dacă sincronizarea eșuează
                if (!success) {
                    mainHandler.post(() -> {
                        Toast.makeText(SyncService.this,
                                "Sincronizare background eșuată",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sync service started");

        // Inițializează procesul de sincronizare periodică
        startPeriodicSync();

        // Efectuează o sincronizare imediată la pornire
        performSync();

        return START_STICKY;
    }

    private void startPeriodicSync() {
        // Anulează orice task programat anterior
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Creează un nou scheduler pentru sincronizarea periodică
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    Log.d(TAG, "Performing scheduled sync");
                    performSync();
                } else {
                    Log.d(TAG, "Network not available, skipping scheduled sync");
                }
            }
        }, SYNC_INTERVAL_MINUTES, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void performSync() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available for sync");
            return;
        }

        // Verifică dacă există operații în așteptare de sincronizat
        if (syncManager.hasPendingSyncOperations()) {
            Log.d(TAG, "Pending operations found, syncing to Firebase");
            syncManager.syncToFirebase();
        }

        // Sincronizează mereu de la Firebase pentru a avea datele cele mai recente
        Log.d(TAG, "Syncing from Firebase");
        syncManager.syncFromFirebase();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Oprește scheduler-ul când serviciul este oprit
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        Log.d(TAG, "Sync service destroyed");
    }

    // Metode statice pentru controlul serviciului din activități
    public static void startSyncService(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        context.startService(intent);
    }

    public static void stopSyncService(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        context.stopService(intent);
    }
}