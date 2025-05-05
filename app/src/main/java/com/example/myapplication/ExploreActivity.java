// ExploreActivity.java
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Explorează rețeaua");

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

        // Încarcă datele
        loadData();

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

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        // Obține statistici despre spitale și medici
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
            }
        });

        // Încarcă spitalele (limitat la 5)
        mDatabase.child("spitale").limitToFirst(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                spitalList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Spital spital = snapshot.getValue(Spital.class);
                    if (spital != null) {
                        spital.setId(snapshot.getKey());
                        spitalList.add(spital);
                    }
                }

                spitaleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Gestionează eroarea
            }
        });

        // Încarcă medicii (limitat la 5)
        mDatabase.child("medici").limitToFirst(5).addValueEventListener(new ValueEventListener() {
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

                mediciAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Gestionează eroarea
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}