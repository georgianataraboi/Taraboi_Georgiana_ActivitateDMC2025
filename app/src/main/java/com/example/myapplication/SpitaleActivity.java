package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.List;

public class SpitaleActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView spitaleRecyclerView;
    private SpitaleAdapter spitaleAdapter;
    private List<Spital> spitalList;
    private List<Spital> filteredList;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private TextView emptyView, resultsText;
    private EditText searchEditText;
    private ImageView clearSearchButton;
    private Button btnAll, btnBucuresti, btnCluj, btnTimisoara;
    private String currentFilter = ""; // Filtru pentru orașe

    // Add SQLite support
    private DatabaseHelper dbHelper;
    private OptimizedSyncManager syncManager;
    private ValueEventListener firebaseListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spitale);

        // Inițializează Firebase și SQLite
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new OptimizedSyncManager(this);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        initializeViews();

        // Configurează RecyclerView
        setupRecyclerView();

        // Setează listeneri pentru butoane
        setupButtonListeners();

        // Configurează căutarea
        setupSearch();

        // Încarcă spitalele cu strategie offline-first
        loadSpitale();
    }

    private void initializeViews() {
        spitaleRecyclerView = findViewById(R.id.spitale_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.clear_search);
        resultsText = findViewById(R.id.results_text);

        // Inițializează butoanele de filtrare
        btnAll = findViewById(R.id.btn_all);
        btnBucuresti = findViewById(R.id.btn_bucuresti);
        btnCluj = findViewById(R.id.btn_cluj);
        btnTimisoara = findViewById(R.id.btn_timisoara);
    }

    private void setupRecyclerView() {
        spitalList = new ArrayList<>();
        filteredList = new ArrayList<>();
        spitaleAdapter = new SpitaleAdapter(filteredList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        spitaleRecyclerView.setLayoutManager(layoutManager);
        // Activează optimizările pentru RecyclerView
        spitaleRecyclerView.setHasFixedSize(true);
        spitaleRecyclerView.setItemViewCacheSize(20);
        spitaleRecyclerView.setAdapter(spitaleAdapter);
    }

    private void setupButtonListeners() {
        btnAll.setOnClickListener(this);
        btnBucuresti.setOnClickListener(this);
        btnCluj.setOnClickListener(this);
        btnTimisoara.setOnClickListener(this);

        // Adaugă acțiune pentru butonul de ștergere
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            applyFilters("", currentFilter);
        });
    }

    private void setupSearch() {
        // Configurează căutarea cu delay pentru a evita căutări multiple
        searchEditText.addTextChangedListener(new TextWatcher() {
            private long lastTextEdit = 0;
            private static final long DELAY = 300; // milisecunde

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nu este necesar
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastTextEdit = System.currentTimeMillis();

                // Arată/ascunde butonul de ștergere
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                spitaleRecyclerView.postDelayed(() -> {
                    if (System.currentTimeMillis() - lastTextEdit >= DELAY) {
                        // Filtrează lista doar dacă utilizatorul a încetat să tasteze
                        applyFilters(s.toString(), currentFilter);
                    }
                }, DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nu este necesar
            }
        });

        // Adaugă acțiune de căutare când se apasă "Search" pe tastatură
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters(searchEditText.getText().toString(), currentFilter);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_all) {
            currentFilter = "";
            updateButtonStyles(btnAll);
            resultsText.setText("Toate spitalele militare din România");
        } else if (id == R.id.btn_bucuresti) {
            currentFilter = "București";
            updateButtonStyles(btnBucuresti);
            resultsText.setText("Spitale militare din București");
        } else if (id == R.id.btn_cluj) {
            currentFilter = "Cluj";
            updateButtonStyles(btnCluj);
            resultsText.setText("Spitale militare din Cluj");
        } else if (id == R.id.btn_timisoara) {
            currentFilter = "Timișoara";
            updateButtonStyles(btnTimisoara);
            resultsText.setText("Spitale militare din Timișoara");
        }

        applyFilters(searchEditText.getText().toString(), currentFilter);
    }

    private void updateButtonStyles(Button selectedButton) {
        // Resetează stilul tuturor butoanelor
        btnAll.setBackgroundResource(R.drawable.button_secondary);
        btnAll.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        btnBucuresti.setBackgroundResource(R.drawable.button_secondary);
        btnBucuresti.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        btnCluj.setBackgroundResource(R.drawable.button_secondary);
        btnCluj.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        btnTimisoara.setBackgroundResource(R.drawable.button_secondary);
        btnTimisoara.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        // Aplică stilul pentru butonul selectat
        selectedButton.setBackgroundResource(R.drawable.button_primary);
        selectedButton.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void loadSpitale() {
        progressBar.setVisibility(View.VISIBLE);

        // First, load from SQLite for immediate response
        new Thread(() -> {
            List<Spital> spitaleFromSQLite = dbHelper.getAllSpitale();

            runOnUiThread(() -> {
                if (!spitaleFromSQLite.isEmpty()) {
                    updateSpitalList(spitaleFromSQLite);
                    progressBar.setVisibility(View.GONE);
                }
            });

            // Then sync with Firebase in background
            syncSpitaleFromFirebase();
        }).start();
    }

    private void syncSpitaleFromFirebase() {
        // Remove any existing listener to prevent duplicates
        if (firebaseListener != null) {
            mDatabase.child("spitale").removeEventListener(firebaseListener);
        }

        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                new Thread(() -> {
                    List<Spital> spitaleFromFirebase = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Spital spital = snapshot.getValue(Spital.class);
                        if (spital != null) {
                            spital.setId(snapshot.getKey());
                            spitaleFromFirebase.add(spital);

                            // Save to SQLite for offline access
                            dbHelper.insertOrUpdateSpital(spital, true);
                        }
                    }

                    runOnUiThread(() -> {
                        updateSpitalList(spitaleFromFirebase);
                        progressBar.setVisibility(View.GONE);
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Only show error if we don't have any data from SQLite
                    if (spitalList.isEmpty()) {
                        Toast.makeText(SpitaleActivity.this,
                                "Eroare la încărcarea spitalelor: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        mDatabase.child("spitale").addValueEventListener(firebaseListener);
    }

    private void updateSpitalList(List<Spital> spitale) {
        spitalList.clear();
        spitalList.addAll(spitale);

        // Apply current filters
        applyFilters(searchEditText.getText().toString(), currentFilter);
    }

    private void applyFilters(String query, String cityFilter) {
        filteredList.clear();

        for (Spital spital : spitalList) {
            boolean matchesSearch = query.isEmpty() ||
                    spital.getNume().toLowerCase().contains(query.toLowerCase().trim()) ||
                    spital.getAdresa().toLowerCase().contains(query.toLowerCase().trim());

            boolean matchesCity = cityFilter.isEmpty() ||
                    spital.getAdresa().contains(cityFilter);

            if (matchesSearch && matchesCity) {
                filteredList.add(spital);
            }
        }

        spitaleAdapter.notifyDataSetChanged();
        updateEmptyView();

        // Actualizează textul cu numărul de rezultate
        if (filteredList.size() > 0) {
            if (cityFilter.isEmpty() && query.isEmpty()) {
                resultsText.setText("Toate spitalele militare din România");
            } else if (!cityFilter.isEmpty() && query.isEmpty()) {
                resultsText.setText("Spitale militare din " + cityFilter);
            } else if (cityFilter.isEmpty() && !query.isEmpty()) {
                resultsText.setText(filteredList.size() + " spitale găsite pentru \"" + query + "\"");
            } else {
                resultsText.setText(filteredList.size() + " spitale găsite în " + cityFilter);
            }
        }
    }

    private void updateEmptyView() {
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            spitaleRecyclerView.setVisibility(View.GONE);

            // Update empty message based on current state
            String message = "";
            if (!spitalList.isEmpty() && (!searchEditText.getText().toString().isEmpty() || !currentFilter.isEmpty())) {
                message = "Nu există spitale care să corespundă criteriilor de căutare.";
            } else if (spitalList.isEmpty()) {
                message = "Se încarcă spitalele...";
            } else {
                message = "Nu există spitale disponibile.";
            }
            emptyView.setText(message);
        } else {
            emptyView.setVisibility(View.GONE);
            spitaleRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove Firebase listener to prevent memory leaks
        if (firebaseListener != null && mDatabase != null) {
            mDatabase.child("spitale").removeEventListener(firebaseListener);
            firebaseListener = null;
        }

        // Clean up sync manager if needed
        if (syncManager != null) {
//            syncManager.cleanup();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh data from SQLite when returning to activity
        new Thread(() -> {
            List<Spital> spitaleFromSQLite = dbHelper.getAllSpitale();

            runOnUiThread(() -> {
                if (!spitaleFromSQLite.isEmpty()) {
                    updateSpitalList(spitaleFromSQLite);
                }
            });
        }).start();
    }
}