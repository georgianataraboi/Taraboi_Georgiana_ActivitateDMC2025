package com.example.lab8;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.lab8.Car;

import java.util.List;

@Dao
public interface CarDao {
    // 1. Metoda de inserare in baza de date
    @Insert
    long insert(Car car);

    // 2. Metoda de selectie a tuturor inregistrarilor
    @Query("SELECT * FROM cars")
    List<Car> getAllCars();

    // 3. Metoda de selectie a obiectelor cu un model specific
    @Query("SELECT * FROM cars WHERE model = :modelName")
    List<Car> getCarsByModel(String modelName);

    // 4. Metoda de selectie a obiectelor care au anul in intervalul specificat
    @Query("SELECT * FROM cars WHERE year BETWEEN :minYear AND :maxYear")
    List<Car> getCarsInYearRange(int minYear, int maxYear);

    // 5. Metoda de stergere a inregistrarilor cu prețul mai mare decat un anumit preț
    @Query("DELETE FROM cars WHERE price > :maxPrice")
    void deleteCarsAbovePrice(double maxPrice);

    // 5b. Metoda de stergere a inregistrarilor cu prețul mai mic decat un anumit preț
    @Query("DELETE FROM cars WHERE price < :minPrice")
    void deleteCarsBelowPrice(double minPrice);

    // 6. Metoda de crestere cu o unitate a anului pentru masinile al caror brand incepe cu o anumita litera
    @Query("UPDATE cars SET year = year + 1 WHERE brand LIKE :startLetter || '%'")
    void incrementYearForBrandsStartingWith(String startLetter);
}
