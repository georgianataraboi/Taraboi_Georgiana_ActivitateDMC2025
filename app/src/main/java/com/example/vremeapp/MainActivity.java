package com.example.vremeapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText cityEditText;
    private Button searchButton;
    private TextView resultTextView;
    private TextView jsonResponseTextView;
    private Spinner daysSpinner;

    private int selectedDays = 1;

    private static final String API_KEY = "Q2wI1AYYH5rfckTCuoAs1H3LhnozI3Vn";
    private static final String BASE_URL = "https://dataservice.accuweather.com/locations/v1/cities/search";
    private static final String FORECAST_BASE_URL = "https://dataservice.accuweather.com/forecasts/v1/daily/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inițializare componente UI
        cityEditText = findViewById(R.id.cityEditText);
        searchButton = findViewById(R.id.searchButton);
        resultTextView = findViewById(R.id.resultTextView);
        jsonResponseTextView = findViewById(R.id.jsonResponseTextView);
        daysSpinner = findViewById(R.id.daysSpinner);

        setupSpinner();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = cityEditText.getText().toString().trim();

                if (!cityName.isEmpty()) {
                    // Executare AsyncTask pentru cautarea codului orașului
                    new CitySearchTask().execute(cityName);
                } else {
                    Toast.makeText(MainActivity.this, "Introduceți numele orașului", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Configurare spinner pentru selectarea numarului de zile
    private void setupSpinner() {
        List<String> daysOptions = new ArrayList<>();
        daysOptions.add("1 zi");
        daysOptions.add("5 zile");
        daysOptions.add("10 zile");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, daysOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daysSpinner.setAdapter(adapter);

        daysSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        selectedDays = 1;
                        break;
                    case 1:
                        selectedDays = 5;
                        break;
                    case 2:
                        selectedDays = 10;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDays = 1;
            }
        });
    }

    // AsyncTask pentru apelul API-ului AccuWeather pentru cautare oraș
    private class CitySearchTask extends AsyncTask<String, Void, Pair<String, String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultTextView.setText("Se caută...");
            jsonResponseTextView.setText("");
        }

        @Override
        protected Pair<String, String> doInBackground(String... params) {
            String cityName = params[0];
            String cityKey = null;
            String jsonResponse = null;

            try {
                // Construire URL pentru cautare
                String urlString = BASE_URL + "?apikey=" + API_KEY + "&q=" + cityName;
                URL url = new URL(urlString);

                // Deschidere conexiune HTTP
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Verificare cod de raspuns
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return new Pair<>("Error: " + responseCode, null);
                }

                // Citire raspuns
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                // Salvare JSON complet
                jsonResponse = response.toString();

                // Parsare JSON pentru obtinerea codului orasului
                JSONArray jsonArray = new JSONArray(jsonResponse);

                if (jsonArray.length() > 0) {
                    JSONObject cityObject = jsonArray.getJSONObject(0);
                    cityKey = cityObject.getString("Key");

                    // Pentru debugging, putem loga și alte informații
                    String cityName1 = cityObject.getString("LocalizedName");
                    String countryName = cityObject.getJSONObject("Country").getString("LocalizedName");
                    System.out.println("Oraș găsit: " + cityName1 + ", " + countryName + ", Key: " + cityKey);
                }

                return new Pair<>(cityKey, jsonResponse);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return new Pair<>("Error: " + e.getMessage(), null);
            }
        }

        @Override
        protected void onPostExecute(Pair<String, String> result) {
            super.onPostExecute(result);

            String cityKey = result.first;
            String jsonResponse = result.second;

            if (cityKey != null) {
                if (cityKey.startsWith("Error:")) {
                    resultTextView.setText(cityKey);
                } else {
                    resultTextView.setText("Cod oraș: " + cityKey);

                    // Obtine vremea folosind codul orasului si numarul de zile selectat
                    new WeatherForecastTask().execute(cityKey);
                }
            } else {
                resultTextView.setText("Eroare la căutarea orașului. Verificați conexiunea la internet sau numele orașului.");
            }

            // Afisare JSON complet
            if (jsonResponse != null) {
                try {
                    // Formatare JSON pentru o afișare mai lizibila
                    JSONArray jsonArray = new JSONArray(jsonResponse);
                    String prettyJson = jsonArray.toString(4);
                    jsonResponseTextView.setText(prettyJson);
                } catch (JSONException e) {
                    jsonResponseTextView.setText(jsonResponse);
                }
            }
        }
    }

    // AsyncTask pentru obtinerea prognozei meteo pentru un oraa
    private class WeatherForecastTask extends AsyncTask<String, Void, Pair<String, String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultTextView.setText(resultTextView.getText() + "\nSe obține prognoza meteo...");
        }

        @Override
        protected Pair<String, String> doInBackground(String... params) {
            String cityKey = params[0];
            String forecastInfo = null;
            String jsonResponse = null;

            try {
                // Construire URL pentru prognoza meteo, în funcție de numărul de zile selectat
                String endpoint = selectedDays + "day/";
                String urlString = FORECAST_BASE_URL + endpoint + cityKey + "?apikey=" + API_KEY + "&language=en-us&details=false&metric=false";
                URL url = new URL(urlString);

                // Deschidere conexiune HTTP
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Verificare cod de răspuns
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return new Pair<>("Error: " + responseCode, null);
                }

                // Citire răspuns
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                // Salvare JSON complet
                jsonResponse = response.toString();

                // Parsare JSON pentru obținerea temperaturilor
                JSONObject forecastJson = new JSONObject(jsonResponse);
                JSONArray dailyForecasts = forecastJson.getJSONArray("DailyForecasts");

                StringBuilder forecastBuilder = new StringBuilder();
                forecastBuilder.append("Prognoza meteo pentru ").append(selectedDays).append(" zile:\n\n");

                // Parcurgem fiecare zi și extragem temperaturile
                for (int i = 0; i < dailyForecasts.length(); i++) {
                    JSONObject dayForecast = dailyForecasts.getJSONObject(i);
                    String date = dayForecast.getString("Date").substring(0, 10);

                    JSONObject temperature = dayForecast.getJSONObject("Temperature");

                    JSONObject minTemp = temperature.getJSONObject("Minimum");
                    double minValue = minTemp.getDouble("Value");
                    String minUnit = minTemp.getString("Unit");

                    JSONObject maxTemp = temperature.getJSONObject("Maximum");
                    double maxValue = maxTemp.getDouble("Value");
                    String maxUnit = maxTemp.getString("Unit");

                    forecastBuilder.append("Ziua ").append(i + 1).append(" (").append(date).append("):\n");
                    forecastBuilder.append("  Temperatură minimă: ").append(minValue).append("° ").append(minUnit).append("\n");
                    forecastBuilder.append("  Temperatură maximă: ").append(maxValue).append("° ").append(maxUnit).append("\n\n");
                }

                forecastInfo = forecastBuilder.toString();

                return new Pair<>(forecastInfo, jsonResponse);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return new Pair<>("Error: " + e.getMessage(), null);
            }
        }

        @Override
        protected void onPostExecute(Pair<String, String> result) {
            super.onPostExecute(result);

            String forecastInfo = result.first;
            String jsonResponse = result.second;

            if (forecastInfo != null) {
                if (forecastInfo.startsWith("Error:")) {
                    resultTextView.setText(resultTextView.getText() + "\n" + forecastInfo);
                } else {
                    resultTextView.setText("Cod oraș: " + resultTextView.getText().toString().replace("Cod oraș: ", "").split("\n")[0] + "\n\n" + forecastInfo);
                }
            } else {
                resultTextView.setText(resultTextView.getText() + "\nEroare la obținerea prognozei meteo.");
            }

            // Afișare JSON complet
            if (jsonResponse != null) {
                try {
                    // Formatare JSON pentru o afișare mai lizibilă
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    String prettyJson = jsonObject.toString(4);
                    jsonResponseTextView.setText(prettyJson);
                } catch (JSONException e) {
                    jsonResponseTextView.setText(jsonResponse);
                }
            }
        }
    }
}