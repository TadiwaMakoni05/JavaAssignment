package com.example.javaassignment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error code: " + geofencingEvent.getErrorCode());
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "User entered the geofence!");

            // 1. Get the Notification Manager
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // 2. Build the visual Notification
            // Note: android.R.drawable.ic_dialog_map is a built-in Android icon so we don't have to draw one!
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "GEOFENCE_CHANNEL_ID")
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle("Destination Reached!")
                    .setContentText("You have entered the 200m radius of your saved location.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true); // Dismisses when swiped

            // 3. Fire it! (200 is just a random ID number for this specific notification)
            notificationManager.notify(200, builder.build());

        } else {
            Log.e("GeofenceReceiver", "Invalid transition type: " + geofenceTransition);
        }
    }
}