package com.example.lab_google;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private List<Marker> markers = new ArrayList<>();
    private Polyline currentPolyline;
    private Button btnConnectMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button btnOpenTulceaMap = findViewById(R.id.btn_open_tulcea_map);
        btnOpenTulceaMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        btnConnectMarkers = findViewById(R.id.btn_connect_markers);
        btnConnectMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectMarkersWithPolyline();
            }
        });
        btnConnectMarkers.setEnabled(false);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verifică permisiunile de locație
        checkLocationPermission();

        // Configurează harta
        setupMap();
    }

    private void setupMap() {

        LatLng galati = new LatLng(45.4353, 28.0080);

        // Deplasează camera la Galați și setează zoom-ul
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(galati, 12));

        // Adaugă un marker pentru Galați
//        mMap.addMarker(new MarkerOptions()
//                .position(galati)
//                .title("Galați"));

        // Setează listener pentru click-uri pe hartă
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                addMarker(latLng);
            }
        });
    }

    private void addMarker(LatLng position) {
        // Adaugă un marker nou la poziția dată
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title("Pin " + (markers.size() + 1)));

        if (marker != null) {
            markers.add(marker);

            // Activează butonul de conectare a markerilor doar dacă există cel puțin 2 markeri
            btnConnectMarkers.setEnabled(markers.size() >= 2);

            Toast.makeText(this, "Marker adăugat: " + position.latitude + ", " + position.longitude,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void connectMarkersWithPolyline() {
        if (markers.size() < 2) {
            Toast.makeText(this, "Adăugați cel puțin 2 markeri pentru a crea o polilinie",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Șterge polilinia existentă, dacă există
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        List<LatLng> points = new ArrayList<>();
        for (Marker marker : markers) {
            points.add(marker.getPosition());
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(5)
                .color(ContextCompat.getColor(this, R.color.colorPolyline));

        currentPolyline = mMap.addPolyline(polylineOptions);

        Toast.makeText(this, "Markerii au fost conectați", Toast.LENGTH_SHORT).show();
    }

    private void checkLocationPermission() {
        // Verifică dacă avem permisiunea
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Dacă permisiunea a fost refuzată anterior și utilizatorul ar trebui să vadă o explicație
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Afișează un dialog explicativ înainte de a cere permisiunea din nou
                new AlertDialog.Builder(this)
                        .setTitle("Permisiune de locație necesară")
                        .setMessage("Această aplicație necesită permisiunea de locație pentru a funcționa corect. Vă rugăm să acordați permisiunea.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Solicită permisiunea după ce utilizatorul a citit explicația
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                // Solicită permisiunea direct dacă nu este nevoie de explicație
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            // Avem deja permisiunea, activăm butonul "My Location"
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Permisiune refuzată", Toast.LENGTH_SHORT).show();
            }
        }
    }
}