package com.example.javaassignment;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LocationEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Connects the database to our DAO instructions
    public abstract LocationDao locationDao();

    // Creates a "Singleton" so we don't accidentally open 5 databases at once
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "location_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}