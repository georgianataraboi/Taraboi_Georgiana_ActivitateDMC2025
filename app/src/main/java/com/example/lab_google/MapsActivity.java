package com.example.lab_google;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapsActivity";
    private static final double POLYGON_RADIUS_METERS = 500; // Raza pentru generarea punctelor poligonului

    // Coordonatele pentru Tulcea
    private static final LatLng TULCEA_LOCATION = new LatLng(45.1733, 28.7967);

    private GoogleMap mMap;
    private LocationManager locationManager;
    private String drawingType = "polygon"; // Valoare implicită
    private LatLng currentLocation;
    private List<LatLng> polygonPoints = new ArrayList<>();
    private Polygon currentPolygon;
    private Marker centerMarker;
    private Button btnDrawShape, btnClear;
    private RadioGroup radioGroup;
    private boolean isDrawingEnabled = false;
    private boolean useRealLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Inițializare componente UI
        radioGroup = findViewById(R.id.radio_group);
        btnDrawShape = findViewById(R.id.btn_draw_shape);
        btnClear = findViewById(R.id.btn_clear);

        // Setare ascultători pentru butoane
        btnDrawShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dacă locația curentă nu este disponibilă sau este în afara României, folosește Tulcea
                LatLng locationToUse = getLocationToUse();

                if ("polygon".equals(drawingType)) {
                    generatePolygonAroundLocation(locationToUse, POLYGON_RADIUS_METERS);
                } else if ("circle".equals(drawingType)) {
                    drawCircle(locationToUse, POLYGON_RADIUS_METERS);
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDrawing();
            }
        });

        // Ascultător pentru schimbarea tipului de desen
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_polygon) {
                    drawingType = "polygon";
                } else if (checkedId == R.id.radio_circle) {
                    drawingType = "circle";
                }
                clearDrawing();
            }
        });

        // Obține fragmentul hărții și notifică când harta este gata de utilizare
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inițializare LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Activează butonul de desenare (va folosi locația implicită Tulcea dacă locația reală nu e disponibilă)
        btnDrawShape.setEnabled(true);
    }

    private LatLng getLocationToUse() {
        // Dacă nu avem locația curentă sau este în Paris sau altă locație în afara României, folosim Tulcea
        if (currentLocation == null || isLocationOutsideRomania(currentLocation)) {
            Toast.makeText(this, "Se folosește locația Tulcea în loc de locația simulată", Toast.LENGTH_SHORT).show();
            return TULCEA_LOCATION;
        }

        return currentLocation;
    }

    // Verifică dacă locația este în afara României (verificare simplificată)
    private boolean isLocationOutsideRomania(LatLng location) {
        // Coordonate aproximative pentru verificarea apartenenței la România
        double minLat = 43.6, maxLat = 48.3;
        double minLng = 20.2, maxLng = 30.0;

        boolean isOutside = location.latitude < minLat || location.latitude > maxLat ||
                location.longitude < minLng || location.longitude > maxLng;

        if (isOutside) {
            Log.d(TAG, "Locația detectată este în afara României: " + location.latitude + ", " + location.longitude);
        }

        return isOutside;
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
        // Deplasează camera la Tulcea și setează zoom-ul
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TULCEA_LOCATION, 13));

        // Adaugă un marker pentru Tulcea
        mMap.addMarker(new MarkerOptions()
                .position(TULCEA_LOCATION)
                .title("Tulcea"));

        // Activează butonul de locație curentă dacă permisiunea este acordată
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // Generează un poligon regulat în jurul locației date
    private void generatePolygonAroundLocation(LatLng center, double radiusMeters) {

        polygonPoints.clear();

        int numPoints = 6; // Hexagon

        for (int i = 0; i < numPoints; i++) {
            double angle = Math.PI * 2 * i / numPoints;
            double x = center.latitude + (radiusMeters / 111320.0) * Math.cos(angle);
            double y = center.longitude + (radiusMeters / (111320.0 * Math.cos(center.latitude * Math.PI / 180))) * Math.sin(angle);
            LatLng point = new LatLng(x, y);
            polygonPoints.add(point);
        }


        polygonPoints.add(polygonPoints.get(0));

        drawPolygon(center);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15));

        Toast.makeText(this, "Poligon generat în jurul locației selectate", Toast.LENGTH_SHORT).show();
    }

    private void drawPolygon(LatLng center) {
        if (currentPolygon != null) {
            currentPolygon.remove();
        }

        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(TULCEA_LOCATION)
                .title("Tulcea"));

        mMap.addMarker(new MarkerOptions()
                .position(center)
                .title("Centrul formei"));

        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(polygonPoints)
                .strokeColor(Color.RED)
                .fillColor(Color.argb(75, 255, 0, 0)); // Semi-transparent

        currentPolygon = mMap.addPolygon(polygonOptions);

        // Adaugă markeri pentru fiecare vârf al poligonului
        for (int i = 0; i < polygonPoints.size() - 1; i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(polygonPoints.get(i))
                    .title("Vârf " + (i + 1)));
        }
    }

    private void drawCircle(LatLng center, double radiusMeters) {

        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(TULCEA_LOCATION)
                .title("Tulcea"));

        centerMarker = mMap.addMarker(new MarkerOptions()
                .position(center)
                .title("Centrul cercului"));

        mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(75, 0, 0, 255))
                .strokeWidth(2));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15));

        Toast.makeText(this, "Cerc desenat cu raza de " + radiusMeters + " metri", Toast.LENGTH_SHORT).show();
    }

    private void clearDrawing() {
        polygonPoints.clear();
        mMap.clear();

        if (currentPolygon != null) {
            currentPolygon.remove();
            currentPolygon = null;
        }

        if (centerMarker != null) {
            centerMarker.remove();
            centerMarker = null;
        }

        mMap.addMarker(new MarkerOptions()
                .position(TULCEA_LOCATION)
                .title("Tulcea"));

        isDrawingEnabled = true;

        Toast.makeText(this, "Desen șters", Toast.LENGTH_SHORT).show();
    }

    private void checkLocationPermission() {
        Log.d(TAG, "Verificăm permisiunile de locație");

        // Verificăm starea actuală a permisiunilor
        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "ACCESS_FINE_LOCATION: " + (hasFineLocation ? "GRANTED" : "DENIED"));
        Log.d(TAG, "ACCESS_COARSE_LOCATION: " + (hasCoarseLocation ? "GRANTED" : "DENIED"));

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
                                ActivityCompat.requestPermissions(MapsActivity.this,
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

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                startLocationUpdates();
            }
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,  // Interval minim de timp între actualizări (milisecunde)
                    10,    // Distanța minimă între actualizări (metri)
                    this);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                onLocationChanged(lastKnownLocation);
            } else {
                // Dacă nu avem locație, setăm Tulcea ca locație implicită
                Log.d(TAG, "Nu s-a putut obține locația curentă, se folosește Tulcea");
                currentLocation = TULCEA_LOCATION;
            }

            // Activează desenarea
            isDrawingEnabled = true;
        } catch (Exception e) {
            Log.e(TAG, "Eroare la solicitarea actualizărilor de locație: " + e.getMessage());
            Toast.makeText(this, "Eroare la obținerea locației, se folosește Tulcea", Toast.LENGTH_SHORT).show();
            // Setăm Tulcea ca locație implicită
            currentLocation = TULCEA_LOCATION;
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Actualizează locația curentă
        LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d(TAG, "Locație actualizată: " + newLocation.latitude + ", " + newLocation.longitude);

        // Verificați dacă locația este în România
        if (isLocationOutsideRomania(newLocation)) {
            Toast.makeText(this, "Locație detectată în afara României, se va folosi Tulcea", Toast.LENGTH_SHORT).show();
            // Nu actualizăm currentLocation, vom folosi Tulcea
        } else {
            // Locația este în România, o actualizăm
            currentLocation = newLocation;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisiune acordată
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
                    Toast.makeText(this, "Permisiune acordată. Puteți utiliza funcțiile de locație.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Permisiune refuzată
                Toast.makeText(this, "Permisiune refuzată. Se va folosi locația Tulcea.", Toast.LENGTH_SHORT).show();
                // Setează Tulcea ca locație implicită
                currentLocation = TULCEA_LOCATION;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationManager != null && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}