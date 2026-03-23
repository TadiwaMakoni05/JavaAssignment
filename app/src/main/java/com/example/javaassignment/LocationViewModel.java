package com.example.javaassignment;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class LocationViewModel extends AndroidViewModel {

    private LocationRepository repository;
    private LiveData<List<LocationEntity>> allLocations;

    // The constructor creates the Repository and loads the saved locations
    public LocationViewModel(@NonNull Application application) {
        super(application);
        repository = new LocationRepository(application);
        allLocations = repository.getAllLocations();
    }

    // A method for the Activity to get the live list of locations
    public LiveData<List<LocationEntity>> getAllLocations() {
        return allLocations;
    }

    // A method for the Activity to save a new location
    public void insert(LocationEntity location) {
        repository.insert(location);
    }

    // --- NEW: Method to delete a location ---
    public void delete(LocationEntity location) {
        repository.delete(location);
    }

    // --- NEW: Method to update an existing location ---
    public void update(LocationEntity location) {
        repository.update(location);
    }
}