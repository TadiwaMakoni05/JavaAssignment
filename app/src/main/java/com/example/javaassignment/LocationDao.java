package com.example.javaassignment;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface LocationDao {

    // Saves a new location to the database
    @Insert
    void insert(LocationEntity location);

    // Retrieves all saved locations to show in our list
    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    LiveData<List<LocationEntity>> getAllLocations();

    // --- NEW: Deletes a specific location from the database ---
    @Delete
    void delete(LocationEntity location);

    // --- NEW: Updates an existing location's name in the database ---
    @Update
    void update(LocationEntity location);

    // Synchronous query used by background service to re-register geofences
    @Query("SELECT * FROM saved_locations")
    List<LocationEntity> getAllLocationsList();
}