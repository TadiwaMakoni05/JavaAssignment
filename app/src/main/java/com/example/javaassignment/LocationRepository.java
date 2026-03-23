package com.example.javaassignment;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationRepository {

    private LocationDao locationDao;
    private LiveData<List<LocationEntity>> allLocations;

    // This ExecutorService creates background threads so we don't freeze the app
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Constructor: This wires up the database when the Repository is created
    public LocationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        locationDao = db.locationDao();
        allLocations = locationDao.getAllLocations();
    }

    // Returns our list of locations. Because it's "LiveData", the UI will
    // update automatically whenever the database changes!
    public LiveData<List<LocationEntity>> getAllLocations() {
        return allLocations;
    }

    // Takes a new location and saves it in the background
    public void insert(LocationEntity location) {
        databaseWriteExecutor.execute(() -> {
            locationDao.insert(location);
        });
    }

    // --- NEW: Deletes a location in the background ---
    public void delete(LocationEntity location) {
        databaseWriteExecutor.execute(() -> {
            locationDao.delete(location);
        });
    }

    // --- NEW: Updates a location in the background ---
    public void update(LocationEntity location) {
        databaseWriteExecutor.execute(() -> {
            locationDao.update(location);
        });
    }
}