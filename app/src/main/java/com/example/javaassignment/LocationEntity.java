package com.example.javaassignment;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double latitude;
    private double longitude;

    // Constructor
    public LocationEntity(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters (Room needs these to read/write the data)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}