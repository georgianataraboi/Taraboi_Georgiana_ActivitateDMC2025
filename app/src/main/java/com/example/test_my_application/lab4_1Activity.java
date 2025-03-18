package com.example.test_my_application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.app.TimePickerDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.time.LocalTime;
public class lab4_1Activity extends AppCompatActivity {

    private EditText editTextNume, editTextCapacitate;
    private TextView textViewSuprafata;
    private CheckBox checkBoxNonStop;
    private Spinner spinnerTipGaraj;
    private Button buttonSubmit;

    private TimePicker timePickerOraDeschidere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab41);

        editTextCapacitate = findViewById(R.id.editTextText2);
        editTextNume = findViewById(R.id.editTextText3);
        textViewSuprafata = findViewById(R.id.textView3);
        checkBoxNonStop = findViewById(R.id.checkBox);
        spinnerTipGaraj = findViewById(R.id.spinner);
        buttonSubmit = findViewById(R.id.button4);

        Button buttonAlegeOra = findViewById(R.id.buttonAlegeOra);
        TextView textViewOraDeschidere = findViewById(R.id.textViewOraDeschidere);

        buttonAlegeOra.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    lab4_1Activity.this,
                    (view, hourOfDay, minute) -> {
                        String oraSelectata = String.format("%02d:%02d", hourOfDay, minute);
                        textViewOraDeschidere.setText("Ora deschidere: " + oraSelectata);
                    },
                    12, 0, true); // Setează ora inițială la 12:00
            timePickerDialog.show();
        });



        double suprafataCalculata = 1200.5;
        textViewSuprafata.setText("Suprafață: " + suprafataCalculata + " mp");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.tip_garaj_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipGaraj.setAdapter(adapter);

        // Ascunde/afișează TimePicker în funcție de CheckBox
        checkBoxNonStop.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timePickerOraDeschidere.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        buttonSubmit.setOnClickListener(v -> {
            String nume = editTextNume.getText().toString().trim();
            String capacitateStr = editTextCapacitate.getText().toString().trim();

            if (nume.isEmpty()) {
                editTextNume.setError("Introduceți numele!");
                return;
            }
            if (capacitateStr.isEmpty()) {
                editTextCapacitate.setError("Introduceți capacitatea!");
                return;
            }

            int capacitate = Integer.parseInt(capacitateStr);
            boolean deschisNonStop = checkBoxNonStop.isChecked();
            String tipGarajStr = spinnerTipGaraj.getSelectedItem().toString();
            TipGaraj tipGaraj = TipGaraj.valueOf(tipGarajStr.toUpperCase());

            String oraDeschidereStr = textViewOraDeschidere.getText().toString().replace("Ora deschidere: ", "");
            LocalTime oraDeschidere;
            try {
                oraDeschidere = deschisNonStop ? LocalTime.of(0, 0) : LocalTime.parse(oraDeschidereStr);
            } catch (Exception e) {
                oraDeschidere = LocalTime.of(0, 0); // Dacă ora nu e setată, default la 00:00
            }

            garajtransport garaj = new garajtransport(nume, capacitate, 1200.5, deschisNonStop, tipGaraj, oraDeschidere);

            Intent intent = new Intent();
            intent.putExtra("garaj", garaj);
            setResult(RESULT_OK, intent);
            finish();
        });

    }
}