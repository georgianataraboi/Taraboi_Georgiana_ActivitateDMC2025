package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MediciActivity extends AppCompatActivity {

    private RecyclerView mediciRecyclerView;
    private MediciGridAdapter mediciAdapter;
    private List<Medic> mediciList;
    private List<Medic> filteredList;
    private ProgressBar progressBar;
    private TextView emptyView, titleTv;
    private EditText searchEditText;
    private ImageView clearSearchButton;
    private DatabaseReference mDatabase;
    private String spitalId;
    private String spitalNume;

    // Adaugă DatabaseHelper și SyncManager
    private DatabaseHelper dbHelper;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medici);

        // Obține ID-ul spitalului din intent
        spitalId = getIntent().getStringExtra("spitalId");
        spitalNume = getIntent().getStringExtra("spitalNume");

        if (spitalNume == null) {
            spitalNume = "Medici";
        }

        // Inițializează Firebase și SQLite
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new SyncManager(this);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Medici " + spitalNume);

        // Inițializează vizualizările
        mediciRecyclerView = findViewById(R.id.medici_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        titleTv = findViewById(R.id.title_text);
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.clear_search);

        // Setează titlul
        titleTv.setText("Toți medicii de la " + spitalNume);

        // Configurează RecyclerView cu grid layout pentru medici
        mediciList = new ArrayList<>();
        filteredList = new ArrayList<>();
        mediciAdapter = new MediciGridAdapter(filteredList, this);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        mediciRecyclerView.setLayoutManager(gridLayoutManager);
        mediciRecyclerView.setHasFixedSize(true);
        mediciRecyclerView.setAdapter(mediciAdapter);

        // Configurează căutarea
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Afișează sau ascunde butonul de ștergere
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // Filtrează lista de medici
                filterMedici(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurează butonul de ștergere
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            filterMedici("");
        });

        // Încarcă medicii din SQLite mai întâi
        loadMediciFromSQLite();
    }

    private void loadMediciFromSQLite() {
        progressBar.setVisibility(View.VISIBLE);

        // Încarcă medicii din SQLite
        List<Medic> mediciFromSQLite;

        if (spitalNume != null && !spitalNume.equals("Medici")) {
            // Încarcă medicii pentru un spital specific
            mediciFromSQLite = dbHelper.getMediciForSpital(spitalNume);
        } else {
            // Încarcă toți medicii
            mediciFromSQLite = dbHelper.getAllMedici();
        }

        if (!mediciFromSQLite.isEmpty()) {
            // Afișează datele din SQLite
            mediciList.clear();
            mediciList.addAll(mediciFromSQLite);
            filterMedici(searchEditText.getText().toString());
            progressBar.setVisibility(View.GONE);

            // Sincronizează cu Firebase în background
            syncMediciWithFirebase();
        } else {
            // Dacă SQLite este gol, încarcă din Firebase
            loadMediciFromFirebase();
        }
    }

    private void syncMediciWithFirebase() {
        // Sincronizare silențioasă în background
        Query query;
        if (spitalId != null) {
            query = mDatabase.child("medici").orderByChild("spital").equalTo(spitalNume);
        } else {
            query = mDatabase.child("medici");
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Medic> updatedMedici = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        updatedMedici.add(medic);

                        // Actualizează în SQLite
                        dbHelper.insertOrUpdateMedic(medic, true);
                    }
                }

                // Doar actualizează afișajul dacă lista s-a schimbat semnificativ
                if (isMediciListDifferent(mediciList, updatedMedici)) {
                    runOnUiThread(() -> {
                        mediciList.clear();
                        mediciList.addAll(updatedMedici);
                        filterMedici(searchEditText.getText().toString());
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Silent fail - continue with SQLite data
            }
        });
    }

    private void loadMediciFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        // Construiește query-ul în funcție de spitalId
        Query query;
        if (spitalId != null) {
            query = mDatabase.child("medici").orderByChild("spital").equalTo(spitalNume);
        } else {
            query = mDatabase.child("medici");
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mediciList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        mediciList.add(medic);

                        // Salvează în SQLite pentru accesul offline
                        dbHelper.insertOrUpdateMedic(medic, true);
                    }
                }

                // Inițializează lista filtrată cu toți medicii
                filteredList.clear();
                filteredList.addAll(mediciList);
                mediciAdapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);

                // Actualizează vizibilitatea pentru lista goală
                updateEmptyView();
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

    private boolean isMediciListDifferent(List<Medic> list1, List<Medic> list2) {
        if (list1.size() != list2.size()) return true;

        // Verifică dacă s-au schimbat ID-urile medicilor
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).getId().equals(list2.get(i).getId())) {
                return true;
            }
        }

        return false;
    }

    private void filterMedici(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(mediciList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Medic medic : mediciList) {
                if (medic.getNume().toLowerCase().contains(lowerCaseQuery) ||
                        medic.getPrenume().toLowerCase().contains(lowerCaseQuery) ||
                        (medic.getSpecialitate() != null && medic.getSpecialitate().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(medic);
                }
            }
        }

        mediciAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            mediciRecyclerView.setVisibility(View.GONE);

            // Mesaj în funcție de căutare
            String searchText = searchEditText.getText().toString();
            if (!searchText.isEmpty()) {
                emptyView.setText("Nu am găsit medici care să corespundă căutării tale");
            } else {
                emptyView.setText("Nu există medici disponibili");
            }
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