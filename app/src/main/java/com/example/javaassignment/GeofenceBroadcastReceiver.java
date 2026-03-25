package com.example.javaassignment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * BroadcastReceiver that fires when the user enters a geofenced area.
 * This works even when the app is not open because it is registered in the manifest
 * and the geofences are maintained by the LocationTrackingService foreground service.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error code: " + geofencingEvent.getErrorCode());
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the names of the geofences that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            StringBuilder locationNames = new StringBuilder();

            if (triggeringGeofences != null) {
                for (int i = 0; i < triggeringGeofences.size(); i++) {
                    if (i > 0) locationNames.append(", ");
                    locationNames.append(triggeringGeofences.get(i).getRequestId());
                }
            }

            String names = locationNames.length() > 0 ? locationNames.toString() : "a saved location";

            Log.d(TAG, "User entered geofence: " + names);

            // Build and show the notification
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Create an intent to open the app when notification is tapped
            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, openAppIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "GEOFENCE_CHANNEL_ID")
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle("📍 You're near: " + names)
                    .setContentText("You are within 200m of your saved location.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("You have entered the 200-meter radius of \"" + names + "\". " +
                                    "Tap to open the app and view directions."))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL); // Sound, vibration, lights

            // Use a unique notification ID based on the location name hash
            int notificationId = names.hashCode();
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
            }

        } else {
            Log.e(TAG, "Invalid transition type: " + geofenceTransition);
        }
    }
}