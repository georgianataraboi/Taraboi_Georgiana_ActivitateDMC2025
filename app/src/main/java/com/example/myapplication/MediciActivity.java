package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediciActivity extends AppCompatActivity {

    private RecyclerView mediciRecyclerView;
    private MediciAdapter mediciAdapter;
    private List<Medic> medicList;
    private List<Medic> filteredList;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Spinner spitalSpinner;
    private Spinner specialitateSpinner;
    private List<String> spitaleList;
    private List<String> specialitatiList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medici);

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        mediciRecyclerView = findViewById(R.id.medici_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        spitalSpinner = findViewById(R.id.spital_filter_spinner);
        specialitateSpinner = findViewById(R.id.specialitate_filter_spinner);

        // Configurează RecyclerView
        medicList = new ArrayList<>();
        filteredList = new ArrayList<>();
        mediciAdapter = new MediciAdapter(filteredList, this);
        mediciRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mediciRecyclerView.setAdapter(mediciAdapter);

        // Inițializează listele pentru filtre
        spitaleList = new ArrayList<>();
        specialitatiList = new ArrayList<>();

        // Adaugă opțiunea "Toate" la filtre
        spitaleList.add("Toate spitalele");
        specialitatiList.add("Toate specialitățile");

        // Configurează spinnerele
        ArrayAdapter<String> spitaleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spitaleList);
        spitaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spitalSpinner.setAdapter(spitaleAdapter);

        ArrayAdapter<String> specialitatiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, specialitatiList);
        specialitatiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        specialitateSpinner.setAdapter(specialitatiAdapter);

        // Încarcă filtrele și medicii
        loadSpitaleAndSpecialitati();
        loadMedici();

        // Configurează listenerii pentru filtre
        spitalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterMedici();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu faceți nimic
            }
        });

        specialitateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterMedici();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu faceți nimic
            }
        });
    }

    private void loadSpitaleAndSpecialitati() {
        mDatabase.child("medici").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> spitaleSet = new HashSet<>();
                Set<String> specialitatiSet = new HashSet<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        spitaleSet.add(medic.getSpital());
                        specialitatiSet.add(medic.getSpecialitate());
                    }
                }

                // Actualizează datele spinner-elor
                spitaleList.clear();
                specialitatiList.clear();

                spitaleList.add("Toate spitalele");
                specialitatiList.add("Toate specialitățile");

                spitaleList.addAll(spitaleSet);
                specialitatiList.addAll(specialitatiSet);

                ((ArrayAdapter) spitalSpinner.getAdapter()).notifyDataSetChanged();
                ((ArrayAdapter) specialitateSpinner.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MediciActivity.this,
                        "Eroare la încărcarea filtrelor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMedici() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("medici").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                medicList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        medicList.add(medic);
                    }
                }

                // Aplică filtrele curente
                filterMedici();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MediciActivity.this,
                        "Eroare la încărcarea medicilor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMedici() {
        String selectedSpital = spitalSpinner.getSelectedItem().toString();
        String selectedSpecialitate = specialitateSpinner.getSelectedItem().toString();

        filteredList.clear();

        for (Medic medic : medicList) {
            boolean spitalMatch = selectedSpital.equals("Toate spitalele") ||
                    medic.getSpital().equals(selectedSpital);
            boolean specialitateMatch = selectedSpecialitate.equals("Toate specialitățile") ||
                    medic.getSpecialitate().equals(selectedSpecialitate);

            if (spitalMatch && specialitateMatch) {
                filteredList.add(medic);
            }
        }

        mediciAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            mediciRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            mediciRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}