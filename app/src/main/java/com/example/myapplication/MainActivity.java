package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    // Declară variabilele pentru preferințe și UI
    private boolean isDarkModeEnabled = false;
    private String lastVisitedHospital = "";
    private SharedPreferences prefsApp;
    private SharedPreferences prefsUser;

    // Adaugă helper-urile pentru baza de date
    private DatabaseHelper dbHelper;
    private SyncManager syncManager;
    private boolean isSyncing = false;

    // UI elements
    private TextView syncStatusText;
    private ProgressBar syncProgressBar;
    private ImageButton syncButton;  // Corectat: ImageButton în loc de Button
    private TextView lastVisitText;
    private Button migrationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Încarcă preferințele utilizatorului înainte de a seta layout-ul
        loadUserPreferences();

        // Aplică tema în funcție de preferințe
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        // Inițializează helper-urile pentru baza de date
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new SyncManager(this);

        // Inițializează UI elements
        syncStatusText = findViewById(R.id.sync_status_text);
        syncProgressBar = findViewById(R.id.sync_progress_bar);
        syncButton = findViewById(R.id.sync_button);
        lastVisitText = findViewById(R.id.last_visit_text);
        migrationButton = findViewById(R.id.migration_button);

        // Setează listener pentru evenimente de sincronizare
        setupSyncListener();

        // Verifică dacă trebuie să migrezi datele de la Firebase la SQLite
        checkMigrationStatus();

        // Pornește serviciul de sincronizare automată
        SyncService.startSyncService(this);

        // Inițializează controlele
        setupUIControls();
    }

    private void setupSyncListener() {
        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                isSyncing = true;
                runOnUiThread(() -> {
                    syncProgressBar.setVisibility(View.VISIBLE);
                    syncStatusText.setText("Sincronizare...");
                    syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_purple));
                    syncButton.setEnabled(false);
                    // Animație de rotație pentru butonul de sync
                    syncButton.animate().rotation(360f).setDuration(1000).start();
                });
            }

            @Override
            public void onSyncProgress(String message) {
                runOnUiThread(() -> {
                    syncStatusText.setText(message);
                });
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                isSyncing = false;
                runOnUiThread(() -> {
                    syncProgressBar.setVisibility(View.GONE);
                    syncButton.setEnabled(true);
                    syncButton.animate().rotation(0f).setDuration(300).start();

                    if (success) {
                        syncStatusText.setText("Sincronizat");
                        syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_purple));
                    } else {
                        syncStatusText.setText("Eroare");
                        syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        Toast.makeText(MainActivity.this,
                                "Eroare la sincronizare: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void checkMigrationStatus() {
        boolean isDataMigrated = prefsApp.getBoolean("isDataMigrated", false);
        boolean isDataInitialized = prefsApp.getBoolean("isDataInitialized", false);

        if (!isDataMigrated && isDataInitialized) {
            // Arată butonul de migrare
            migrationButton.setVisibility(View.VISIBLE);
            migrationButton.setOnClickListener(v -> showMigrationDialog());

            // Notifică utilizatorul că există date care trebuie migrate
            syncStatusText.setText("Date în Firebase");
            syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (!isDataInitialized) {
            // Trebuie inițializate datele în Firebase
            migrationButton.setVisibility(View.GONE);
            syncStatusText.setText("Inițializare...");
            initializeFirebaseData();
        } else {
            // Datele sunt deja migrate, ascunde butonul
            migrationButton.setVisibility(View.GONE);

            // Verifică starea sincronizării
            updateSyncStatus();
        }
    }

    private void setupUIControls() {
        // Setează iconurile și butoanele
        ImageView programareIcon = findViewById(R.id.programare_icon);
        ImageView spitaleIcon = findViewById(R.id.spitale_icon);
        ImageView mediciIcon = findViewById(R.id.medici_icon);
        Button exploreButton = findViewById(R.id.explore_button);
        Button detaliiButton = findViewById(R.id.detalii_button);
        Button statisticsButton = findViewById(R.id.statistics_button);
        SwitchCompat darkModeSwitch = findViewById(R.id.dark_mode_switch);

        // Găsește alte elemente UI care ar trebui să schimbe culoarea
        TextView welcomeText = findViewById(R.id.welcome_title); // Adaugă ID-ul corect
//        LinearLayout mainLayout = findViewById(R.id.); // Adaugă ID-ul corect

        // Updatează display-ul ultimului spital vizitat
        updateLastVisitText();

        // Configurează switch-ul pentru mod întunecat
        darkModeSwitch.setChecked(isDarkModeEnabled);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDarkModeEnabled = isChecked;
            saveUserPreferences();

            // Schimbă culorile butoanelor și elementelor UI
            updateUIColors(isChecked);
        });

        // Aplică culorile la început
        updateUIColors(isDarkModeEnabled);

        // Buton de sincronizare
        syncButton.setOnClickListener(v -> performSync());

        // Butoane de navigare (codul tău existent)
        programareIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProgramareActivity.class);
            startActivity(intent);
        });

        spitaleIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SpitaleActivity.class);
            startActivity(intent);
        });

        mediciIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MediciActivity.class);
            startActivity(intent);
        });

        exploreButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExploreActivity.class);
            startActivity(intent);
        });

        detaliiButton.setOnClickListener(v -> showDetailsDialog());

        statisticsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });
    }

    // Metodă pentru a actualiza culorile UI
    private void updateUIColors(boolean isDarkMode) {
        // Definește culorile pentru modul normal și întunecat
        int buttonBackgroundColor;
        int buttonTextColor;
        int backgroundTint;
        int textColor;
        int subtitleColor;
        int descriptionColor;

        if (isDarkMode) {
            // Culori pentru modul întunecat
            buttonBackgroundColor = ContextCompat.getColor(this, R.color.button_dark_background);
            buttonTextColor = ContextCompat.getColor(this, R.color.button_dark_text);
            backgroundTint = ContextCompat.getColor(this, R.color.dark_background_tint);
            textColor = ContextCompat.getColor(this, R.color.dark_text_color);
            subtitleColor = ContextCompat.getColor(this, R.color.dark_subtitle_color);
            descriptionColor = ContextCompat.getColor(this, R.color.dark_description_color);
        } else {
            // Culori pentru modul normal
            buttonBackgroundColor = ContextCompat.getColor(this, R.color.button_light_background);
            buttonTextColor = ContextCompat.getColor(this, R.color.button_light_text);
            backgroundTint = ContextCompat.getColor(this, R.color.light_background_tint);
            textColor = ContextCompat.getColor(this, R.color.light_text_color);
            subtitleColor = ContextCompat.getColor(this, R.color.light_subtitle_color);
            descriptionColor = ContextCompat.getColor(this, R.color.light_description_color);
        }

        // Aplică culorile la butoane
        Button exploreButton = findViewById(R.id.explore_button);
        Button detaliiButton = findViewById(R.id.detalii_button);
        Button statisticsButton = findViewById(R.id.statistics_button);

        // Schimbă backgroundul și culoarea textului butoanelor
        if (exploreButton != null) {
            exploreButton.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
            exploreButton.setTextColor(buttonTextColor);
        }

        if (detaliiButton != null) {
            detaliiButton.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
            detaliiButton.setTextColor(buttonTextColor);
        }

        if (statisticsButton != null) {
            statisticsButton.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
            statisticsButton.setTextColor(buttonTextColor);
        }

        // Schimbă culoarea icoanelor (dacă sunt ImageView-uri cu tint)
//        ImageView programareIcon = findViewById(R.id.programare_icon);
//        ImageView spitaleIcon = findViewById(R.id.spitale_icon);
//        ImageView mediciIcon = findViewById(R.id.medici_icon);

        int iconTint = isDarkMode ? ContextCompat.getColor(this, R.color.icon_dark_tint) :
                ContextCompat.getColor(this, R.color.icon_light_tint);

//        if (programareIcon != null) programareIcon.setColorFilter(iconTint);
//        if (spitaleIcon != null) spitaleIcon.setColorFilter(iconTint);
//        if (mediciIcon != null) mediciIcon.setColorFilter(iconTint);

        // Schimbă culoarea textului (dacă ai)
        TextView welcomeText = findViewById(R.id.welcome_title);
        TextView lastVisitText = findViewById(R.id.last_visit_text);
        TextView subtitleText = findViewById(R.id.subtitle);
        TextView descriptionText = findViewById(R.id.description);

        if (welcomeText != null) welcomeText.setTextColor(textColor);
        if (lastVisitText != null) lastVisitText.setTextColor(textColor);
        if (subtitleText != null) subtitleText.setTextColor(subtitleColor);
        if (descriptionText != null) descriptionText.setTextColor(descriptionColor);
//        // Schimbă backgroundul principal (opțional)
//        View mainLayout = findViewById(R.id.main_layout);
//        if (mainLayout != null) {
//            mainLayout.setBackgroundColor(backgroundTint);
//        }
    }

    private void showMigrationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Migrare Date");
        builder.setMessage("Datele vor fi migrate de la Firebase la SQLite pentru o experiență mai rapidă și funcționalitate offline.\n\nAceasta operație va dura câteva momente.");
        builder.setCancelable(false);

        builder.setPositiveButton("Începe migrarea", (dialog, which) -> {
            performMigration();
        });

        builder.setNegativeButton("Mai târziu", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void performMigration() {
        // Arată progress
        migrationButton.setText("Migrare în curs...");
        migrationButton.setEnabled(false);

        // Sincronizează toate datele de la Firebase la SQLite
        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                runOnUiThread(() -> {
                    syncProgressBar.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Migrare începută...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSyncProgress(String message) {
                runOnUiThread(() -> {
                    syncStatusText.setText("Migrare: " + message);
                });
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    syncProgressBar.setVisibility(View.GONE);

                    if (success) {
                        // Marchează migrarea ca finalizată
                        SharedPreferences.Editor editor = prefsApp.edit();
                        editor.putBoolean("isDataMigrated", true);
                        editor.apply();

                        // IMPORTANT: Ascunde butonul de migrare
                        migrationButton.setVisibility(View.GONE);

                        Toast.makeText(MainActivity.this,
                                "Migrare completă! Aplicația este acum optimizată pentru lucru offline.",
                                Toast.LENGTH_LONG).show();

                        // Restabilește listener-ul normal pentru sincronizare
                        setupSyncListener();

                        // Actualizează status-ul
                        updateSyncStatus();
                    } else {
                        migrationButton.setText("Migrare date → SQLite");
                        migrationButton.setEnabled(true);
                        Toast.makeText(MainActivity.this,
                                "Eroare la migrare: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        syncManager.syncFromFirebase();
    }

    private void performSync() {
        if (isSyncing) {
            Toast.makeText(this, "Sincronizare deja în curs...", Toast.LENGTH_SHORT).show();
            return;
        }

        syncManager.doFullSync();
    }

    private void updateLastVisitText() {
        if (!lastVisitedHospital.isEmpty()) {
            lastVisitText.setText("Ultimul spital vizitat: " + lastVisitedHospital);
            lastVisitText.setVisibility(View.VISIBLE);
        } else {
            lastVisitText.setText("Ultimul spital vizitat: -");
            lastVisitText.setVisibility(View.VISIBLE);
        }
    }

    private void updateSyncStatus() {
        if (syncManager.hasPendingSyncOperations()) {
            syncStatusText.setText("Modificări nesincronizate");
            syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            syncStatusText.setText("Sincronizat");
            syncStatusText.setTextColor(getResources().getColor(android.R.color.holo_purple));
        }
    }

    // Restul metodelor rămân la fel...
    private void showDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Despre GeoMed");

        View view = getLayoutInflater().inflate(R.layout.dialog_about, null);
        builder.setView(view);

        builder.setPositiveButton("Închide", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Vezi pe hartă", (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("latitude", 44.4424);
            intent.putExtra("longitude", 26.0892);
            intent.putExtra("title", "GeoMed Headquarters");
            startActivity(intent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveUserPreferences() {
        prefsUser = getSharedPreferences("GeoMedUserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefsUser.edit();
        editor.putBoolean("darkMode", isDarkModeEnabled);
        editor.putString("lastVisitedHospital", lastVisitedHospital);
        editor.apply();
    }

    private void loadUserPreferences() {
        prefsUser = getSharedPreferences("GeoMedUserPrefs", MODE_PRIVATE);
        prefsApp = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        isDarkModeEnabled = prefsUser.getBoolean("darkMode", false);
        lastVisitedHospital = prefsUser.getBoolean("hasVisitedHospital", false) ?
                prefsUser.getString("lastVisitedHospital", "") : "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserPreferences();
        updateLastVisitText();
        updateSyncStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nu oprim serviciul când activitatea este distrusă, el rulează în background
    }

    public static void setLastVisitedHospital(Context context, String hospitalName) {
        SharedPreferences prefs = context.getSharedPreferences("GeoMedUserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastVisitedHospital", hospitalName);
        editor.putBoolean("hasVisitedHospital", true);
        editor.apply();
    }

    // Metodă pentru inițializarea datelor Firebase (ca backup)
    private void initializeFirebaseData() {
        // Codul tău existent pentru inițializarea datelor în Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference spitaleRef = database.getReference("spitale");
        DatabaseReference mediciRef = database.getReference("medici");
        // Verifică dacă datele au fost deja inițializate
        boolean isDataInitialized = prefsApp.getBoolean("isDataInitialized", false);

        if (!isDataInitialized) {
            // SPITALE MILITARE DIN ROMÂNIA

            // 1. Spitalul Militar Central București
            Map<String, Object> spitalCentral = new HashMap<>();
            spitalCentral.put("nume", "Spitalul Militar Central");
            spitalCentral.put("adresa", "Str. Mircea Vulcănescu 88, București");
            spitalCentral.put("telefon", "021 319 3051");
            spitalCentral.put("website", "https://www.scumc.ro");
            spitalCentral.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_central.jpg");
            spitalCentral.put("descriere", "Spitalul Universitar de Urgență Militar Central 'Dr. Carol Davila' este una dintre cele mai prestigioase unități medicale militare din România, cu tradiție în excelență medicală. Oferă servicii complete de diagnosticare și tratament pentru o gamă largă de afecțiuni.");
                    spitalCentral.put("locatie", "44.4424,26.0892");

            spitaleRef.push().setValue(spitalCentral);

            // 2. Spitalul Militar Cluj-Napoca
            Map<String, Object> spitalCluj = new HashMap<>();
            spitalCluj.put("nume", "Spitalul Militar Cluj-Napoca");
            spitalCluj.put("adresa", "Str. General Traian Moșoiu 22, Cluj-Napoca");
            spitalCluj.put("telefon", "0264 598 381");
            spitalCluj.put("website", "https://www.spitalulmilitarcluj.ro");
            spitalCluj.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_cluj.jpg");
            spitalCluj.put("descriere", "Spitalul Militar Cluj-Napoca este o unitate medicală de prestigiu din Transilvania, oferind servicii medicale de înaltă calitate. Instituția are o tradiție îndelungată în domeniul medical militar și civil.");
            spitalCluj.put("locatie", "46.7712,23.5896");

            spitaleRef.push().setValue(spitalCluj);

            // 3. Spitalul Militar Timișoara
            Map<String, Object> spitalTimisoara = new HashMap<>();
            spitalTimisoara.put("nume", "Spitalul Militar Timișoara");
            spitalTimisoara.put("adresa", "Str. Gheorghe Lazăr 7, Timișoara");
            spitalTimisoara.put("telefon", "0256 493 352");
            spitalTimisoara.put("website", "https://www.spitalulmilitartimisoara.ro");
            spitalTimisoara.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_timisoara.jpg");
            spitalTimisoara.put("descriere", "Spitalul Militar Timișoara oferă asistență medicală de urgență și servicii specializate pentru militari și civili. Unitatea dispune de echipamente moderne și personal medical cu înaltă calificare.");
            spitalTimisoara.put("locatie", "45.7489,21.2295");

            spitaleRef.push().setValue(spitalTimisoara);

            // 4. Spitalul Militar Craiova
            Map<String, Object> spitalCraiova = new HashMap<>();
            spitalCraiova.put("nume", "Spitalul Militar Craiova");
            spitalCraiova.put("adresa", "Str. Caracal 150, Craiova");
            spitalCraiova.put("telefon", "0251 582 300");
            spitalCraiova.put("website", "https://www.spitalmilitarcraiova.ro");
            spitalCraiova.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_craiova.jpg");
            spitalCraiova.put("descriere", "Spitalul Militar Craiova deservește regiunea Olteniei cu servicii medicale de calitate. Unitatea dispune de secții diverse și aparatură modernă pentru diagnosticare și tratament.");
            spitalCraiova.put("locatie", "44.3114,23.8016");

            spitaleRef.push().setValue(spitalCraiova);

            // 5. Spitalul Militar Constanța
            Map<String, Object> spitalConstanta = new HashMap<>();
            spitalConstanta.put("nume", "Spitalul Militar Constanța");
            spitalConstanta.put("adresa", "Str. Dezrobirii 3-5, Constanța");
            spitalConstanta.put("telefon", "0241 660 390");
            spitalConstanta.put("website", "https://www.spitalmilitarconstanta.ro");
            spitalConstanta.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_constanta.jpg");
            spitalConstanta.put("descriere", "Spitalul Militar Constanța este o instituție medicală cu tradiție în zona Dobrogei, oferind servicii medicale complete. Unitatea este dotată cu echipamente moderne și dispune de personal medical specializat.");
            spitalConstanta.put("locatie", "44.1732,28.6231");

            spitaleRef.push().setValue(spitalConstanta);

            // 6. Spitalul Militar Brașov
            Map<String, Object> spitalBrasov = new HashMap<>();
            spitalBrasov.put("nume", "Spitalul Militar Brașov");
            spitalBrasov.put("adresa", "Str. Pieții 9, Brașov");
            spitalBrasov.put("telefon", "0268 416 970");
            spitalBrasov.put("website", "https://www.spitalmilitarbrasov.ro");
            spitalBrasov.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_brasov.jpg");
            spitalBrasov.put("descriere", "Spitalul Militar Brașov oferă servicii medicale de calitate pentru zona centrală a țării. Unitatea deservește atât personalul militar, cât și pacienți civili, având secții diverse și laboratoare modernizate.");
            spitalBrasov.put("locatie", "45.6427,25.5887");

            spitaleRef.push().setValue(spitalBrasov);

            // 7. Spitalul Militar Iași
            Map<String, Object> spitalIasi = new HashMap<>();
            spitalIasi.put("nume", "Spitalul Militar Iași");
            spitalIasi.put("adresa", "Str. Berthelot 7-9, Iași");
            spitalIasi.put("telefon", "0232 210 930");
            spitalIasi.put("website", "https://www.spitalmilitariasi.ro");
            spitalIasi.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_iasi.jpg");
            spitalIasi.put("descriere", "Spitalul Militar Iași este o unitate medicală de referință pentru Moldova, cu specialități diverse și personal medical de înaltă calificare. Oferă servicii complete de diagnosticare și tratament.");
            spitalIasi.put("locatie", "47.1585,27.6014");

            spitaleRef.push().setValue(spitalIasi);

            // 8. Spitalul Militar Sibiu
            Map<String, Object> spitalSibiu = new HashMap<>();
            spitalSibiu.put("nume", "Spitalul Militar Sibiu");
            spitalSibiu.put("adresa", "Str. Constituției 19-21, Sibiu");
            spitalSibiu.put("telefon", "0269 233 769");
            spitalSibiu.put("website", "https://www.spitalmilitarsibiu.ro");
            spitalSibiu.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_sibiu.jpg");
            spitalSibiu.put("descriere", "Spitalul Militar Sibiu oferă asistență medicală de înaltă calitate, deservind regiunea centrală a României. Unitatea dispune de secții diverse și echipament modern de diagnostic și tratament.");
            spitalSibiu.put("locatie", "45.7983,24.1469");

            spitaleRef.push().setValue(spitalSibiu);

            // 9. Spitalul Militar Galați
            Map<String, Object> spitalGalati = new HashMap<>();
            spitalGalati.put("nume", "Spitalul Militar Galați");
            spitalGalati.put("adresa", "Str. Traian 199, Galați");
            spitalGalati.put("telefon", "0236 413 131");
            spitalGalati.put("website", "https://www.spitalmilitargalati.ro");
            spitalGalati.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_galati.jpg");
            spitalGalati.put("descriere", "Spitalul Militar Galați este o unitate medicală modernă care deservește zona de sud-est a României. Oferă o gamă largă de servicii medicale, având secții specializate și laboratoare performante.");
            spitalGalati.put("locatie", "45.4353,28.0080");

            spitaleRef.push().setValue(spitalGalati);

            // 10. Spitalul Militar Bacău
            Map<String, Object> spitalBacau = new HashMap<>();
            spitalBacau.put("nume", "Spitalul Militar Bacău");
            spitalBacau.put("adresa", "Str. Oituz 14, Bacău");
            spitalBacau.put("telefon", "0234 524 822");
            spitalBacau.put("website", "https://www.spitalmilitarbacau.ro");
            spitalBacau.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/spitale%2Fspital_bacau.jpg");
            spitalBacau.put("descriere", "Spitalul Militar Bacău oferă servicii medicale complexe pentru regiunea Moldovei. Unitatea dispune de secții diverse și laborator de analize modern, oferind îngrijire de calitate.");
            spitalBacau.put("locatie", "46.5670,26.9146");

            spitaleRef.push().setValue(spitalBacau);

            // MEDICI

            // Medici la Spitalul Militar Central

            // 1. Dr. Popescu Ion - Cardiologie
            Map<String, Object> medic1 = new HashMap<>();
            medic1.put("nume", "Popescu");
            medic1.put("prenume", "Ion");
            medic1.put("specialitate", "Cardiologie");
            medic1.put("spital", "Spitalul Militar Central");
            medic1.put("program", "Luni-Vineri: 08:00-14:00");
            medic1.put("telefon", "021 319 3051");
            medic1.put("email", "ion.popescu@scumc.ro");
            medic1.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor1.jpg");
            medic1.put("descriere", "Dr. Popescu este specialist în cardiologie intervențională cu peste 15 ani de experiență. Este absolvent al UMF Carol Davila și a efectuat stagii de pregătire în Franța și Germania.");

            mediciRef.push().setValue(medic1);

            // 2. Dr. Ionescu Maria - Neurologie
            Map<String, Object> medic2 = new HashMap<>();
            medic2.put("nume", "Ionescu");
            medic2.put("prenume", "Maria");
            medic2.put("specialitate", "Neurologie");
            medic2.put("spital", "Spitalul Militar Central");
            medic2.put("program", "Luni-Joi: 10:00-16:00");
            medic2.put("telefon", "021 319 3052");
            medic2.put("email", "maria.ionescu@scumc.ro");
            medic2.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor2.jpg");
            medic2.put("descriere", "Dr. Ionescu este specializată în diagnosticarea și tratarea afecțiunilor neurologice, cu focus pe boli neurodegenerative. A publicat numeroase studii în domeniul neurologiei clinice.");

            mediciRef.push().setValue(medic2);

            // 3. Dr. Dumitrescu Alexandru - Chirurgie Generală
            Map<String, Object> medic3 = new HashMap<>();
            medic3.put("nume", "Dumitrescu");
            medic3.put("prenume", "Alexandru");
            medic3.put("specialitate", "Chirurgie Generală");
            medic3.put("spital", "Spitalul Militar Central");
            medic3.put("program", "Luni, Miercuri, Vineri: 08:00-16:00");
            medic3.put("telefon", "021 319 3053");
            medic3.put("email", "alexandru.dumitrescu@scumc.ro");
            medic3.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor3.jpg");
            medic3.put("descriere", "Dr. Dumitrescu este chirurg cu experiență în intervenții laparoscopice și chirurgie oncologică. Este specializat în tratamentul afecțiunilor digestive și hepato-biliare.");

            mediciRef.push().setValue(medic3);

            // Medici la Spitalul Militar Cluj-Napoca

            // 4. Dr. Georgescu Andrei - Ortopedie
            Map<String, Object> medic4 = new HashMap<>();
            medic4.put("nume", "Georgescu");
            medic4.put("prenume", "Andrei");
            medic4.put("specialitate", "Ortopedie");
            medic4.put("spital", "Spitalul Militar Cluj-Napoca");
            medic4.put("program", "Luni, Miercuri, Vineri: 09:00-15:00");
            medic4.put("telefon", "0264 598 382");
            medic4.put("email", "andrei.georgescu@spitalmilitar.ro");
            medic4.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor4.jpg");
            medic4.put("descriere", "Dr. Georgescu este specializat în chirurgie ortopedică și traumatologie, cu expertiză în protezare articulară și chirurgie artroscopică. A efectuat numeroase intervenții de succes la nivelul genunchiului și șoldului.");

            mediciRef.push().setValue(medic4);

            // 5. Dr. Pop Mihaela - Oftalmologie
            Map<String, Object> medic5 = new HashMap<>();
            medic5.put("nume", "Pop");
            medic5.put("prenume", "Mihaela");
            medic5.put("specialitate", "Oftalmologie");
            medic5.put("spital", "Spitalul Militar Cluj-Napoca");
            medic5.put("program", "Marți-Vineri: 08:00-14:00");
            medic5.put("telefon", "0264 598 383");
            medic5.put("email", "mihaela.pop@spitalmilitar.ro");
            medic5.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor5.jpg");
            medic5.put("descriere", "Dr. Pop este medic primar oftalmolog, cu competențe în chirurgia cataractei și glaucomului. Utilizează tehnici moderne de diagnostic și tratament al afecțiunilor oculare.");

            mediciRef.push().setValue(medic5);

            // Medici la Spitalul Militar Timișoara

            // 6. Dr. Marinescu Vlad - Urologie
            Map<String, Object> medic6 = new HashMap<>();
            medic6.put("nume", "Marinescu");
            medic6.put("prenume", "Vlad");
            medic6.put("specialitate", "Urologie");
            medic6.put("spital", "Spitalul Militar Timișoara");
            medic6.put("program", "Luni-Joi: 09:00-15:00");
            medic6.put("telefon", "0256 493 353");
            medic6.put("email", "vlad.marinescu@spitalmilitar.ro");
            medic6.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor6.jpg");
            medic6.put("descriere", "Dr. Marinescu este specialist urolog, cu expertiză în tratamentul litiazei urinare și al afecțiunilor prostatei. Utilizează tehnici minim invazive pentru intervenții urologice.");

            mediciRef.push().setValue(medic6);

            // 7. Dr. Radu Elena - Dermatologie
            Map<String, Object> medic7 = new HashMap<>();
            medic7.put("nume", "Radu");
            medic7.put("prenume", "Elena");
            medic7.put("specialitate", "Dermatologie");
            medic7.put("spital", "Spitalul Militar Timișoara");
            medic7.put("program", "Luni, Marți, Joi: 10:00-16:00");
            medic7.put("telefon", "0256 493 354");
            medic7.put("email", "elena.radu@spitalmilitar.ro");
            medic7.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor7.jpg");
            medic7.put("descriere", "Dr. Radu este medic dermatolog cu expertiză în diagnosticarea și tratarea afecțiunilor cutanate. Este specializată în dermatoscopie și tratamentul afecțiunilor inflamatorii ale pielii.");

            mediciRef.push().setValue(medic7);

            // Medici la Spitalul Militar Craiova

            // 8. Dr. Diaconu Adrian - Gastroenterologie
            Map<String, Object> medic8 = new HashMap<>();
            medic8.put("nume", "Diaconu");
            medic8.put("prenume", "Adrian");
            medic8.put("specialitate", "Gastroenterologie");
            medic8.put("spital", "Spitalul Militar Craiova");
            medic8.put("program", "Luni-Vineri: 08:00-14:00");
            medic8.put("telefon", "0251 582 301");
            medic8.put("email", "adrian.diaconu@spitalmilitar.ro");
            medic8.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor8.jpg");
            medic8.put("descriere", "Dr. Diaconu este gastroenterolog cu experiență în endoscopie digestivă și tratamentul bolilor inflamatorii intestinale. Efectuează proceduri de diagnostic și terapeutice pentru afecțiunile digestive.");

            mediciRef.push().setValue(medic8);

            // Medici la Spitalul Militar Constanța

            // 9. Dr. Stancu Diana - ORL
            Map<String, Object> medic9 = new HashMap<>();
            medic9.put("nume", "Stancu");
            medic9.put("prenume", "Diana");
            medic9.put("specialitate", "ORL");
            medic9.put("spital", "Spitalul Militar Constanța");
            medic9.put("program", "Luni, Miercuri, Vineri: 09:00-15:00");
            medic9.put("telefon", "0241 660 391");
            medic9.put("email", "diana.stancu@spitalmilitar.ro");
            medic9.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor9.jpg");
            medic9.put("descriere", "Dr. Stancu este specializată în diagnosticarea și tratarea afecțiunilor ORL. Are competențe în chirurgia endoscopică sinusală și tratamentul tulburărilor de auz.");

            mediciRef.push().setValue(medic9);

            // Medici la Spitalul Militar Brașov

            // 10. Dr. Constantinescu Radu - Reumatologie
            Map<String, Object> medic10 = new HashMap<>();
            medic10.put("nume", "Constantinescu");
            medic10.put("prenume", "Radu");
            medic10.put("specialitate", "Reumatologie");
            medic10.put("spital", "Spitalul Militar Brașov");
            medic10.put("program", "Marți-Vineri: 09:00-15:00");
            medic10.put("telefon", "0268 416 971");
            medic10.put("email", "radu.constantinescu@spitalmilitar.ro");
            medic10.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor10.jpg");
            medic10.put("descriere", "Dr. Constantinescu este specialist în diagnosticarea și tratarea bolilor reumatice. Are expertiză în managementul artritei reumatoide și al spondilitei anchilozante.");

            mediciRef.push().setValue(medic10);

            // Medici la Spitalul Militar Iași

            // 11. Dr. Neagu Daniela - Endocrinologie
            Map<String, Object> medic11 = new HashMap<>();
            medic11.put("nume", "Neagu");
            medic11.put("prenume", "Daniela");
            medic11.put("specialitate", "Endocrinologie");
            medic11.put("spital", "Spitalul Militar Iași");
            medic11.put("program", "Luni-Joi: 08:00-14:00");
            medic11.put("telefon", "0232 210 931");
            medic11.put("email", "daniela.neagu@spitalmilitar.ro");
            medic11.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor11.jpg");
            medic11.put("descriere", "Dr. Neagu este specializată în diagnosticarea și tratarea afecțiunilor endocrine. Are expertiză în managementul diabetului și al afecțiunilor tiroidiene.");

            mediciRef.push().setValue(medic11);

            // Medici la Spitalul Militar Sibiu

            // 12. Dr. Badea Mircea - Pneumologie
            Map<String, Object> medic12 = new HashMap<>();
            medic12.put("nume", "Badea");
            medic12.put("prenume", "Mircea");
            medic12.put("specialitate", "Pneumologie");
            medic12.put("spital", "Spitalul Militar Sibiu");
            medic12.put("program", "Luni, Miercuri, Vineri: 08:00-14:00");
            medic12.put("telefon", "0269 233 770");
            medic12.put("email", "mircea.badea@spitalmilitar.ro");
            medic12.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor12.jpg");
            medic12.put("descriere", "Dr. Badea este specialist în pneumologie, cu expertiză în diagnosticarea și tratarea afecțiunilor respiratorii. Este specializat în bronhoscopie și tratamentul apneei de somn.");

            mediciRef.push().setValue(medic12);

            // Medici la Spitalul Militar Galați

            // 13. Dr. Cristea Sorin - Neurochirurgie
            Map<String, Object> medic13 = new HashMap<>();
            medic13.put("nume", "Cristea");
            medic13.put("prenume", "Sorin");
            medic13.put("specialitate", "Neurochirurgie");
            medic13.put("spital", "Spitalul Militar Galați");
            medic13.put("program", "Luni, Marți, Joi: 08:00-16:00");
            medic13.put("telefon", "0236 413 132");
            medic13.put("email", "sorin.cristea@spitalmilitar.ro");
            medic13.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor13.jpg");
            medic13.put("descriere", "Dr. Cristea este neurochirurg cu vasta experiență în tratamentul patologiei tumorale cerebrale și spinale. A efectuat numeroase intervenții chirurgicale complexe la nivelul sistemului nervos central.");

            mediciRef.push().setValue(medic13);

            // Medici la Spitalul Militar Bacău

            // 14. Dr. Vasilescu Ana - Psihiatrie
            Map<String, Object> medic14 = new HashMap<>();
            medic14.put("nume", "Vasilescu");
            medic14.put("prenume", "Ana");
            medic14.put("specialitate", "Psihiatrie");
            medic14.put("spital", "Spitalul Militar Bacău");
            medic14.put("program", "Marți-Vineri: 09:00-15:00");
            medic14.put("telefon", "0234 524 823");
            medic14.put("email", "ana.vasilescu@spitalmilitar.ro");
            medic14.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor14.jpg");
            medic14.put("descriere", "Dr. Vasilescu este specializată în diagnosticarea și tratamentul afecțiunilor psihiatrice. Are competențe în terapia tulburărilor anxioase și depresive, precum și în managementul stresului post-traumatic.");

            mediciRef.push().setValue(medic14);

            // 15. Dr. Olteanu Gabriel - Medicină Internă
            Map<String, Object> medic15 = new HashMap<>();
            medic15.put("nume", "Olteanu");
            medic15.put("prenume", "Gabriel");
            medic15.put("specialitate", "Medicină Internă");
            medic15.put("spital", "Spitalul Militar Central");
            medic15.put("program", "Luni-Vineri: 08:00-14:00");
            medic15.put("telefon", "021 319 3054");
            medic15.put("email", "gabriel.olteanu@scumc.ro");
            medic15.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor15.jpg");
            medic15.put("descriere", "Dr. Olteanu este specialist în medicină internă, cu expertiză în diagnosticarea și tratarea afecțiunilor metabolice și cardiovasculare. Are competențe în ecografie abdominală și ecocardiografie.");

            mediciRef.push().setValue(medic15);

            // 16. Dr. Ștefănescu Laura - Radiologie
            Map<String, Object> medic16 = new HashMap<>();
            medic16.put("nume", "Ștefănescu");
            medic16.put("prenume", "Laura");
            medic16.put("specialitate", "Radiologie");
            medic16.put("spital", "Spitalul Militar Constanța");
            medic16.put("program", "Luni-Joi: 08:00-14:00");
            medic16.put("telefon", "0241 660 392");
            medic16.put("email", "laura.stefanescu@spitalmilitar.ro");
            medic16.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor16.jpg");
            medic16.put("descriere", "Dr. Ștefănescu este specializată în diagnosticul imagistic prin TC, RMN și ecografie. Are expertiză în interpretarea imagisticii neurologice și abdominale.");

            mediciRef.push().setValue(medic16);

            // 17. Dr. Drăgan Victor - Oncologie
            Map<String, Object> medic17 = new HashMap<>();
            medic17.put("nume", "Drăgan");
            medic17.put("prenume", "Victor");
            medic17.put("specialitate", "Oncologie");
            medic17.put("spital", "Spitalul Militar Iași");
            medic17.put("program", "Luni, Miercuri, Vineri: 09:00-15:00");
            medic17.put("telefon", "0232 210 932");
            medic17.put("email", "victor.dragan@spitalmilitar.ro");
            medic17.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor17.jpg");
            medic17.put("descriere", "Dr. Drăgan este specializat în oncologie medicală, cu expertiză în tratamentul cancerului mamar și pulmonar. Utilizează protocoale moderne de chimioterapie și terapie țintită.");

            mediciRef.push().setValue(medic17);

            // 18. Dr. Marin Bogdan - Chirurgie Cardiacă
            Map<String, Object> medic18 = new HashMap<>();
            medic18.put("nume", "Marin");
            medic18.put("prenume", "Bogdan");
            medic18.put("specialitate", "Chirurgie Cardiacă");
            medic18.put("spital", "Spitalul Militar Central");
            medic18.put("program", "Luni, Marți, Joi: 08:00-16:00");
            medic18.put("telefon", "021 319 3055");
            medic18.put("email", "bogdan.marin@scumc.ro");
            medic18.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor18.jpg");
            medic18.put("descriere", "Dr. Marin este chirurg cardiac cu experiență în bypass coronarian și chirurgia valvulară. A efectuat numeroase intervenții complexe la nivel cardiac și vascular.");

            mediciRef.push().setValue(medic18);

            // 19. Dr. Popa Andreea - Diabet și Nutriție
            Map<String, Object> medic19 = new HashMap<>();
            medic19.put("nume", "Popa");
            medic19.put("prenume", "Andreea");
            medic19.put("specialitate", "Diabet și Nutriție");
            medic19.put("spital", "Spitalul Militar Cluj-Napoca");
            medic19.put("program", "Luni-Vineri: 08:00-14:00");
            medic19.put("telefon", "0264 598 384");
            medic19.put("email", "andreea.popa@spitalmilitar.ro");
            medic19.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor19.jpg");
            medic19.put("descriere", "Dr. Popa este specializată în managementul diabetului și al afecțiunilor metabolice. Oferă consultanță nutrițională personalizată și monitorizare pentru pacienții cu diabet zaharat.");

            mediciRef.push().setValue(medic19);

            // 20. Dr. Mihai Costin - Hematologie
            Map<String, Object> medic20 = new HashMap<>();
            medic20.put("nume", "Mihai");
            medic20.put("prenume", "Costin");
            medic20.put("specialitate", "Hematologie");
            medic20.put("spital", "Spitalul Militar Timișoara");
            medic20.put("program", "Marți-Vineri: 09:00-15:00");
            medic20.put("telefon", "0256 493 355");
            medic20.put("email", "costin.mihai@spitalmilitar.ro");
            medic20.put("imagine", "https://firebasestorage.googleapis.com/v0/b/geomed-app.appspot.com/o/medici%2Fdoctor20.jpg");
            medic20.put("descriere", "Dr. Mihai este specialist în diagnosticarea și tratarea afecțiunilor hematologice. Are expertiză în managementul anemiilor, leucemiilor și limfoamelor.");

            mediciRef.push().setValue(medic20);

            // Marchează datele ca fiind inițializate
            SharedPreferences.Editor editor = prefsApp.edit();
            editor.putBoolean("isDataInitialized", true);
            editor.apply();
            performMigration();
            Toast.makeText(this, "Baza de date a fost inițializată cu succes!", Toast.LENGTH_SHORT).show();
        }
    }
}