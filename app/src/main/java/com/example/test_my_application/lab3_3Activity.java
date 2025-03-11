package com.example.test_my_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class lab3_3Activity extends AppCompatActivity {

    private static final String TAG = "lab3_3Activity";
    private int valoare1, valoare2, suma;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab33);

//        TextView textView = findViewById(R.id.textView2);
        Button butonTrimiteInapoi=findViewById(R.id.button3);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String mesaj = extras.getString("mesaj");
            valoare1 = extras.getInt("val1");
            valoare2 = extras.getInt("val2");
            suma=valoare1+valoare2;
//            String textAfisat = "Mesaj: " + mesaj + "\nValoare 1: " + valoare1 + "\nValoare 2: " + valoare2;
//            textView.setText(textAfisat);

            Log.d(TAG, "Mesaj primit: " + mesaj);
            Log.d(TAG, "Valoare 1: " + valoare1);
            Log.d(TAG, "Valoare 2: " + valoare2);

            String toastMessage = "Mesaj: " + mesaj + " | Valoare1: " + valoare1 + " | Valoare2: " + valoare2;
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        }
        butonTrimiteInapoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent= new Intent();
                returnIntent.putExtra("mesaj_return", "Mesaj primit și procesat!");
                returnIntent.putExtra("suma", suma);

                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
