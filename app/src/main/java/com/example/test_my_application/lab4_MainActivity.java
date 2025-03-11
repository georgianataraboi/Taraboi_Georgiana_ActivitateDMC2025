package com.example.test_my_application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class lab4_MainActivity extends AppCompatActivity {

    private TextView textViewGarajDetalii;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lab4_main);

        textViewGarajDetalii = findViewById(R.id.textView2);
        Button button = findViewById(R.id.button5);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(lab4_MainActivity.this, lab4_1Activity.class);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            garajtransport garaj = data.getParcelableExtra("garaj");

            if (garaj != null) {

                String detalii = "Nume: " + garaj.getNume() +
                        "\nCapacitate: " + garaj.getCapacitate() +
                        "\nSuprafață: " + garaj.getSuprafata() + " mp" +
                        "\nDeschis Non-Stop: " + (garaj.isDeschisNonStop() ? "Da" : "Nu") +
                        "\nTip Garaj: " + garaj.getTipGaraj();

                textViewGarajDetalii.setText(detalii);
            }
        }
    }
}
