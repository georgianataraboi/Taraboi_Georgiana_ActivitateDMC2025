package com.example.lab8;


import android.content.Context;


import java.util.List;

public class CarRepository {
    private CarDao carDao;

    public CarRepository(Context context) {
        CarDatabase database = CarDatabase.getInstance(context);
        carDao = database.carDao();
    }

    // 1. Inserare
    public long insertCar(Car car) {
        return carDao.insert(car);
    }

    // 2. Selectare toate
    public List<Car> getAllCars() {
        return carDao.getAllCars();
    }

    // 3. Selectare dupa model
    public List<Car> getCarsByModel(String modelName) {
        return carDao.getCarsByModel(modelName);
    }

    // 4. Selectare dupa interval de ani
    public List<Car> getCarsInYearRange(int minYear, int maxYear) {
        return carDao.getCarsInYearRange(minYear, maxYear);
    }

    // 5a. Stergere masini cu pret mai mare decat parametrul
    public void deleteCarsAbovePrice(double maxPrice) {
        carDao.deleteCarsAbovePrice(maxPrice);
    }

    // 5b. Stergere masini cu pret mai mic decat parametrul
    public void deleteCarsBelowPrice(double minPrice) {
        carDao.deleteCarsBelowPrice(minPrice);
    }

    // 6. Incrementare an pentru masini cu brand incepand cu litera specificata
    public void incrementYearForBrandsStartingWith(String startLetter) {
        carDao.incrementYearForBrandsStartingWith(startLetter);
    }
}
