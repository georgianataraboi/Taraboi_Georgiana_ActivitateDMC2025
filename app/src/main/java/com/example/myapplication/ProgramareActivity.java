package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProgramareActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner spitalSpinner;
    private Spinner specialitateSpinner;
    private Spinner medicSpinner;
    private Spinner oraSpinner;
    private Button submitButton;
    private DatabaseReference mDatabase;

    private List<String> spitaleList;
    private List<String> specialitatiList;
    private List<String> mediciList;
    private List<String> oreList;

    private Map<String, String> mediciIdMap;

    private String selectedDate;
    private String spitalId;
    private String medicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programare);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inițializează vizualizările
        calendarView = findViewById(R.id.calendar_view);
        spitalSpinner = findViewById(R.id.spital_spinner);
        specialitateSpinner = findViewById(R.id.specialitate_spinner);
        medicSpinner = findViewById(R.id.medic_spinner);
        oraSpinner = findViewById(R.id.ora_spinner);
        submitButton = findViewById(R.id.submit_button);

        // Inițializează listele
        spitaleList = new ArrayList<>();
        specialitatiList = new ArrayList<>();
        mediciList = new ArrayList<>();
        oreList = new ArrayList<>();
        mediciIdMap = new HashMap<>();

        // Configurează adaptoarele pentru spinner-e
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

        // Dacă avem date din intent, precompletează formular
        preloadDataFromIntent();

        // Setează data inițială
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());

        // Listener pentru calendar
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(calendar.getTime());

            // Actualizează orele disponibile în funcție de data selectată
            updateOreDisponibile();
        });

        // Încarcă spitalele
        loadSpitale();

        // Listener pentru spinner spitale
        spitalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < spitaleList.size()) {
                    String selectedSpital = spitaleList.get(position);
                    loadSpecialitati(selectedSpital);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu facem nimic
            }
        });

        // Listener pentru spinner specialități
        specialitateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spitaleList.isEmpty() || position < 0 || position >= specialitatiList.size()) return;

                String selectedSpital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
                String selectedSpecialitate = specialitatiList.get(position);
                loadMedici(selectedSpital, selectedSpecialitate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu facem nimic
            }
        });

        // Listener pentru spinner medici
        medicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateOreDisponibile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu facem nimic
            }
        });

        // Listener pentru butonul de submit
        submitButton.setOnClickListener(v -> saveProgramare());
    }

    private void preloadDataFromIntent() {
        // Verifică dacă avem date din intent (pentru precompletare)
        String spitalNume = getIntent().getStringExtra("spitalNume");
        String medicNume = getIntent().getStringExtra("medicNume");
        String specialitate = getIntent().getStringExtra("specialitate");

        if (spitalNume != null) {
            // Vom seta spinner-ul după ce datele sunt încărcate
            spitaleList.add(spitalNume);
        }

        if (specialitate != null) {
            specialitatiList.add(specialitate);
        }

        if (medicNume != null) {
            mediciList.add(medicNume);
        }
    }

    private void loadSpitale() {
        mDatabase.child("spitale").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                spitaleList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Spital spital = snapshot.getValue(Spital.class);
                    if (spital != null) {
                        spitaleList.add(spital.getNume());
                    }
                }

                // Notifică adapter-ul că datele s-au schimbat
                ((ArrayAdapter) spitalSpinner.getAdapter()).notifyDataSetChanged();

                // Verifică dacă avem date precompletate
                String preselectedSpital = getIntent().getStringExtra("spitalNume");
                if (preselectedSpital != null) {
                    int position = spitaleList.indexOf(preselectedSpital);
                    if (position >= 0) {
                        spitalSpinner.setSelection(position);
                    }
                }

                // Încarcă specialitățile pentru spitalul selectat
                if (!spitaleList.isEmpty()) {
                    String selectedSpital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
                    loadSpecialitati(selectedSpital);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProgramareActivity.this,
                        "Eroare la încărcarea spitalelor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSpecialitati(String spital) {
        // Obține toate specialitățile pentru spitalul selectat
        mDatabase.child("medici").orderByChild("spital").equalTo(spital)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        specialitatiList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Medic medic = snapshot.getValue(Medic.class);
                            if (medic != null && !specialitatiList.contains(medic.getSpecialitate())) {
                                specialitatiList.add(medic.getSpecialitate());
                            }
                        }

                        // Notifică adapter-ul că datele s-au schimbat
                        ((ArrayAdapter) specialitateSpinner.getAdapter()).notifyDataSetChanged();

                        // Verifică dacă avem date precompletate
                        String preselectedSpecialitate = getIntent().getStringExtra("specialitate");
                        if (preselectedSpecialitate != null) {
                            int position = specialitatiList.indexOf(preselectedSpecialitate);
                            if (position >= 0) {
                                specialitateSpinner.setSelection(position);
                            }
                        }

                        // Încarcă medicii pentru specialitatea selectată
                        if (!specialitatiList.isEmpty()) {
                            String selectedSpecialitate = specialitatiList.get(specialitateSpinner.getSelectedItemPosition());
                            loadMedici(spital, selectedSpecialitate);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ProgramareActivity.this,
                                "Eroare la încărcarea specialităților: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMedici(String spital, String specialitate) {
        // Obține toți medicii pentru spitalul și specialitatea selectate
        mDatabase.child("medici")
                .orderByChild("spital").equalTo(spital)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        mediciList.clear();
                        mediciIdMap.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Medic medic = snapshot.getValue(Medic.class);
                            if (medic != null && medic.getSpecialitate().equals(specialitate)) {
                                mediciList.add(medic.getNumeComplet());
                                mediciIdMap.put(medic.getNumeComplet(), snapshot.getKey());
                            }
                        }

                        // Notifică adapter-ul că datele s-au schimbat
                        ((ArrayAdapter) medicSpinner.getAdapter()).notifyDataSetChanged();

                        // Verifică dacă avem date precompletate
                        String preselectedMedic = getIntent().getStringExtra("medicNume");
                        if (preselectedMedic != null) {
                            int position = mediciList.indexOf(preselectedMedic);
                            if (position >= 0) {
                                medicSpinner.setSelection(position);
                            }
                        }

                        // Actualizează orele disponibile
                        updateOreDisponibile();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ProgramareActivity.this,
                                "Eroare la încărcarea medicilor: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateOreDisponibile() {
        // Generează intervale orare pentru programări (simplificat)
        oreList.clear();

        // Adăugă intervale orare standard între 8:00 și 16:00
        oreList.add("08:00 - 08:30");
        oreList.add("08:30 - 09:00");
        oreList.add("09:00 - 09:30");
        oreList.add("09:30 - 10:00");
        oreList.add("10:00 - 10:30");
        oreList.add("10:30 - 11:00");
        oreList.add("11:00 - 11:30");
        oreList.add("11:30 - 12:00");
        oreList.add("12:00 - 12:30");
        oreList.add("12:30 - 13:00");
        oreList.add("13:00 - 13:30");
        oreList.add("13:30 - 14:00");
        oreList.add("14:00 - 14:30");
        oreList.add("14:30 - 15:00");
        oreList.add("15:00 - 15:30");
        oreList.add("15:30 - 16:00");

        // Notifică adapter-ul că datele s-au schimbat
        if (oraSpinner.getAdapter() != null) {
            ((ArrayAdapter) oraSpinner.getAdapter()).notifyDataSetChanged();
        }
    }

    private void saveProgramare() {
        // Verifică dacă avem toate datele necesare
        if (spitaleList.isEmpty() || specialitatiList.isEmpty() ||
                mediciList.isEmpty() || oreList.isEmpty()) {
            Toast.makeText(this, "Lipsesc date necesare pentru programare", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifică selecțiile
        if (spitalSpinner.getSelectedItemPosition() < 0 ||
                specialitateSpinner.getSelectedItemPosition() < 0 ||
                medicSpinner.getSelectedItemPosition() < 0 ||
                oraSpinner.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Vă rugăm să selectați toate opțiunile", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obține valorile selectate
        String spital = spitaleList.get(spitalSpinner.getSelectedItemPosition());
        String specialitate = specialitatiList.get(specialitateSpinner.getSelectedItemPosition());
        String medic = mediciList.get(medicSpinner.getSelectedItemPosition());
        String ora = oreList.get(oraSpinner.getSelectedItemPosition());

        // Obține ID-ul medicului
        medicId = mediciIdMap.get(medic);

        // Folosește un ID generic pentru utilizator (într-o implementare completă, ar trebui să fie ID-ul utilizatorului autentificat)
        String userId = "guest_user";

        // Creează o nouă programare
        String programareId = mDatabase.child("programari").push().getKey();

        Map<String, Object> programare = new HashMap<>();
        programare.put("userId", userId);
        programare.put("spital", spital);
        programare.put("specialitate", specialitate);
        programare.put("medic", medic);
        programare.put("medicId", medicId);
        programare.put("data", selectedDate);
        programare.put("ora", ora);
        programare.put("status", "confirmată");
        programare.put("timestamp", System.currentTimeMillis());

        // Salvează programarea în Firebase
        mDatabase.child("programari").child(programareId).setValue(programare)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProgramareActivity.this,
                            "Programare realizată cu succes!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProgramareActivity.this,
                                "Eroare la salvarea programării: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}