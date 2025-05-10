package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.ViewGroup;

public class StatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private PieChart pieChart;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private TextView lastUpdateText;

    // Add SQLite support
    private DatabaseHelper dbHelper;
    private OptimizedSyncManager syncManager;
    private ValueEventListener firebaseListener;
    private Handler mainHandler;

    // Statistics data containers
    private StatisticsData currentStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Configure toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Statistici Medicale");

        // Initialize views
        initializeViews();

        // Initialize database helpers
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = DatabaseHelper.getInstance(this);
        syncManager = new OptimizedSyncManager(this);
        mainHandler = new Handler(Looper.getMainLooper());

        // Load statistics
        loadStatistics();
    }

    private void initializeViews() {
        barChart = findViewById(R.id.bar_chart);
        pieChart = findViewById(R.id.pie_chart);
        progressBar = findViewById(R.id.progress_bar);

        // Simply find the TextView that's already in your XML layout
        lastUpdateText = findViewById(R.id.last_update_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadStatistics();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);
        pieChart.setVisibility(View.GONE);

        // First, load from SQLite for immediate response
        loadFromSQLite();

        // Then sync with Firebase in background
        syncFromFirebase();
    }

    private void loadFromSQLite() {
        new Thread(() -> {
            try {
                // Get all medici from SQLite
                List<Medic> mediciList = dbHelper.getAllMedici();

                if (!mediciList.isEmpty()) {
                    StatisticsData stats = calculateStatistics(mediciList);

                    mainHandler.post(() -> {
                        currentStats = stats;
                        updateUI(stats);
                        lastUpdateText.setText("Ultima actualizare: Date offline");

                        if (progressBar.getVisibility() == View.VISIBLE) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                // If SQLite fails, try Firebase
                syncFromFirebase();
            }
        }).start();
    }

    private void syncFromFirebase() {
        // Remove any existing listener
        if (firebaseListener != null) {
            mDatabase.child("medici").removeEventListener(firebaseListener);
        }

        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                new Thread(() -> {
                    List<Medic> mediciList = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Medic medic = snapshot.getValue(Medic.class);
                        if (medic != null) {
                            medic.setId(snapshot.getKey());
                            mediciList.add(medic);

                            // Save to SQLite for offline access
                            dbHelper.insertOrUpdateMedic(medic, true);
                        }
                    }

                    StatisticsData stats = calculateStatistics(mediciList);

                    mainHandler.post(() -> {
                        currentStats = stats;
                        updateUI(stats);

                        // Update last update time
                        lastUpdateText.setText("Ultima actualizare: " +
                                java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));

                        progressBar.setVisibility(View.GONE);
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);

                    // Only show error if we don't have any data from SQLite
                    if (currentStats == null) {
                        Toast.makeText(StatisticsActivity.this,
                                "Eroare la încărcarea datelor: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // We have SQLite data, just inform about sync failure
                        Toast.makeText(StatisticsActivity.this,
                                "Folosim date offline. Sincronizarea a eșuat.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        mDatabase.child("medici").addListenerForSingleValueEvent(firebaseListener);
    }

    private StatisticsData calculateStatistics(List<Medic> mediciList) {
        StatisticsData stats = new StatisticsData();

        for (Medic medic : mediciList) {
            // Count per hospital
            String spital = medic.getSpital();
            if (spital != null) {
                stats.hospitalCounts.put(spital,
                        stats.hospitalCounts.getOrDefault(spital, 0) + 1);
            }

            // Count per speciality
            String specialitate = medic.getSpecialitate();
            if (specialitate != null) {
                stats.specialityCounts.put(specialitate,
                        stats.specialityCounts.getOrDefault(specialitate, 0) + 1);
            }

            // Count per county
            String county = extractCountyFromHospital(spital);
            if (county != null) {
                stats.countyCounts.put(county,
                        stats.countyCounts.getOrDefault(county, 0) + 1);
            }
        }

        return stats;
    }

    private void updateUI(StatisticsData stats) {
        // Update bar chart
        setupBarChart(stats.hospitalCounts);

        // Update pie chart (you can choose which one to display)
        Map<String, Integer> top5Specialities = getTopNWithOthers(stats.specialityCounts, 5);
        setupPieChartCommon(top5Specialities, "Top 5 Specialități");

        // Or use counties instead:
        // Map<String, Integer> top5Counties = getTopNWithOthers(stats.countyCounts, 5);
        // setupPieChartCommon(top5Counties, "Top 5 Județe");

        barChart.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.VISIBLE);
    }

    // Inner class to hold statistics data
    private static class StatisticsData {
        Map<String, Integer> hospitalCounts = new HashMap<>();
        Map<String, Integer> specialityCounts = new HashMap<>();
        Map<String, Integer> countyCounts = new HashMap<>();
    }

    // Metodă helper pentru a obține Top N cu "Altele"
    private Map<String, Integer> getTopNWithOthers(Map<String, Integer> data, int n) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(data.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map<String, Integer> result = new HashMap<>();
        int othersCount = 0;

        for (int i = 0; i < sorted.size(); i++) {
            if (i < n) {
                result.put(sorted.get(i).getKey(), sorted.get(i).getValue());
            } else {
                othersCount += sorted.get(i).getValue();
            }
        }

        if (othersCount > 0) {
            result.put("Altele", othersCount);
        }

        return result;
    }

    // Metodă helper pentru a extrage județul
    private String extractCountyFromHospital(String hospitalName) {
        if (hospitalName == null) return "Necunoscut";

        // Listă de județe românești cu variantele lor
        Map<String, String[]> countyVariants = new HashMap<>();
        countyVariants.put("București", new String[]{"bucuresti", "central", "sectorul", "ilfov"});
        countyVariants.put("Cluj", new String[]{"cluj"});
        countyVariants.put("Timiș", new String[]{"timis", "timișoara"});
        countyVariants.put("Brașov", new String[]{"brasov", "brașov"});
        countyVariants.put("Iași", new String[]{"iasi", "iași"});
        countyVariants.put("Constanța", new String[]{"constanta", "constanța"});
        countyVariants.put("Galați", new String[]{"galati", "galați"});
        countyVariants.put("Sibiu", new String[]{"sibiu"});
        countyVariants.put("Suceava", new String[]{"suceava"});
        countyVariants.put("Teleorman", new String[]{"teleorman", "alexandria"});
        countyVariants.put("Tulcea", new String[]{"tulcea"});
        countyVariants.put("Vaslui", new String[]{"vaslui"});
        countyVariants.put("Vâlcea", new String[]{"valcea", "vâlcea", "râmnicu vâlcea"});
        countyVariants.put("Vrancea", new String[]{"vrancea", "focșani"});

        String lowerHospitalName = hospitalName.toLowerCase();

        // Căutăm prin toate județele și variantele lor
        for (Map.Entry<String, String[]> entry : countyVariants.entrySet()) {
            String county = entry.getKey();
            String[] variants = entry.getValue();

            for (String variant : variants) {
                if (lowerHospitalName.contains(variant)) {
                    return county;
                }
            }
        }

        return "Altele";
    }

    private void setupPieChartCommon(Map<String, Integer> data, String title) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, "Nu există date pentru " + title, Toast.LENGTH_SHORT).show();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, title);

        dataSet.setColors(new int[]{
                Color.parseColor("#3F51B5"), // Indigo
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#00BCD4"), // Cyan
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#F44336"), // Red
                Color.parseColor("#795548"), // Brown
                Color.parseColor("#607D8B"), // Blue Grey
                Color.parseColor("#FFC107")  // Amber
        });

        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        // Formatare personalizată pentru valori
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", value);
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText(title);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#333333"));
        pieChart.setHoleRadius(35f);
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setUsePercentValues(true);
        pieChart.animateY(1200);

        // Configurare legendă
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        legend.setWordWrapEnabled(true);

        pieChart.setEntryLabelTextSize(10f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setDrawEntryLabels(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        pieChart.invalidate();
    }

    private void setupBarChart(Map<String, Integer> data) {
        List<BarEntry> entries = new ArrayList<>();
        final String[] labels = new String[data.size()];
        int index = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));

            // Extrage doar orașul din numele spitalului
            String originalName = entry.getKey();
            String cityName = extractCityName(originalName);
            labels[index] = cityName;
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Medici per Oraș");
        dataSet.setColors(new int[]{R.color.chart_color1}, this);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);

        // Configurare axă X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        xAxis.setLabelCount(labels.length);

        // Pentru aliniere perfectă
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(entries.size() - 0.5f);

        // Configurare axe
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setTextSize(10f);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);

        // Configurare generală
        barChart.getLegend().setTextSize(12f);
        barChart.animateY(1200);
        barChart.setExtraBottomOffset(50f);
        barChart.setExtraLeftOffset(10f);
        barChart.setExtraRightOffset(20f);
        barChart.setExtraTopOffset(20f);

        // Setări pentru interacțiune
        barChart.setTouchEnabled(true);
        barChart.setPinchZoom(false);
        barChart.setScaleXEnabled(false);
        barChart.setScaleYEnabled(false);

        barChart.setVisibleXRangeMaximum(labels.length);
        barChart.moveViewToX(0);
        barChart.setFitBars(true);

        barChart.invalidate();
    }

    // Metodă helper pentru a extrage orașul din numele spitalului
    private String extractCityName(String hospitalName) {
        if (hospitalName == null) return "";

        // Cazul "Spitalul X din Oraș"
        if (hospitalName.contains(" din ")) {
            String[] parts = hospitalName.split(" din ");
            if (parts.length > 1) {
                return parts[1];
            }
        }

        // Cazul "Spitalul Municipal/Județean Oraș"
        if (hospitalName.contains("Municipal") || hospitalName.contains("Județean")) {
            String[] words = hospitalName.split(" ");
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equals("Municipal") || words[i].equals("Județean")) {
                    if (i + 1 < words.length) {
                        return words[i + 1];
                    }
                }
            }
        }

        // Cazul "Spitalul Militar Oraș"
        if (hospitalName.contains("Militar")) {
            String[] words = hospitalName.split(" ");
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equals("Militar")) {
                    if (i + 1 < words.length) {
                        return words[i + 1];
                    }
                }
            }
        }

        // Cazul general - ia ultimul cuvânt (de obicei orașul)
        String[] words = hospitalName.split(" ");
        if (words.length > 0) {
            return words[words.length - 1];
        }

        return hospitalName; // Returnează numele original dacă nu se potrivește niciun pattern
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove Firebase listener to prevent memory leaks
        if (firebaseListener != null && mDatabase != null) {
            mDatabase.child("medici").removeEventListener(firebaseListener);
            firebaseListener = null;
        }

        // Clean up sync manager if needed
        if (syncManager != null) {
//            syncManager.cleanup();
        }

        // Clean up handler
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload data when returning to activity
        loadStatistics();
    }
}