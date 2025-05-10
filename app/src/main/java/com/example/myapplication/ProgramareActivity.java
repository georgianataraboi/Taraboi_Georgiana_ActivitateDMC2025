package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProgramareActivity extends AppCompatActivity {

    private static final String TAG = "ProgramareActivity";
    private static final String FIXED_USER_ID = "incerc";

    private CalendarView calendarView;
    private Spinner spitalSpinner;
    private Spinner specialitateSpinner;
    private Spinner medicSpinner;
    private Spinner oraSpinner;
    private Button submitButton;
    private ProgressBar loadingIndicator;
    private TextView statusMessage;
    private DatabaseReference firebaseRef;

    private List<String> spitaleList;
    private List<String> specialitatiList;
    private List<String> mediciList;
    private List<String> oreList;

    private Map<String, String> mediciIdMap;

    private String selectedDate;
    private String medicId;

    // Fixed current date for consistency
    private Calendar fixedCurrentDate;
    private String fixedCurrentDateStr;

    // Flag to prevent multiple submissions
    private boolean isSubmitting = false;

    // Optimized sync components
    private DatabaseHelper dbHelper;
    private OptimizedSyncManager optimizedSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_programare);
            Log.d(TAG, "ProgramareActivity onCreate started");

            // Configure toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }

            // Initialize Firebase and database helpers
            firebaseRef = FirebaseDatabase.getInstance().getReference();
            dbHelper = DatabaseHelper.getInstance(this);
            optimizedSyncManager = new OptimizedSyncManager(this);

            // Initialize views
            initializeViews();

            // Initialize lists
            initializeLists();

            // Configure spinners adapters
            configureSpinnerAdapters();

            // Initialize dates
            initializeDates();

            // Setup calendar
            setupCalendar();

            // Setup spinner listeners
            setupSpinnerListeners();

            // Preload data from intent
            preloadDataFromIntent();

            // Submit button listener
            submitButton.setOnClickListener(v -> {
                updateStatusMessage("", View.GONE);
                if (!isSubmitting) {
                    saveProgramare();
                }
            });

            // Load initial data with smart sync
            loadSpitale();

            Log.d(TAG, "onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendar_view);
        spitalSpinner = findViewById(R.id.spital_spinner);
        specialitateSpinner = findViewById(R.id.specialitate_spinner);
        medicSpinner = findViewById(R.id.medic_spinner);
        oraSpinner = findViewById(R.id.ora_spinner);
        submitButton = findViewById(R.id.submit_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        statusMessage = findViewById(R.id.status_message);

        if (calendarView == null) {
            throw new RuntimeException("Calendar view not found - check activity_programare.xml");
        }
    }

    private void initializeLists() {
        spitaleList = new ArrayList<>();
        specialitatiList = new ArrayList<>();
        mediciList = new ArrayList<>();
        oreList = new ArrayList<>();
        mediciIdMap = new HashMap<>();
    }

    private void configureSpinnerAdapters() {
        ArrayAdapter<String> spitaleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spitaleList);
        spitaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spitalSpinner.setAdapter(spitaleAdapter);

        ArrayAdapter<String> specialitatiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, specialitatiList);
        specialitatiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        specialitateSpinner.setAdapter(specialitatiAdapter);

        ArrayAdapter<String> mediciAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mediciList);
        mediciAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medicSpinner.setAdapter(mediciAdapter);

        ArrayAdapter<String> oreAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, oreList);
        oreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        oraSpinner.setAdapter(oreAdapter);
    }

    private void initializeDates() {
        fixedCurrentDate = Calendar.getInstance();
        fixedCurrentDate.set(Calendar.HOUR_OF_DAY, 0);
        fixedCurrentDate.set(Calendar.MINUTE, 0);
        fixedCurrentDate.set(Calendar.SECOND, 0);
        fixedCurrentDate.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fixedCurrentDateStr = sdf.format(fixedCurrentDate.getTime());
        selectedDate = fixedCurrentDateStr;
    }

    private void setupCalendar() {
        Log.d(TAG, "Setting up calendar");

        Calendar minDate = (Calendar) fixedCurrentDate.clone();
        calendarView.setMinDate(minDate.getTimeInMillis());
        calendarView.setDate(minDate.getTimeInMillis(), true, true);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Log.d(TAG, "Date changed: " + year + "-" + (month + 1) + "-" + dayOfMonth);

            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            Calendar today = (Calendar) fixedCurrentDate.clone();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (selectedCalendar.before(today)) {
                Log.d(TAG, "User attempted to select a past date");
                Toast.makeText(this, "Nu poți selecta o dată din trecut", Toast.LENGTH_SHORT).show();
                calendarView.setDate(today.getTimeInMillis(), true, true);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = sdf.format(today.getTime());
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = sdf.format(selectedCalendar.getTime());
                Log.d(TAG, "Selected date: " + selectedDate);
                updateStatusMessage("", View.GONE);
                updateOreDisponibile();
            }
        });
    }

    private void setupSpinnerListeners() {
        spitalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < spitaleList.size()) {
                    String selectedSpital = spitaleList.get(position);
                    loadSpecialitati(selectedSpital);
                    updateStatusMessage("", View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        specialitateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spitaleList.isEmpty() || position < 0 || position >= specialitatiList.size())
                    return;

                String selectedSpital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
                String selectedSpecialitate = specialitatiList.get(position);
                loadMedici(selectedSpital, selectedSpecialitate);
                updateStatusMessage("", View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        medicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStatusMessage("", View.GONE);
                updateOreDisponibile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        oraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStatusMessage("", View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Force calendar display refresh
        if (calendarView != null) {
            calendarView.postDelayed(() -> {
                long currentDate = calendarView.getDate();
                calendarView.setDate(currentDate, true, true);
            }, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }

    private void updateStatusMessage(String message, int visibility) {
        if (statusMessage != null) {
            statusMessage.setText(message);
            statusMessage.setVisibility(visibility);
        }
    }

    private boolean isDateValid(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar currentCalendar = (Calendar) fixedCurrentDate.clone();
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
            currentCalendar.set(Calendar.MINUTE, 0);
            currentCalendar.set(Calendar.SECOND, 0);
            currentCalendar.set(Calendar.MILLISECOND, 0);
            Date currentDate = currentCalendar.getTime();
            Log.d(TAG, "Checking date: " + dateStr + " against current date: " + sdf.format(currentDate));
            return date != null && (date.after(currentDate) || date.equals(currentDate));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date", e);
            return false;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void preloadDataFromIntent() {
        try {
            String spitalNume = getIntent().getStringExtra("spitalNume");
            String medicNume = getIntent().getStringExtra("medicNume");
            String specialitate = getIntent().getStringExtra("specialitate");

            if (spitalNume != null && !spitalNume.isEmpty()) {
                spitaleList.add(spitalNume);
            }
            if (specialitate != null && !specialitate.isEmpty()) {
                specialitatiList.add(specialitate);
            }
            if (medicNume != null && !medicNume.isEmpty()) {
                mediciList.add(medicNume);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preloading data from intent: " + e.getMessage(), e);
        }
    }


    private void loadSpitale() {
        optimizedSyncManager.loadSpitale(new OptimizedSyncManager.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                populateSpitaleUI();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading spitale: " + error);
                // Still populate UI with SQLite data if Firebase fails
                populateSpitaleUI();
            }

            @Override
            public void onDataAvailable(String data) {
                Log.d(TAG, "Data available: " + data);
            }
        });
    }

    private void populateSpitaleUI() {
        new Thread(() -> {
            List<Spital> spitale = dbHelper.getAllSpitale();

            runOnUiThread(() -> {
                spitaleList.clear();
                for (Spital spital : spitale) {
                    if (spital != null && spital.getNume() != null) {
                        spitaleList.add(spital.getNume());
                    }
                }

                if (spitalSpinner.getAdapter() != null) {
                    ((ArrayAdapter<?>) spitalSpinner.getAdapter()).notifyDataSetChanged();
                }

                String preselectedSpital = getIntent().getStringExtra("spitalNume");
                if (preselectedSpital != null && !spitaleList.isEmpty()) {
                    int position = spitaleList.indexOf(preselectedSpital);
                    if (position >= 0) {
                        spitalSpinner.setSelection(position);
                    }
                }
            });
        }).start();
    }

    private void loadSpecialitati(String spital) {
        if (spital == null || spital.isEmpty()) return;

        optimizedSyncManager.loadMediciForSpital(spital, new OptimizedSyncManager.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                populateSpecialitatiUI(spital);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading medici for spital: " + error);
                // Still populate UI with SQLite data if Firebase fails
                populateSpecialitatiUI(spital);
            }

            @Override
            public void onDataAvailable(String data) {
                Log.d(TAG, "Data available: " + data);
            }
        });
    }

    private void populateSpecialitatiUI(String spital) {
        new Thread(() -> {
            List<Medic> medici = dbHelper.getMediciForSpital(spital);

            runOnUiThread(() -> {
                specialitatiList.clear();
                for (Medic medic : medici) {
                    if (medic != null && medic.getSpecialitate() != null &&
                            !specialitatiList.contains(medic.getSpecialitate())) {
                        specialitatiList.add(medic.getSpecialitate());
                    }
                }

                if (specialitateSpinner.getAdapter() != null) {
                    ((ArrayAdapter<?>) specialitateSpinner.getAdapter()).notifyDataSetChanged();
                }

                String preselectedSpecialitate = getIntent().getStringExtra("specialitate");
                if (preselectedSpecialitate != null && !specialitatiList.isEmpty()) {
                    int position = specialitatiList.indexOf(preselectedSpecialitate);
                    if (position >= 0) {
                        specialitateSpinner.setSelection(position);
                    }
                }
            });
        }).start();
    }

    private void loadMedici(String spital, String specialitate) {
        if (spital == null || spital.isEmpty() || specialitate == null || specialitate.isEmpty()) return;

        new Thread(() -> {
            List<Medic> medici = dbHelper.getMediciForSpital(spital);

            runOnUiThread(() -> {
                mediciList.clear();
                mediciIdMap.clear();

                for (Medic medic : medici) {
                    if (medic != null && medic.getSpecialitate() != null &&
                            medic.getSpecialitate().equals(specialitate)) {
                        String numeComplet = getNumeComplet(medic);
                        mediciList.add(numeComplet);
                        mediciIdMap.put(numeComplet, medic.getId());
                    }
                }

                if (medicSpinner.getAdapter() != null) {
                    ((ArrayAdapter<?>) medicSpinner.getAdapter()).notifyDataSetChanged();
                }

                String preselectedMedic = getIntent().getStringExtra("medicNume");
                if (preselectedMedic != null && !mediciList.isEmpty()) {
                    int position = mediciList.indexOf(preselectedMedic);
                    if (position >= 0) {
                        medicSpinner.setSelection(position);
                    }
                }

                updateOreDisponibile();
            });
        }).start();
    }

    private String getNumeComplet(Medic medic) {
        String numeComplet = medic.getNumeComplet();
        if (numeComplet == null || numeComplet.isEmpty()) {
            if (medic.getPrenume() != null && medic.getNume() != null) {
                numeComplet = medic.getPrenume() + " " + medic.getNume();
            } else if (medic.getNume() != null) {
                numeComplet = medic.getNume();
            } else if (medic.getPrenume() != null) {
                numeComplet = medic.getPrenume();
            } else {
                numeComplet = "Unknown Doctor";
            }
        }
        return numeComplet;
    }

    private void updateOreDisponibile() {
        if (selectedDate == null || !isDateValid(selectedDate)) {
            oreList.clear();
            if (oraSpinner.getAdapter() != null) {
                ((ArrayAdapter<?>) oraSpinner.getAdapter()).notifyDataSetChanged();
            }
            updateStatusMessage("Nu poți selecta ore pentru o dată trecută", View.VISIBLE);
            return;
        }

        if (mediciList.isEmpty() || medicSpinner.getSelectedItemPosition() < 0 ||
                spitaleList.isEmpty() || spitalSpinner.getSelectedItemPosition() < 0) {
            List<String> allTimeSlots = generateTimeSlots();
            oreList.clear();
            oreList.addAll(allTimeSlots);
            if (oraSpinner.getAdapter() != null) {
                ((ArrayAdapter<?>) oraSpinner.getAdapter()).notifyDataSetChanged();
            }
            return;
        }

        String selectedMedic = mediciList.get(medicSpinner.getSelectedItemPosition());
        String selectedSpital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
        String medicIdSelected = mediciIdMap.get(selectedMedic);

        showLoading(true);

        // This method needs to be implemented in OptimizedSyncManager if it doesn't exist
        // For now, let's use a different approach
        checkAvailabilityWithNewCallback(medicIdSelected, selectedDate, selectedSpital);
    }

    private void checkAvailabilityWithNewCallback(String medicId, String date, String spital) {
        new Thread(() -> {
            // Get programari from database
            List<Map<String, Object>> programari = dbHelper.getProgramariForDate(medicId, date, spital);

            runOnUiThread(() -> {
                showLoading(false);
                updateAvailableTimeSlots(programari);
            });

            // Optionally sync from Firebase in background
            // This would need to be implemented in OptimizedSyncManager
        }).start();
    }
    private void updateAvailableTimeSlots(List<Map<String, Object>> programari) {
        List<String> allTimeSlots = generateTimeSlots();

        Set<String> bookedSlots = new HashSet<>();
        for (Map<String, Object> programare : programari) {
            String ora = (String) programare.get("ora");
            if (ora != null) {
                bookedSlots.add(ora);
            }
        }

        boolean isToday = selectedDate.equals(fixedCurrentDateStr);
        int currentHour = fixedCurrentDate.get(Calendar.HOUR_OF_DAY);
        int currentMinute = fixedCurrentDate.get(Calendar.MINUTE);

        oreList.clear();
        for (String slot : allTimeSlots) {
            if (bookedSlots.contains(slot)) {
                continue;
            }

            if (isToday) {
                String[] timeParts = slot.split(" - ")[0].split(":");
                int slotHour = Integer.parseInt(timeParts[0]);
                int slotMinute = Integer.parseInt(timeParts[1]);

                if (slotHour < currentHour ||
                        (slotHour == currentHour && slotMinute <= currentMinute + 30)) {
                    continue;
                }
            }

            oreList.add(slot);
        }

        if (oraSpinner.getAdapter() != null) {
            ((ArrayAdapter<?>) oraSpinner.getAdapter()).notifyDataSetChanged();
        }

        if (oreList.isEmpty()) {
            updateStatusMessage("Nu există intervale orare disponibile pentru data, medicul și spitalul selectate", View.VISIBLE);
        }
    }

    private List<String> generateTimeSlots() {
        List<String> allTimeSlots = new ArrayList<>();
        allTimeSlots.add("08:00 - 08:30");
        allTimeSlots.add("08:30 - 09:00");
        allTimeSlots.add("09:00 - 09:30");
        allTimeSlots.add("09:30 - 10:00");
        allTimeSlots.add("10:00 - 10:30");
        allTimeSlots.add("10:30 - 11:00");
        allTimeSlots.add("11:00 - 11:30");
        allTimeSlots.add("11:30 - 12:00");
        allTimeSlots.add("12:00 - 12:30");
        allTimeSlots.add("12:30 - 13:00");
        allTimeSlots.add("13:00 - 13:30");
        allTimeSlots.add("13:30 - 14:00");
        allTimeSlots.add("14:00 - 14:30");
        allTimeSlots.add("14:30 - 15:00");
        allTimeSlots.add("15:00 - 15:30");
        allTimeSlots.add("15:30 - 16:00");
        return allTimeSlots;
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (submitButton != null) {
            submitButton.setEnabled(!show);
        }
    }

    private void saveProgramare() {
        try {
            isSubmitting = true;
            showLoading(true);

            if (!isDateValid(selectedDate)) {
                showLoading(false);
                isSubmitting = false;
                Toast.makeText(this, "Nu poți face programări în trecut! Te rugăm să selectezi altă dată.", Toast.LENGTH_LONG).show();

                Calendar today = (Calendar) fixedCurrentDate.clone();
                calendarView.setDate(today.getTimeInMillis(), true, true);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = sdf.format(today.getTime());

                updateOreDisponibile();
                return;
            }

            if (spitaleList.isEmpty() || specialitatiList.isEmpty() ||
                    mediciList.isEmpty() || oreList.isEmpty()) {
                showLoading(false);
                isSubmitting = false;
                Toast.makeText(this, "Lipsesc date necesare pentru programare", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spitalSpinner.getSelectedItemPosition() < 0 ||
                    specialitateSpinner.getSelectedItemPosition() < 0 ||
                    medicSpinner.getSelectedItemPosition() < 0 ||
                    oraSpinner.getSelectedItemPosition() < 0) {
                showLoading(false);
                isSubmitting = false;
                Toast.makeText(this, "Vă rugăm să selectați toate opțiunile", Toast.LENGTH_SHORT).show();
                return;
            }

            final String spital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
            final String specialitate = specialitatiList.get(specialitateSpinner.getSelectedItemPosition());
            final String medic = mediciList.get(medicSpinner.getSelectedItemPosition());
            final String ora = oreList.get(oraSpinner.getSelectedItemPosition());
            medicId = mediciIdMap.get(medic);

            completeAppointmentBooking(spital, specialitate, medic, ora);
        } catch (Exception e) {
            Log.e(TAG, "Error saving appointment: " + e.getMessage(), e);
            showLoading(false);
            isSubmitting = false;
            Toast.makeText(this, "Eroare la salvarea programării: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void completeAppointmentBooking(String spital, String specialitate, String medic, String ora) {
        try {
            String programareId = firebaseRef.child("programari").push().getKey();
            if (programareId == null) {
                throw new Exception("Nu s-a putut genera un ID valid pentru programare");
            }

            final Map<String, Object> programare = new HashMap<>();
            programare.put("id", programareId);
            programare.put("userId", FIXED_USER_ID);
            programare.put("spital", spital);
            programare.put("specialitate", specialitate);
            programare.put("medic", medic);
            programare.put("medicId", medicId);
            programare.put("data", selectedDate);
            programare.put("ora", ora);
            programare.put("status", "confirmată");
            programare.put("timestamp", System.currentTimeMillis());

            optimizedSyncManager.saveProgramare(programare, new OptimizedSyncManager.SaveCallback() {
                @Override
                public void onSavedLocally() {
                    showLoading(false);
                    isSubmitting = false;
                    updateStatusMessage("Programare salvată!", View.VISIBLE);

                    submitButton.postDelayed(() -> finish(), 2000);
                }

                @Override
                public void onSavedToFirebase(boolean success) {
                    if (success) {
                        Log.d(TAG, "Successfully synced to Firebase");
                    } else {
                        Log.w(TAG, "Firebase sync failed, will retry later");
                    }
                }

                @Override
                public void onWillRetryLater() {
                    Toast.makeText(ProgramareActivity.this,
                            "Programarea va fi sincronizată automat când conexiunea devine disponibilă",
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    isSubmitting = false;
                    Toast.makeText(ProgramareActivity.this,
                            "Eroare la salvarea programării: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error completing appointment booking: " + e.getMessage(), e);
            showLoading(false);
            isSubmitting = false;
            Toast.makeText(this, "Eroare la finalizarea programării: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}