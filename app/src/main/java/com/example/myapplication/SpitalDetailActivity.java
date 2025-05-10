package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;

public class SpitalDetailActivity extends AppCompatActivity {

    private ImageView spitalImageView;
    private TextView numeTv, adresaTv, telefonTv, descriereTv, veziTotiMediciiTv;
    private LinearLayout programareBtn, mapBtn, websiteContainer;
    private RecyclerView mediciRecyclerView;
    private MediciAdapter mediciAdapter;
    private List<Medic> mediciList;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private String spitalId;
    private Spital currentSpital;
    private MaterialToolbar toolbar;

    private LinearLayout weatherContainer;
    private CardView weatherCard;
    private TextView weatherTempTv, weatherDescTv, weatherDetailsTv, weatherTimeTv;
    private ImageView weatherIconIv;
    private static final String ACCUWEATHER_API_KEY = "Q2wI1AYYH5rfckTCuoAs1H3LhnozI3Vn";

    // Add SQLite support
    private DatabaseHelper dbHelper;
    private OptimizedSyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spital_detail);

        // Obține ID-ul spitalului din intent
        spitalId = getIntent().getStringExtra("spitalId");
        if (spitalId == null) {
            Toast.makeText(this, "Eroare la încărcarea detaliilor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inițializează Firebase și SQLite
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new OptimizedSyncManager(this);

        // Configurează toolbar-ul
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inițializează vizualizările
        initializeViews();

        // Configurează RecyclerView pentru medici cu scroll orizontal
        setupRecyclerView();

        // Încarcă detaliile spitalului
        loadSpitalDetails();

        // Setează listeneri pentru butoane
        setupButtonListeners();
    }

    private void initializeViews() {
        spitalImageView = findViewById(R.id.spital_image);
        numeTv = findViewById(R.id.spital_nume);
        adresaTv = findViewById(R.id.spital_adresa);
        telefonTv = findViewById(R.id.spital_telefon);
        descriereTv = findViewById(R.id.spital_descriere);
        programareBtn = findViewById(R.id.programare_btn);
        mapBtn = findViewById(R.id.map_btn);
        progressBar = findViewById(R.id.progress_bar);
        mediciRecyclerView = findViewById(R.id.medici_recycler_view);
        veziTotiMediciiTv = findViewById(R.id.vezi_toti_medicii);
        websiteContainer = findViewById(R.id.website_container);

        // Inițializează view-urile pentru vreme
        weatherCard = findViewById(R.id.weather_card);
        weatherContainer = findViewById(R.id.weather_container);
        weatherTempTv = findViewById(R.id.weather_temp);
        weatherDescTv = findViewById(R.id.weather_description);
        weatherDetailsTv = findViewById(R.id.weather_details);
        weatherTimeTv = findViewById(R.id.weather_time);
        weatherIconIv = findViewById(R.id.weather_icon);
    }

    private void setupRecyclerView() {
        mediciList = new ArrayList<>();
        mediciAdapter = new MediciAdapter(mediciList, this);

        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        mediciRecyclerView.setLayoutManager(horizontalLayoutManager);
        mediciRecyclerView.setHasFixedSize(true);
        mediciRecyclerView.setAdapter(mediciAdapter);
    }

    private void setupButtonListeners() {
        programareBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SpitalDetailActivity.this, ProgramareActivity.class);
            intent.putExtra("spitalId", spitalId);
            if (currentSpital != null) {
                intent.putExtra("spitalNume", currentSpital.getNume());
            }
            startActivity(intent);
        });

        mapBtn.setOnClickListener(v -> {
            if (currentSpital != null && currentSpital.getLocatie() != null) {
                String[] latLng = currentSpital.getLocatie().split(",");
                if (latLng.length == 2) {
                    Uri gmmIntentUri = Uri.parse("geo:" + latLng[0] + "," + latLng[1] + "?q=" +
                            Uri.encode(currentSpital.getNume()));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(SpitalDetailActivity.this,
                                "Google Maps nu este instalat", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        websiteContainer.setOnClickListener(v -> {
            if (currentSpital != null && currentSpital.getWebsite() != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(currentSpital.getWebsite()));
                startActivity(browserIntent);
            }
        });

        // Listener pentru a vedea toți medicii într-o activitate separată
        veziTotiMediciiTv.setOnClickListener(v -> {
            if (currentSpital != null) {
                Intent intent = new Intent(SpitalDetailActivity.this, MediciActivity.class);
                intent.putExtra("spitalId", spitalId);
                intent.putExtra("spitalNume", currentSpital.getNume());
                startActivity(intent);
            }
        });
    }

    private void loadSpitalDetails() {
        progressBar.setVisibility(View.VISIBLE);

        // First, try to load from SQLite
        new Thread(() -> {
            Spital spitalFromSQLite = findSpitalById(spitalId);

            runOnUiThread(() -> {
                if (spitalFromSQLite != null) {
                    currentSpital = spitalFromSQLite;
                    updateUI();
                    loadMediciForSpital(currentSpital.getNume());

                    // Load weather
                    weatherCard.postDelayed(() -> {
                        loadWeatherForSpital();
                    }, 500);
                }
                progressBar.setVisibility(View.GONE);
            });

            // Then sync with Firebase in background
            syncSpitalFromFirebase();
        }).start();
    }

    private Spital findSpitalById(String id) {
        List<Spital> allSpitale = dbHelper.getAllSpitale();
        for (Spital spital : allSpitale) {
            if (id.equals(spital.getId())) {
                return spital;
            }
        }
        return null;
    }

    private void syncSpitalFromFirebase() {
        mDatabase.child("spitale").child(spitalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Spital spitalFromFirebase = dataSnapshot.getValue(Spital.class);
                if (spitalFromFirebase != null) {
                    spitalFromFirebase.setId(dataSnapshot.getKey());

                    // Save to SQLite
                    dbHelper.insertOrUpdateSpital(spitalFromFirebase, true);

                    // Update UI if data is different
                    runOnUiThread(() -> {
                        if (currentSpital == null || !spitalFromFirebase.equals(currentSpital)) {
                            currentSpital = spitalFromFirebase;
                            updateUI();

                            // Reload medici if spital name has changed
                            loadMediciForSpital(currentSpital.getNume());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Log error but don't show to user since we have SQLite data
                android.util.Log.e("SpitalDetail", "Firebase error: " + databaseError.getMessage());
            }
        });
    }

    private void updateUI() {
        if (currentSpital == null) return;

        // Setează titlul în toolbar
        getSupportActionBar().setTitle(currentSpital.getNume());

        // Populează vizualizările
        numeTv.setText(currentSpital.getNume());
        adresaTv.setText(currentSpital.getAdresa());
        telefonTv.setText(currentSpital.getTelefon());
        descriereTv.setText(currentSpital.getDescriere());

        // Încarcă imaginea cu Glide
        if (currentSpital.getImagine() != null && !currentSpital.getImagine().isEmpty()) {
            Glide.with(SpitalDetailActivity.this)
                    .load(currentSpital.getImagine())
                    .placeholder(R.drawable.ic_hospital)
                    .error(R.drawable.ic_hospital)
                    .centerCrop()
                    .into(spitalImageView);
        } else {
            spitalImageView.setImageResource(R.drawable.ic_hospital);
        }
    }

    private void loadMediciForSpital(String spitalNume) {
        if (spitalNume == null || spitalNume.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);

        // Load from SQLite first
        new Thread(() -> {
            List<Medic> mediciFromSQLite = dbHelper.getMediciForSpital(spitalNume);

            runOnUiThread(() -> {
                updateMediciList(mediciFromSQLite);
                progressBar.setVisibility(View.GONE);
            });

            // Then sync with Firebase in background
            syncMediciFromFirebase(spitalNume);
        }).start();
    }

    private void syncMediciFromFirebase(String spitalNume) {
        Query query = mDatabase.child("medici").orderByChild("spital").equalTo(spitalNume);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Medic> mediciFromFirebase = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medic medic = snapshot.getValue(Medic.class);
                    if (medic != null) {
                        medic.setId(snapshot.getKey());
                        mediciFromFirebase.add(medic);

                        // Save to SQLite
                        dbHelper.insertOrUpdateMedic(medic, true);
                    }
                }

                runOnUiThread(() -> {
                    updateMediciList(mediciFromFirebase);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Log error but don't show to user since we have SQLite data
                android.util.Log.e("SpitalDetail", "Medici Firebase error: " + databaseError.getMessage());
            }
        });
    }

    private void updateMediciList(List<Medic> mediciList) {
        this.mediciList.clear();
        this.mediciList.addAll(mediciList);
        mediciAdapter.notifyDataSetChanged();

        // Afișează/ascunde mesajul "niciun medic" și butonul "vezi toți medicii"
        if (mediciList.isEmpty()) {
            findViewById(R.id.no_medici_text).setVisibility(View.VISIBLE);
            mediciRecyclerView.setVisibility(View.GONE);
            veziTotiMediciiTv.setVisibility(View.GONE);
        } else {
            findViewById(R.id.no_medici_text).setVisibility(View.GONE);
            mediciRecyclerView.setVisibility(View.VISIBLE);
            veziTotiMediciiTv.setVisibility(View.VISIBLE);
        }
    }

    private void loadWeatherForSpital() {
        if (currentSpital == null || currentSpital.getAdresa() == null) {
            android.util.Log.d("WeatherDebug", "No hospital data available");
            return;
        }

        // Afișează cardul și loading
        weatherCard.post(() -> {
            weatherCard.setVisibility(View.VISIBLE);
            findViewById(R.id.weather_loading).setVisibility(View.VISIBLE);
            weatherContainer.setVisibility(View.GONE);
        });

        String city = extractCityFromAddress(currentSpital.getAdresa());
        android.util.Log.d("WeatherDebug", "Loading weather for: " + city);

        getLocationKey(city);
    }

    private void getLocationKey(String city) {
        new Thread(() -> {
            try {
                String url = String.format(
                        "https://dataservice.accuweather.com/locations/v1/cities/search?apikey=%s&q=%s&language=ro",
                        ACCUWEATHER_API_KEY,
                        URLEncoder.encode(city + ", Romania", "UTF-8")
                );

                android.util.Log.d("WeatherDebug", "URL: " + url);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                android.util.Log.d("WeatherDebug", "Response code: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray locations = new JSONArray(response.toString());
                    if (locations.length() > 0) {
                        JSONObject location = locations.getJSONObject(0);
                        String locationKey = location.getString("Key");
                        android.util.Log.d("WeatherDebug", "Location key: " + locationKey);
                        getCurrentWeather(locationKey);
                    } else {
                        android.util.Log.d("WeatherDebug", "No locations found");
                        runOnUiThread(() -> weatherCard.setVisibility(View.GONE));
                    }
                } else {
                    android.util.Log.e("WeatherDebug", "Error response code: " + responseCode);
                    runOnUiThread(() -> weatherCard.setVisibility(View.GONE));
                }

            } catch (Exception e) {
                android.util.Log.e("WeatherDebug", "Error: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> weatherCard.setVisibility(View.GONE));
            }
        }).start();
    }

    private void getCurrentWeather(String locationKey) {
        new Thread(() -> {
            try {
                // URL pentru vremea curentă
                String url = String.format(
                        "https://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s&language=ro&details=true",
                        locationKey,
                        ACCUWEATHER_API_KEY
                );

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsează răspunsul vremii
                JSONArray weatherArray = new JSONArray(response.toString());
                if (weatherArray.length() > 0) {
                    JSONObject weather = weatherArray.getJSONObject(0);

                    // Extrage datele
                    String weatherText = weather.getString("WeatherText");
                    boolean hasPrecipitation = weather.getBoolean("HasPrecipitation");

                    JSONObject temperature = weather.getJSONObject("Temperature");
                    JSONObject metric = temperature.getJSONObject("Metric");
                    double temp = metric.getDouble("Value");

                    JSONObject realFeel = weather.getJSONObject("RealFeelTemperature");
                    double feelsLike = realFeel.getJSONObject("Metric").getDouble("Value");

                    int humidity = weather.getInt("RelativeHumidity");
                    double visibility = weather.getJSONObject("Visibility").getJSONObject("Metric").getDouble("Value");

                    JSONObject wind = weather.getJSONObject("Wind");
                    double windSpeed = wind.getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");

                    int weatherIcon = weather.getInt("WeatherIcon");
                    String observationTime = weather.getString("LocalObservationDateTime");

                    // Actualizează UI-ul
                    runOnUiThread(() -> {
                        updateWeatherUI(temp, feelsLike, weatherText, humidity, visibility,
                                windSpeed, weatherIcon, observationTime, hasPrecipitation);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> weatherCard.setVisibility(View.GONE));
            }
        }).start();
    }

    private void updateWeatherUI(double temp, double feelsLike, String description,
                                 int humidity, double visibility, double windSpeed,
                                 int iconCode, String time, boolean hasPrecipitation) {

        // Afișează temperatura
        weatherTempTv.setText(String.format("%.0f°C", temp));

        // Afișează descrierea
        weatherDescTv.setText(description);

        // Afișează detalii suplimentare
        String details = String.format(
                "Se simte ca: %.0f°C • Umiditate: %d%% • Vânt: %.1f km/h • Vizibilitate: %.1f km",
                feelsLike, humidity, windSpeed, visibility
        );
        weatherDetailsTv.setText(details);

        // Formatează timpul
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            Date date = inputFormat.parse(time);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            weatherTimeTv.setText("Actualizat la " + outputFormat.format(date));
        } catch (Exception e) {
            weatherTimeTv.setText("Acum");
        }

        // Încarcă iconița vremii
        loadAccuWeatherIcon(iconCode);

        // Afișează cardul vremii
        findViewById(R.id.weather_loading).setVisibility(View.GONE);
        weatherContainer.setVisibility(View.VISIBLE);
    }

    private void loadAccuWeatherIcon(int iconCode) {
        // AccuWeather folosește coduri numerice pentru iconițe
        String iconUrl = String.format("https://developer.accuweather.com/sites/default/files/%02d-s.png", iconCode);

        Glide.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.ic_weather_default)
                .error(R.drawable.ic_weather_default)
                .into(weatherIconIv);
    }

    private String extractCityFromAddress(String address) {
        // Cazul specific pentru Cluj-Napoca
        if (address.contains("Cluj")) {
            return "Cluj-Napoca";
        }

        // Cazul pentru București (sau "Central" în cazul tău)
        if (address.toLowerCase().contains("bucuresti") || address.toLowerCase().contains("central")) {
            return "Bucuresti";
        }

        // Extrage orașul din adresă
        String[] parts = address.split(",");
        if (parts.length > 1) {
            String city = parts[parts.length - 1].trim();
            // Verifică dacă e un județ cunoscut
            if (city.contains("Cluj")) return "Cluj-Napoca";
            if (city.contains("Bucur")) return "Bucuresti";
            return city;
        }

        // Dacă nu poate extrage, returnează primul cuvânt
        String[] words = address.split(" ");
        if (words.length > 0) {
            return words[0];
        }

        return "Bucuresti"; // Default fallback
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up sync manager if needed
        if (syncManager != null) {
//            syncManager.cleanup();
        }
    }
}