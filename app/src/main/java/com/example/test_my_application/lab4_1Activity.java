package com.example.test_my_application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class lab4_1Activity extends AppCompatActivity {

    private EditText editTextNume, editTextCapacitate;
    private TextView textViewSuprafata;
    private CheckBox checkBoxNonStop;
    private Spinner spinnerTipGaraj;
    private Button buttonSubmit;

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

        double suprafataCalculata = 1200.5;
        textViewSuprafata.setText("Suprafață: " + suprafataCalculata + " mp");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.tip_garaj_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipGaraj.setAdapter(adapter);


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String nume = editTextNume.getText().toString().trim();
                int capacitate = Integer.parseInt(editTextCapacitate.getText().toString());
                boolean deschisNonStop = checkBoxNonStop.isChecked();
                String tipGarajStr = spinnerTipGaraj.getSelectedItem().toString();

                TipGaraj tipGaraj;
                try {
                    tipGaraj = TipGaraj.valueOf(tipGarajStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    tipGaraj = TipGaraj.PUBLIC;
                }


                garajtransport garaj = new garajtransport(nume, capacitate, suprafataCalculata, deschisNonStop, tipGaraj);


                Intent intent = new Intent();
                intent.putExtra("garaj", garaj);

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
