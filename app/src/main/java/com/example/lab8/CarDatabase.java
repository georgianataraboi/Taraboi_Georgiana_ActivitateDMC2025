package com.example.lab8;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.lab8.Car;

@Database(entities = {Car.class}, version = 1)
public abstract class CarDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "car_database";
    private static CarDatabase instance;

    public abstract CarDao carDao();

    public static synchronized CarDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CarDatabase.class,
                            DATABASE_NAME
                    ).allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
