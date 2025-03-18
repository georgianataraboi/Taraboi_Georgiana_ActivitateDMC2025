package com.example.test_my_application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class lab4_MainActivity extends AppCompatActivity {

    private ListView listViewGaraje;
    private List<garajtransport> listaGaraje;
    private ArrayAdapter<garajtransport> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab4_main);

        listViewGaraje = findViewById(R.id.listViewGaraje);
        Button button = findViewById(R.id.button5);

        listaGaraje = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaGaraje);
        listViewGaraje.setAdapter(adapter);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(lab4_MainActivity.this, lab4_1Activity.class);
            startActivityForResult(intent, 1);
        });

        listViewGaraje.setOnItemClickListener((parent, view, position, id) -> {
            Toast.makeText(this, listaGaraje.get(position).toString(), Toast.LENGTH_LONG).show();
        });

        listViewGaraje.setOnItemLongClickListener((parent, view, position, id) -> {
            listaGaraje.remove(position);
            adapter.notifyDataSetChanged();
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            garajtransport garaj = data.getParcelableExtra("garaj");

            if (garaj != null) {
                listaGaraje.add(garaj);
                adapter.notifyDataSetChanged();
            }
        }
    }
}
