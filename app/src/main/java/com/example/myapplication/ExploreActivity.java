package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private RecyclerView spitaleRecyclerView;
    private RecyclerView mediciRecyclerView;
    private SpitaleAdapter spitaleAdapter;
    private MediciAdapter mediciAdapter;
    private List<Spital> spitalList;
    private List<Medic> medicList;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private CardView overviewCardView;
    private TextView statisticsText;
    private Button viewAllSpitaleBtn, viewAllMediciBtn;

    // Adaugă DatabaseHelper și SyncManager
    private DatabaseHelper dbHelper;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Explorează rețeaua");

        // Inițializează Firebase și SQLite
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new SyncManager(this);

        // Inițializează vizualizările
        spitaleRecyclerView = findViewById(R.id.spitale_recycler_view);
        mediciRecyclerView = findViewById(R.id.medici_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        overviewCardView = findViewById(R.id.overview_card);
        statisticsText = findViewById(R.id.statistics_text);
        viewAllSpitaleBtn = findViewById(R.id.view_all_spitale_btn);
        viewAllMediciBtn = findViewById(R.id.view_all_medici_btn);

        // Configurează RecyclerView pentru spitale
        spitalList = new ArrayList<>();
        spitaleAdapter = new SpitaleAdapter(spitalList, this);
        spitaleRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        spitaleRecyclerView.setAdapter(spitaleAdapter);

        // Configurează RecyclerView pentru medici
        medicList = new ArrayList<>();
        mediciAdapter = new MediciAdapter(medicList, this);
        mediciRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediciRecyclerView.setAdapter(mediciAdapter);

        // Încarcă datele din SQLite mai întâi
        loadDataFromSQLite();

        // Apoi încearcă să actualizezi din Firebase
        syncDataFromFirebase();

        // Configurează butoanele pentru vizualizare completă
        viewAllSpitaleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExploreActivity.this, SpitaleActivity.class);
                startActivity(intent);
            }
        });

        viewAllMediciBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExploreActivity.this, MediciActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadDataFromSQLite() {
        progressBar.setVisibility(View.VISIBLE);

        // Încarcă datele din SQLite
        List<Spital> allSpitale = dbHelper.getAllSpitale();
        List<Medic> allMedici = getAllMedici(); // Trebuie să adaugi această metodă în DatabaseHelper

        // Actualizează statisticile
        String stats = "Rețeaua GeoMed include " + allSpitale.size() + " spitale militare și " +
                allMedici.size() + " medici specialiști din diverse domenii medicale.";
        statisticsText.setText(stats);

        // Afișează primele 5 spitale
        spitalList.clear();
        for (int i = 0; i < Math.min(5, allSpitale.size()); i++) {
            spitalList.add(allSpitale.get(i));
        }
        spitaleAdapter.notifyDataSetChanged();

        // Afișează primii 5 medici
        medicList.clear();
        for (int i = 0; i < Math.min(5, allMedici.size()); i++) {
            medicList.add(allMedici.get(i));
        }
        mediciAdapter.notifyDataSetChanged();

        progressBar.setVisibility(View.GONE);

        // Dacă nu există date în SQLite, încearcă Firebase
        if (allSpitale.isEmpty() || allMedici.isEmpty()) {
            loadDataFromFirebase();
        }
    }

    private void syncDataFromFirebase() {
        // Sincronizare în background
        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                // Nu arătăm nimic, sincronizarea este în background
            }

            @Override
            public void onSyncProgress(String message) {
                // Opțional: poți actualiza un indicator subtil de sincronizare
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                if (success) {
                    runOnUiThread(() -> {
                        // Reîncarcă datele după sincronizare
                        loadDataFromSQLite();
                    });
                }
            }
        });

        // Pornește sincronizarea
        syncManager.syncFromFirebase();
    }

    private void loadDataFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        // Încarcă direct din Firebase ca fallback
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Numără spitalele și medicii
                long spitaleCount = dataSnapshot.child("spitale").getChildrenCount();
                long mediciCount = dataSnapshot.child("medici").getChildrenCount();

                // Actualizează statisticile
                String stats = "Rețeaua GeoMed include " + spitaleCount + " spitale militare și " +
                        mediciCount + " medici specialiști din diverse domenii medicale.";
                statisticsText.setText(stats);

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ExploreActivity.this, "Eroare la încărcarea datelor", Toast.LENGTH_SHORT).show();
            }
        });

        // Încarcă spitalele (limitat la 5) din Firebase
        mDatabase.child("spitale").limitToFirst(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                spitalList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Spital spital = snapshot.getValue(Spital.class);
                    if (spital != null) {
                        spital.setId(snapshot.getKey());
                        spitalList.add(spital);

                        // Salvează în SQLite
                        dbHelper.insertOrUpdateSpital(spital, true);
                    }
                }

                spitaleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ExploreActivity.this, "Eroare la încărcarea spitalelor", Toast.LENGTH_SHORT).show();
            }
        });

        // Încarcă medicii (limitat la 5) din Firebase
        mDatabase.child("medici").limitToFirst(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                medicList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        medicList.add(medic);

                        // Salvează în SQLite
                        dbHelper.insertOrUpdateMedic(medic, true);
                    }
                }

                mediciAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ExploreActivity.this, "Eroare la încărcarea medicilor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Adaugă această metodă pentru a obține toți medicii din SQLite
    private List<Medic> getAllMedici() {
        List<Medic> medici = new ArrayList<>();
        // Trebuie să adaugi metoda getAllMedici() în DatabaseHelper
        // sau să folosești getMediciForSpital(null) adaptat pentru toți medicii
        return medici;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}