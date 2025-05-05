// SpitaleActivity.java
package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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

public class SpitaleActivity extends AppCompatActivity {

    private RecyclerView spitaleRecyclerView;
    private SpitaleAdapter spitaleAdapter;
    private List<Spital> spitalList;
    private List<Spital> filteredList;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spitale);

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        spitaleRecyclerView = findViewById(R.id.spitale_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        searchEditText = findViewById(R.id.search_edit_text);

        // Configurează RecyclerView
        spitalList = new ArrayList<>();
        filteredList = new ArrayList<>();
        spitaleAdapter = new SpitaleAdapter(filteredList, this);
        spitaleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        spitaleRecyclerView.setAdapter(spitaleAdapter);

        // Încarcă spitalele din Firebase
        loadSpitale();

        // Configurează căutarea
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nu este necesar
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filtrează lista la schimbarea textului
                filterSpitale(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nu este necesar
            }
        });
    }

    private void loadSpitale() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("spitale").addValueEventListener(new ValueEventListener() {
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

                // Inițializează lista filtrată cu toate spitalele
                filteredList.clear();
                filteredList.addAll(spitalList);
                spitaleAdapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);

                updateEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SpitaleActivity.this,
                        "Eroare la încărcarea spitalelor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterSpitale(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(spitalList);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (Spital spital : spitalList) {
                if (spital.getNume().toLowerCase().contains(lowerCaseQuery) ||
                        spital.getAdresa().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(spital);
                }
            }
        }

        spitaleAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            spitaleRecyclerView.setVisibility(View.GONE);
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
}