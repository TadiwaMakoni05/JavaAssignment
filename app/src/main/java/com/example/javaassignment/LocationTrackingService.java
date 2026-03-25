package com.example.javaassignment;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

/**
 * Foreground service that:
 *  1. Continuously tracks the user's GPS location.
 *  2. Monitors geofences even when the app is closed.
 *  3. Sends notifications when the user enters a saved location's 200m radius.
 */
public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackService";
    private static final String CHANNEL_ID = "TRACKING_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GeofencingClient geofencingClient;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as a foreground service with a persistent notification
        Notification notification = buildForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Begin location updates
        startLocationUpdates();

        // Re-register geofences for all saved locations
        reRegisterGeofences();

        // If the system kills the service, restart it
        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // every 10 seconds
                .setMinUpdateIntervalMillis(5000)       // fastest: every 5 seconds
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
                    // Broadcast location to the activity if it's open
                    Intent broadcastIntent = new Intent("LOCATION_UPDATE");
                    broadcastIntent.putExtra("latitude", location.getLatitude());
                    broadcastIntent.putExtra("longitude", location.getLongitude());
                    sendBroadcast(broadcastIntent);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    /**
     * Re-registers geofences for all saved locations from the database.
     * This ensures geofences survive app/service restarts.
     */
    private void reRegisterGeofences() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        LocationRepository.databaseWriteExecutor.execute(() -> {
            // We use a synchronous query on a background thread
            List<LocationEntity> locations = db.locationDao().getAllLocationsList();
            if (locations == null || locations.isEmpty()) return;

            for (LocationEntity loc : locations) {
                addGeofenceInternal(loc.getLatitude(), loc.getLongitude(), loc.getName());
            }
        });
    }

    private void addGeofenceInternal(double lat, double lng, String locationName) {
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, locationName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence registered for: " + locationName))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to register geofence: " + e.getMessage()));
        }
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Tracker Active")
                .setContentText("Tracking your location and monitoring saved places")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Persistent notification for GPS tracking service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
