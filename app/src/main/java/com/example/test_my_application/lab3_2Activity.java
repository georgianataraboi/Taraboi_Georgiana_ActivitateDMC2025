package com.example.test_my_application;

import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class lab3_2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lab32);

        Button buttonOpenLab3_3 = findViewById(R.id.button2);

        buttonOpenLab3_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(lab3_2Activity.this, lab3_3Activity.class);

                Bundle bundle = new Bundle();
                bundle.putString("mesaj", "Salut din lab3_2Activity!");
                bundle.putInt("val1", 1);
                bundle.putInt("val2", 2);

                intent.putExtras(bundle);

                startActivityForResult(intent, 1);
            }
        });
    }

    // primim rezultatul
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

            String mesajReturn = data.getStringExtra("mesaj_return");
            int sumaReturnata = data.getIntExtra("suma", 0);

            Toast.makeText(this, "Mesaj: " + mesajReturn + "\nSuma: " + sumaReturnata, Toast.LENGTH_LONG).show();
//
//            Intent returnIntent = new Intent(this, lab3MainActivity.class);
//            returnIntent.putExtra("mesaj_final", mesajReturn);
//            returnIntent.putExtra("suma_finala", sumaReturnata);

//            startActivity(returnIntent);
        }
    }
}
