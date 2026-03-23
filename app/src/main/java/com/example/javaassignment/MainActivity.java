package com.example.javaassignment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private LocationViewModel locationViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.GeofencingClient geofencingClient;

    // UI Elements for visibility toggling
    private View mapContainer;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        checkAndRequestPermissions();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);

        // Initialize UI Elements
        mapContainer = findViewById(R.id.mapContainer);
        recyclerView = findViewById(R.id.locationsRecyclerView);
        fab = findViewById(R.id.fabSaveLocation);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        LocationAdapter adapter = new LocationAdapter();
        recyclerView.setAdapter(adapter);

        // --- UPDATED: Navigation Logic ---
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Show Map and FAB
                mapContainer.setVisibility(View.VISIBLE);
                fab.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_history) {
                // Hide Map and FAB to let List take over the screen
                mapContainer.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Standard Click to Navigate
        adapter.setOnLocationClickListener(location -> {
            startDrivingNavigation(location.getLatitude(), location.getLongitude());
        });

        // Long Click to Edit or Delete
        adapter.setOnLocationLongClickListener(this::showOptionsDialog);

        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        locationViewModel.getAllLocations().observe(this, adapter::setLocations);

        fab.setOnClickListener(v -> saveRealLocation());
    }

    private void saveRealLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            promptForLocationName(location.getLatitude(), location.getLongitude());
                        } else {
                            Toast.makeText(MainActivity.this, "Searching for GPS signal...", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Need GPS permission first!", Toast.LENGTH_SHORT).show();
        }
    }

    private void promptForLocationName(double lat, double lng) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Name your parking spot");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g., Level 4, Row G");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "My Spot";

            LocationEntity realLocation = new LocationEntity(name, lat, lng);
            locationViewModel.insert(realLocation);
            addGeofence(lat, lng, name);

            Toast.makeText(this, "Saved: " + name, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showOptionsDialog(LocationEntity location) {
        CharSequence[] options = new CharSequence[]{"Edit Name", "Delete Spot"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(location.getName());

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                promptForEdit(location);
            } else if (which == 1) {
                locationViewModel.delete(location);
                removeGeofence(location.getName());
                Toast.makeText(this, "Location Deleted", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void promptForEdit(LocationEntity location) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(location.getName());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                location.setName(newName);
                locationViewModel.update(location);
                Toast.makeText(this, "Name Updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addGeofence(double lat, double lng, String locationName) {
        com.google.android.gms.location.Geofence geofence = new com.google.android.gms.location.Geofence.Builder()
                .setRequestId(locationName)
                .setCircularRegion(lat, lng, 200)
                .setExpirationDuration(com.google.android.gms.location.Geofence.NEVER_EXPIRE)
                .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        com.google.android.gms.location.GeofencingRequest geofencingRequest = new com.google.android.gms.location.GeofencingRequest.Builder()
                .setInitialTrigger(com.google.android.gms.location.GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        android.content.Intent intent = new android.content.Intent(this, GeofenceBroadcastReceiver.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(this, aVoid -> Toast.makeText(this, "Geofence Armed (200m)", Toast.LENGTH_SHORT).show());
        }
    }

    private void removeGeofence(String requestId) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        ids.add(requestId);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.removeGeofences(ids);
        }
    }

    public void startDrivingNavigation(double lat, double lng) {
        android.net.Uri gmmIntentUri = android.net.Uri.parse("google.navigation:q=" + lat + "," + lng);
        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Google Maps is not installed!", Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel("GEOFENCE_CHANNEL_ID", "Geofence Alerts", android.app.NotificationManager.IMPORTANCE_HIGH);
            getSystemService(android.app.NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "App needs location to work properly.", Toast.LENGTH_LONG).show();
        }
    }
}