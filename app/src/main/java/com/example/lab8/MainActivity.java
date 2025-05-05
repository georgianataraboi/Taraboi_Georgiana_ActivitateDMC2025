package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // UI Components
    private EditText etBrand, etModel, etYear, etPrice;
    private EditText etModelSearch, etMinYear, etMaxYear, etPriceThreshold, etStartLetter;
    private Button btnAddCar, btnSearchModel, btnFilterYear;
    private Button btnDeleteAbove, btnDeleteBelow, btnIncrementYear, btnShowAll;
    private ListView lvCars;

    // Data
    private CarRepository carRepository;
    private CarAdapter carAdapter;
    private List<Car> carList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button galleryButton = findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImageGalleryActivity.class);
                startActivity(intent);
            }
        });

        carRepository = new CarRepository(this);

        initializeViews();

        carList = new ArrayList<>();
        carAdapter = new CarAdapter(this, carList);
        lvCars.setAdapter(carAdapter);

        refreshCarList();

        setupListeners();
    }

    private void initializeViews() {
        // Input fields for adding a car
        etBrand = findViewById(R.id.et_brand);
        etModel = findViewById(R.id.et_model);
        etYear = findViewById(R.id.et_year);
        etPrice = findViewById(R.id.et_price);
        btnAddCar = findViewById(R.id.btn_add_car);

        // Input fields for searching and filtering
        etModelSearch = findViewById(R.id.et_model_search);
        etMinYear = findViewById(R.id.et_min_year);
        etMaxYear = findViewById(R.id.et_max_year);
        etPriceThreshold = findViewById(R.id.et_price_threshold);
        etStartLetter = findViewById(R.id.et_start_letter);

        // Button actions
        btnSearchModel = findViewById(R.id.btn_search_model);
        btnFilterYear = findViewById(R.id.btn_filter_year);
        btnDeleteAbove = findViewById(R.id.btn_delete_above);
        btnDeleteBelow = findViewById(R.id.btn_delete_below);
        btnIncrementYear = findViewById(R.id.btn_increment_year);
        btnShowAll = findViewById(R.id.btn_show_all);

        // ListView for car display
        lvCars = findViewById(R.id.lv_cars);
    }

    private void setupListeners() {
        // 1. Add a new car
        btnAddCar.setOnClickListener(v -> addNewCar());

        // 2. Show all cars
        btnShowAll.setOnClickListener(v -> refreshCarList());

        // 3. Search by model
        btnSearchModel.setOnClickListener(v -> searchByModel());

        // 4. Filter by year range
        btnFilterYear.setOnClickListener(v -> filterByYearRange());

        // 5a. Delete cars above price threshold
        btnDeleteAbove.setOnClickListener(v -> deleteCarsAbovePrice());

        // 5b. Delete cars below price threshold
        btnDeleteBelow.setOnClickListener(v -> deleteCarsBelowPrice());

        // 6. Increment year for cars with brand starting with letter
        btnIncrementYear.setOnClickListener(v -> incrementYearForBrands());
    }

    // 1. Method to add a new car
    private void addNewCar() {
        // Validate inputs
        if (validateInputs()) {
            String brand = etBrand.getText().toString().trim();
            String model = etModel.getText().toString().trim();
            int year = Integer.parseInt(etYear.getText().toString());
            double price = Double.parseDouble(etPrice.getText().toString());

            // Create and insert car
            Car car = new Car(brand, model, year, price);
            long id = carRepository.insertCar(car);

            if (id > 0) {
                Toast.makeText(this, "Masina adaugata cu succes", Toast.LENGTH_SHORT).show();
                clearInputFields();
                refreshCarList();
            } else {
                Toast.makeText(this, "Eroare la adaugarea masinii", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etBrand.getText())) {
            etBrand.setError("Introduceti marca");
            return false;
        }
        if (TextUtils.isEmpty(etModel.getText())) {
            etModel.setError("Introduceti modelul");
            return false;
        }
        if (TextUtils.isEmpty(etYear.getText())) {
            etYear.setError("Introduceti anul");
            return false;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Introduceti pretul");
            return false;
        }
        return true;
    }

    private void clearInputFields() {
        etBrand.setText("");
        etModel.setText("");
        etYear.setText("");
        etPrice.setText("");
    }

    // 2. Method to refresh the car list with all cars
    private void refreshCarList() {
        List<Car> cars = carRepository.getAllCars();
        updateCarListView(cars);
    }

    // 3. Method to search cars by model
    private void searchByModel() {
        String model = etModelSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(model)) {
            List<Car> cars = carRepository.getCarsByModel(model);
            updateCarListView(cars);
            String message = cars.size() + " masini gasite pentru modelul " + model;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            etModelSearch.setError("Introduceți un model");
        }
    }

    // 4. Method to filter cars by year range
    private void filterByYearRange() {
        if (TextUtils.isEmpty(etMinYear.getText())) {
            etMinYear.setError("Introduceți anul minim");
            return;
        }
        if (TextUtils.isEmpty(etMaxYear.getText())) {
            etMaxYear.setError("Introduceți anul maxim");
            return;
        }

        int minYear = Integer.parseInt(etMinYear.getText().toString());
        int maxYear = Integer.parseInt(etMaxYear.getText().toString());

        if (minYear > maxYear) {
            Toast.makeText(this, "Anul minim trebuie sa fie mai mic decat anul maxim", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Car> cars = carRepository.getCarsInYearRange(minYear, maxYear);
        updateCarListView(cars);
        String message = cars.size() + " masini gasite intre anii " + minYear + " si " + maxYear;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // 5a. Method to delete cars above price threshold
    private void deleteCarsAbovePrice() {
        if (TextUtils.isEmpty(etPriceThreshold.getText())) {
            etPriceThreshold.setError("Introduceti un prag de pret");
            return;
        }

        double priceThreshold = Double.parseDouble(etPriceThreshold.getText().toString());
        carRepository.deleteCarsAbovePrice(priceThreshold);
        refreshCarList();
        Toast.makeText(this, "Masini cu pret peste " + priceThreshold + " € sterse", Toast.LENGTH_SHORT).show();
    }

    // 5b. Method to delete cars below price threshold
    private void deleteCarsBelowPrice() {
        if (TextUtils.isEmpty(etPriceThreshold.getText())) {
            etPriceThreshold.setError("Introduceti un prag de pret");
            return;
        }

        double priceThreshold = Double.parseDouble(etPriceThreshold.getText().toString());
        carRepository.deleteCarsBelowPrice(priceThreshold);
        refreshCarList();
        Toast.makeText(this, "Masini cu pret sub " + priceThreshold + " € șterse", Toast.LENGTH_SHORT).show();
    }

    // 6. Method to increment year for cars with brand starting with letter
    private void incrementYearForBrands() {
        String startLetter = etStartLetter.getText().toString().trim();
        if (TextUtils.isEmpty(startLetter)) {
            etStartLetter.setError("Introduceti o litera");
            return;
        }

        carRepository.incrementYearForBrandsStartingWith(startLetter);
        refreshCarList();
        Toast.makeText(this, "An incrementat pentru marci care incep cu " + startLetter, Toast.LENGTH_SHORT).show();
    }

    // Helper method to update ListView
    private void updateCarListView(List<Car> cars) {
        carAdapter.updateData(cars);
    }
}