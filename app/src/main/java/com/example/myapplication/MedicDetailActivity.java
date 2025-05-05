// MedicDetailActivity.java
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

        // Inițializează Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

        // Încarcă detaliile medicului
        loadMedicDetails();

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
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Programare la " + currentMedic.getNumeComplet());
                    startActivity(emailIntent);
                    break;
            }
        });

        builder.show();
    }

    private void loadMedicDetails() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("medici").child(medicId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentMedic = dataSnapshot.getValue(Medic.class);
                if (currentMedic != null) {
                    // Setează titlul toolbar-ului
                    getSupportActionBar().setTitle(currentMedic.getNumeComplet());

                    // Populează vizualizările
                    numeTv.setText(currentMedic.getNumeComplet());
                    specialitateTv.setText(currentMedic.getSpecialitate());
                    spitalTv.setText(currentMedic.getSpital());
                    programTv.setText(currentMedic.getProgram());
                    telefonTv.setText(currentMedic.getTelefon());
                    emailTv.setText(currentMedic.getEmail());
                    descriereTv.setText(currentMedic.getDescriere());

                    // Încarcă imaginea cu Glide
                    if (currentMedic.getImagine() != null && !currentMedic.getImagine().isEmpty()) {
                        Glide.with(MedicDetailActivity.this)
                                .load(currentMedic.getImagine())
                                .placeholder(R.drawable.ic_doctor)
                                .error(R.drawable.ic_doctor)
                                .into(medicImageView);
                    } else {
                        medicImageView.setImageResource(R.drawable.ic_doctor);
                    }

                    // Actualizează butonul de programare cu spitalul
                    programareBtn.setText("Programare la " + currentMedic.getSpital());
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}