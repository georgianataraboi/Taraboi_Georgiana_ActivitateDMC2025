// SpitalDetailActivity.java
package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SpitalDetailActivity extends AppCompatActivity {

    private ImageView spitalImageView;
    private TextView numeTv, adresaTv, telefonTv, websiteTv, descriereTv;
    private Button programareBtn, mapBtn;
    private RecyclerView mediciRecyclerView;
    private MediciAdapter mediciAdapter;
    private List<Medic> mediciList;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private String spitalId;
    private Spital currentSpital;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spital_detail);

        // Obține ID-ul spitalului din intent
        spitalId = getIntent().getStringExtra("spitalId");
        if (spitalId == null) {
            Toast.makeText(this, "Eroare la încărcarea detaliilor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        spitalImageView = findViewById(R.id.spital_image);
        numeTv = findViewById(R.id.spital_nume);
        adresaTv = findViewById(R.id.spital_adresa);
        telefonTv = findViewById(R.id.spital_telefon);
        websiteTv = findViewById(R.id.spital_website);
        descriereTv = findViewById(R.id.spital_descriere);
        programareBtn = findViewById(R.id.programare_btn);
        mapBtn = findViewById(R.id.map_btn);
        progressBar = findViewById(R.id.progress_bar);
        mediciRecyclerView = findViewById(R.id.medici_recycler_view);

        // Configurează RecyclerView
        mediciList = new ArrayList<>();
        mediciAdapter = new MediciAdapter(mediciList, this);
        mediciRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediciRecyclerView.setAdapter(mediciAdapter);

        // Încarcă detaliile spitalului
        loadSpitalDetails();

        // Setează listeneri pentru butoane
        programareBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SpitalDetailActivity.this, ProgramareActivity.class);
            intent.putExtra("spitalId", spitalId);
            intent.putExtra("spitalNume", currentSpital.getNume());
            startActivity(intent);
        });

        mapBtn.setOnClickListener(v -> {
            if (currentSpital != null && currentSpital.getLocatie() != null) {
                String[] latLng = currentSpital.getLocatie().split(",");
                if (latLng.length == 2) {
                    Uri gmmIntentUri = Uri.parse("geo:" + latLng[0] + "," + latLng[1] + "?q=" +
                            Uri.encode(currentSpital.getNume()));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(SpitalDetailActivity.this,
                                "Google Maps nu este instalat", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        websiteTv.setOnClickListener(v -> {
            if (currentSpital != null && currentSpital.getWebsite() != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(currentSpital.getWebsite()));
                startActivity(browserIntent);
            }
        });
    }

    private void loadSpitalDetails() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("spitale").child(spitalId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentSpital = dataSnapshot.getValue(Spital.class);
                if (currentSpital != null) {
                    // Setează titlul toolbar-ului
                    getSupportActionBar().setTitle(currentSpital.getNume());

                    // Populează vizualizările
                    numeTv.setText(currentSpital.getNume());
                    adresaTv.setText(currentSpital.getAdresa());
                    telefonTv.setText(currentSpital.getTelefon());
                    websiteTv.setText(currentSpital.getWebsite());
                    descriereTv.setText(currentSpital.getDescriere());

                    // Încarcă imaginea cu Glide
                    if (currentSpital.getImagine() != null && !currentSpital.getImagine().isEmpty()) {
                        Glide.with(SpitalDetailActivity.this)
                                .load(currentSpital.getImagine())
                                .placeholder(R.drawable.ic_hospital)
                                .error(R.drawable.ic_hospital)
                                .into(spitalImageView);
                    } else {
                        spitalImageView.setImageResource(R.drawable.ic_hospital);
                    }

                    // Încarcă medicii pentru acest spital
                    loadMediciForSpital(currentSpital.getNume());
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SpitalDetailActivity.this,
                        "Eroare la încărcarea detaliilor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMediciForSpital(String spitalNume) {
        Query query = mDatabase.child("medici").orderByChild("spital").equalTo(spitalNume);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mediciList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        mediciList.add(medic);
                    }
                }
                mediciAdapter.notifyDataSetChanged();

                // Afișează/ascunde mesajul "niciun medic"
                if (mediciList.isEmpty()) {
                    findViewById(R.id.no_medici_text).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.no_medici_text).setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SpitalDetailActivity.this,
                        "Eroare la încărcarea medicilor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}