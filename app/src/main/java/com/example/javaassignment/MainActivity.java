package com.example.javaassignment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Main Activity that:
 * 1. Displays an embedded Google Map with the user's current location.
 * 2. Allows adding/saving custom GPS locations (favorite places, landmarks).
 * 3. Shows saved locations as markers on the map with 200m geofence circles.
 * 4. Provides live in-app directions (polyline route) to a selected saved location.
 * 5. Starts a foreground service for background geofence monitoring & notifications.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 200;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 300;

    // Replace this with your actual Google Maps Directions API key
    private static final String DIRECTIONS_API_KEY = "AIzaSyC2eVMVMIp2XX_2DGUWdmWS-s01NbIZi7U";

    private LocationViewModel locationViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private GeofencingClient geofencingClient;

    // Google Map
    private GoogleMap googleMap;
    private Marker currentLocationMarker;
    private Polyline currentRoutePolyline;
    private LatLng currentLatLng;

    // Saved location markers and circles on the map
    private List<Marker> savedMarkers = new ArrayList<>();
    private List<com.google.android.gms.maps.model.Circle> savedCircles = new ArrayList<>();

    // UI Elements
    private View mapContainer;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private LinearLayout infoBar;
    private TextView tvRouteInfo;
    private ImageButton btnClearRoute;

    // Broadcast receiver for location updates from the foreground service
    private BroadcastReceiver locationUpdateReceiver;

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
        infoBar = findViewById(R.id.infoBar);
        tvRouteInfo = findViewById(R.id.tvRouteInfo);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Initialize the Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        LocationAdapter adapter = new LocationAdapter();
        recyclerView.setAdapter(adapter);

        // Bottom Navigation: switch between Map view and History list
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                mapContainer.setVisibility(View.VISIBLE);
                fab.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_history) {
                mapContainer.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
                infoBar.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Tap on a saved location -> show in-app directions on the map
        adapter.setOnLocationClickListener(location -> {
            // Switch to map view first
            bottomNav.setSelectedItemId(R.id.nav_home);
            showDirectionsOnMap(location.getLatitude(), location.getLongitude(), location.getName());
        });

        // Long-press on a saved location -> edit or delete
        adapter.setOnLocationLongClickListener(this::showOptionsDialog);

        // ViewModel: observe saved locations and update map markers
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        locationViewModel.getAllLocations().observe(this, locations -> {
            adapter.setLocations(locations);
            updateMapMarkers(locations);
        });

        // FAB: save current GPS location
        fab.setOnClickListener(v -> saveRealLocation());

        // Clear route button
        btnClearRoute.setOnClickListener(v -> clearRoute());

        // Setup location update receiver from the foreground service
        setupLocationReceiver();
    }

    // ======================== GOOGLE MAP SETUP ========================

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Enable the blue dot for current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // Get initial location and zoom to it
        getInitialLocation();

        // When user taps a marker on the map, show directions to it
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.equals(currentLocationMarker)) return false; // Skip current location marker

            Object tag = marker.getTag();
            if (tag instanceof LocationEntity) {
                LocationEntity loc = (LocationEntity) tag;
                showDirectionsOnMap(loc.getLatitude(), loc.getLongitude(), loc.getName());
                return true;
            }
            return false;
        });
    }

    /**
     * Centers the map on the user's current GPS position.
     */
    private void getInitialLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                updateCurrentLocationMarker(currentLatLng);
            }
        });
    }

    /**
     * Updates the "You are here" marker on the map.
     */
    private void updateCurrentLocationMarker(LatLng latLng) {
        if (googleMap == null) return;
        currentLatLng = latLng;

        if (currentLocationMarker != null) {
            currentLocationMarker.setPosition(latLng);
        } else {
            currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }

    /**
     * Clears existing saved-location markers and redraws them from the database list.
     * Each saved location gets a red marker and a 200m radius geofence circle.
     */
    private void updateMapMarkers(List<LocationEntity> locations) {
        if (googleMap == null) return;

        // Remove old markers and circles
        for (Marker m : savedMarkers) m.remove();
        for (com.google.android.gms.maps.model.Circle c : savedCircles) c.remove();
        savedMarkers.clear();
        savedCircles.clear();

        if (locations == null) return;

        for (LocationEntity loc : locations) {
            LatLng position = new LatLng(loc.getLatitude(), loc.getLongitude());

            // Add marker
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(loc.getName())
                    .snippet("Lat: " + loc.getLatitude() + ", Lng: " + loc.getLongitude())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) {
                marker.setTag(loc); // Tag with entity so we can retrieve it on click
                savedMarkers.add(marker);
            }

            // Draw 200m geofence radius circle
            com.google.android.gms.maps.model.Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(200) // 200 meters
                    .strokeColor(Color.argb(180, 66, 133, 244))   // Blue stroke
                    .fillColor(Color.argb(40, 66, 133, 244))      // Light blue fill
                    .strokeWidth(3f));
            savedCircles.add(circle);
        }
    }

    // ======================== IN-APP DIRECTIONS ========================

    /**
     * Fetches directions from current location to the destination and draws
     * a polyline route on the embedded Google Map.
     */
    private void showDirectionsOnMap(double destLat, double destLng, String destName) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Waiting for GPS fix... Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear any previous route
        clearRoute();

        // Show info bar with loading message
        infoBar.setVisibility(View.VISIBLE);
        tvRouteInfo.setText("Calculating route to " + destName + "...");

        // Build the Directions API URL
        String origin = currentLatLng.latitude + "," + currentLatLng.longitude;
        String destination = destLat + "," + destLng;
        String url = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + origin
                + "&destination=" + destination
                + "&mode=driving"
                + "&key=" + DIRECTIONS_API_KEY;

        // Make the API request on a background thread using OkHttp
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Directions API request failed", e);
                runOnUiThread(() -> {
                    tvRouteInfo.setText("Failed to get directions. Showing straight line.");
                    // Fallback: draw a straight line
                    drawStraightLine(destLat, destLng, destName);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    try {
                        DirectionsParser parser = new DirectionsParser(responseBody);
                        List<LatLng> routePoints = parser.getPoints();

                        if (routePoints.isEmpty()) {
                            tvRouteInfo.setText("No route found. Showing straight line to " + destName);
                            drawStraightLine(destLat, destLng, destName);
                            return;
                        }

                        // Draw the route polyline on the map
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(routePoints)
                                .width(10f)
                                .color(Color.rgb(66, 133, 244)) // Google blue
                                .geodesic(true);

                        currentRoutePolyline = googleMap.addPolyline(polylineOptions);

                        // Update info bar with distance and duration
                        tvRouteInfo.setText("📍 " + destName + "  •  " +
                                parser.getDistance() + "  •  " + parser.getDuration());

                        // Zoom the camera to show the entire route
                        zoomToRoute(routePoints, new LatLng(destLat, destLng));

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing directions", e);
                        tvRouteInfo.setText("Error parsing route. Showing straight line.");
                        drawStraightLine(destLat, destLng, destName);
                    }
                });
            }
        });
    }

    /**
     * Fallback: draws a straight line from current location to destination
     * (used when Directions API fails or returns no route).
     */
    private void drawStraightLine(double destLat, double destLng, String destName) {
        if (googleMap == null || currentLatLng == null) return;

        LatLng dest = new LatLng(destLat, destLng);

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(currentLatLng)
                .add(dest)
                .width(8f)
                .color(Color.rgb(234, 67, 53)) // Red dashed line for fallback
                .geodesic(true);

        currentRoutePolyline = googleMap.addPolyline(polylineOptions);

        // Calculate straight-line distance
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                currentLatLng.latitude, currentLatLng.longitude,
                destLat, destLng, results);
        String distText = results[0] > 1000
                ? String.format("%.1f km", results[0] / 1000)
                : String.format("%.0f m", results[0]);

        tvRouteInfo.setText("📍 " + destName + "  •  ~" + distText + " (straight line)");
        infoBar.setVisibility(View.VISIBLE);

        // Zoom to show both points
        List<LatLng> points = new ArrayList<>();
        points.add(currentLatLng);
        points.add(dest);
        zoomToRoute(points, dest);
    }

    /**
     * Adjusts the map camera to show the entire route.
     */
    private void zoomToRoute(List<LatLng> routePoints, LatLng destination) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        if (currentLatLng != null) boundsBuilder.include(currentLatLng);
        boundsBuilder.include(destination);
        for (LatLng point : routePoints) {
            boundsBuilder.include(point);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
    }

    /**
     * Clears the current route polyline and hides the info bar.
     */
    private void clearRoute() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        infoBar.setVisibility(View.GONE);
    }

    // ======================== SAVE & MANAGE LOCATIONS ========================

    /**
     * Gets the user's current GPS location and prompts for a name to save it.
     */
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name this location");

        final EditText input = new EditText(this);
        input.setHint("e.g., Home, Office, Coffee Shop");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "My Location";

            LocationEntity realLocation = new LocationEntity(name, lat, lng);
            locationViewModel.insert(realLocation);
            addGeofence(lat, lng, name);

            Toast.makeText(this, "Saved: " + name, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showOptionsDialog(LocationEntity location) {
        CharSequence[] options = new CharSequence[]{"Navigate Here", "Edit Name", "Delete Location"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(location.getName());

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Navigate in-app
                showDirectionsOnMap(location.getLatitude(), location.getLongitude(), location.getName());
            } else if (which == 1) {
                promptForEdit(location);
            } else if (which == 2) {
                locationViewModel.delete(location);
                removeGeofence(location.getName());
                Toast.makeText(this, "Location Deleted", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void promptForEdit(LocationEntity location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        final EditText input = new EditText(this);
        input.setText(location.getName());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                String oldName = location.getName();
                location.setName(newName);
                locationViewModel.update(location);

                // Re-register geofence with new name
                removeGeofence(oldName);
                addGeofence(location.getLatitude(), location.getLongitude(), newName);

                Toast.makeText(this, "Name Updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ======================== GEOFENCING ========================

    private void addGeofence(double lat, double lng, String locationName) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(locationName)
                .setCircularRegion(lat, lng, 200) // 200 meters radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                locationName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(this, aVoid ->
                            Toast.makeText(this, "Geofence set (200m radius)", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Geofence failed: " + e.getMessage()));
        }
    }

    private void removeGeofence(String requestId) {
        List<String> ids = new ArrayList<>();
        ids.add(requestId);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.removeGeofences(ids);
        }
    }

    // ======================== LOCATION SERVICE & RECEIVER ========================

    /**
     * Registers a BroadcastReceiver to get live location updates from
     * the LocationTrackingService foreground service.
     */
    private void setupLocationReceiver() {
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double lat = intent.getDoubleExtra("latitude", 0);
                double lng = intent.getDoubleExtra("longitude", 0);
                LatLng newPos = new LatLng(lat, lng);
                updateCurrentLocationMarker(newPos);
            }
        };

        IntentFilter filter = new IntentFilter("LOCATION_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationUpdateReceiver, filter);
        }
    }

    /**
     * Starts the foreground location tracking service.
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // ======================== PERMISSIONS ========================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for geofence alerts
            NotificationChannel geofenceChannel = new NotificationChannel(
                    "GEOFENCE_CHANNEL_ID",
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            geofenceChannel.setDescription("Alerts when you enter a saved location's radius");

            getSystemService(NotificationManager.class).createNotificationChannel(geofenceChannel);
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Android 13+ needs POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted, request background location and start service
            requestBackgroundLocationPermission();
            startTrackingService();
        }
    }

    /**
     * Background location permission must be requested separately on Android 10+.
     */
    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Show rationale first
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Needed")
                        .setMessage("This app needs background location access to notify you " +
                                "when you're near a saved location, even when the app is closed.\n\n" +
                                "Please select \"Allow all the time\" on the next screen.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    BACKGROUND_LOCATION_REQUEST_CODE);
                        })
                        .setNegativeButton("Skip", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Enable the blue dot on the map
                if (googleMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                    }
                }
                getInitialLocation();
                requestBackgroundLocationPermission();
                startTrackingService();
            } else {
                Toast.makeText(this, "Location permission is required for this app to work.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Background location enabled! Geofences will work when app is closed.",
                        Toast.LENGTH_LONG).show();
                startTrackingService();
            } else {
                Toast.makeText(this, "Background location denied. Geofence notifications won't work when app is closed.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ======================== LIFECYCLE ========================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationUpdateReceiver != null) {
            unregisterReceiver(locationUpdateReceiver);
        }
        // NOTE: We intentionally do NOT stop the service here.
        // The service keeps running to monitor geofences even when the app is closed.
    }
}