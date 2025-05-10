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

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MedicDetailActivity extends AppCompatActivity {

    private ImageView medicImageView;
    private TextView numeTv, specialitateTv, spitalTv, programTv, telefonTv, emailTv, descriereTv;
    private Button programareBtn, contactBtn;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private String medicId;
    private Medic currentMedic;

    // Adaugă DatabaseHelper și SyncManager
    private DatabaseHelper dbHelper;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medic_detail);

        // Obține ID-ul medicului din intent
        medicId = getIntent().getStringExtra("medicId");
        if (medicId == null) {
            Toast.makeText(this, "Eroare la încărcarea profilului", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inițializează Firebase și SQLite
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new SyncManager(this);

        // Configurează toolbar-ul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        medicImageView = findViewById(R.id.medic_image);
        numeTv = findViewById(R.id.medic_nume);
        specialitateTv = findViewById(R.id.medic_specialitate);
        spitalTv = findViewById(R.id.medic_spital);
        programTv = findViewById(R.id.medic_program);
        telefonTv = findViewById(R.id.medic_telefon);
        emailTv = findViewById(R.id.medic_email);
        descriereTv = findViewById(R.id.medic_descriere);
        programareBtn = findViewById(R.id.programare_btn);
        contactBtn = findViewById(R.id.contact_btn);
        progressBar = findViewById(R.id.progress_bar);

        // Încarcă detaliile medicului din SQLite mai întâi
        loadMedicFromSQLite();

        // Setează listenerii pentru butoane
        programareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMedic != null) {
                    Intent intent = new Intent(MedicDetailActivity.this, ProgramareActivity.class);
                    intent.putExtra("medicId", medicId);
                    intent.putExtra("medicNume", currentMedic.getNumeComplet());
                    intent.putExtra("spitalNume", currentMedic.getSpital());
                    intent.putExtra("specialitate", currentMedic.getSpecialitate());
                    startActivity(intent);
                }
            }
        });

        contactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMedic != null) {
                    showContactOptions();
                }
            }
        });

        // Adăugăm listeneri pentru telefon și email
        telefonTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMedic != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + currentMedic.getTelefon()));
                    startActivity(intent);
                }
            }
        });

        emailTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMedic != null) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + currentMedic.getEmail()));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Programare la " + currentMedic.getNumeComplet());
                    startActivity(intent);
                }
            }
        });
    }

    private void loadMedicFromSQLite() {
        progressBar.setVisibility(View.VISIBLE);

        // Încearcă să încarcă din SQLite
        currentMedic = dbHelper.getMedic(medicId);

        if (currentMedic != null) {
            // Afișează datele
            displayMedicDetails();

            // Sincronizează cu Firebase în background
            syncMedicWithFirebase();
        } else {
            // Dacă nu există în SQLite, încarcă din Firebase
            loadMedicFromFirebase();
        }
    }

    private void syncMedicWithFirebase() {
        // Sincronizare silențioasă în background
        mDatabase.child("medici").child(medicId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Medic updatedMedic = dataSnapshot.getValue(Medic.class);
                if (updatedMedic != null) {
                    updatedMedic.setId(medicId);

                    // Actualizează în SQLite
                    dbHelper.insertOrUpdateMedic(updatedMedic, true);

                    // Dacă datele s-au schimbat, actualizează afișajul
                    if (!isMedicDataSame(currentMedic, updatedMedic)) {
                        currentMedic = updatedMedic;
                        runOnUiThread(() -> displayMedicDetails());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Eșec la sincronizare - continuă cu datele din SQLite
            }
        });
    }

    private void loadMedicFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("medici").child(medicId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentMedic = dataSnapshot.getValue(Medic.class);
                if (currentMedic != null) {
                    currentMedic.setId(medicId);

                    // Salvează în SQLite pentru viitorul acces offline
                    dbHelper.insertOrUpdateMedic(currentMedic, true);

                    // Afișează datele
                    displayMedicDetails();
                } else {
                    Toast.makeText(MedicDetailActivity.this, "Medicul nu a fost găsit", Toast.LENGTH_SHORT).show();
                    finish();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MedicDetailActivity.this,
                        "Eroare la încărcarea profilului: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayMedicDetails() {
        if (currentMedic == null) return;

        numeTv.setText(currentMedic.getNumeComplet());
        specialitateTv.setText(currentMedic.getSpecialitate());
        spitalTv.setText(currentMedic.getSpital());
        programTv.setText(currentMedic.getProgram());
        telefonTv.setText(currentMedic.getTelefon());
        emailTv.setText(currentMedic.getEmail());
        descriereTv.setText(currentMedic.getDescriere());

        // Încarcă imaginea cu Glide
        if (currentMedic.getImagine() != null && !currentMedic.getImagine().isEmpty()) {
            Glide.with(this)
                    .load(currentMedic.getImagine())
                    .placeholder(R.drawable.ic_doctor)
                    .error(R.drawable.ic_doctor)
                    .into(medicImageView);
        } else {
            medicImageView.setImageResource(R.drawable.ic_doctor);
        }

        // Actualizează butonul de programare
        programareBtn.setText("Programare");

        progressBar.setVisibility(View.GONE);
    }

    private boolean isMedicDataSame(Medic medic1, Medic medic2) {
        if (medic1 == null || medic2 == null) return false;

        // Compară câmpurile importante
        return medic1.getNumeComplet().equals(medic2.getNumeComplet()) &&
                medic1.getSpecialitate().equals(medic2.getSpecialitate()) &&
                medic1.getSpital().equals(medic2.getSpital()) &&
                medic1.getProgram().equals(medic2.getProgram());
    }

    private void showContactOptions() {
        // Creează un dialog pentru opțiunile de contact
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Contactează medicul");

        // Adaugă opțiunile
        String[] options = {"Apel telefonic", "Email"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Telefon
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + currentMedic.getTelefon()));
                    startActivity(dialIntent);
                    break;
                case 1: // Email
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:" + currentMedic.getEmail()));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Programare " + currentMedic.getNumeComplet());
                    startActivity(emailIntent);
                    break;
            }
        });

        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}