package com.example.javaassignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationHolder> {

    private List<LocationEntity> locations = new ArrayList<>();

    // 1. Create variables to hold our custom listeners
    private OnLocationClickListener listener;
    private OnLocationLongClickListener longClickListener; // <-- NEW

    // 2. Define the Interfaces (The walkie-talkie channels)
    public interface OnLocationClickListener {
        void onLocationClick(LocationEntity location);
    }

    // <-- NEW: Interface for Press and Hold
    public interface OnLocationLongClickListener {
        void onLocationLongClick(LocationEntity location);
    }

    // 3. Create methods so MainActivity can hand the adapter the listeners
    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.listener = listener;
    }

    // <-- NEW: Setter for the Long Click
    public void setOnLocationLongClickListener(OnLocationLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public LocationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationHolder holder, int position) {
        LocationEntity currentLocation = locations.get(position);

        // Note: If your LocationEntity uses getLocationName() instead of getName(), change it here!
        holder.textViewName.setText(currentLocation.getName());

        holder.textViewCoordinates.setText("Lat: " + currentLocation.getLatitude() +
                ", Lng: " + currentLocation.getLongitude());
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void setLocations(List<LocationEntity> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    // The ViewHolder holds the UI elements for a single row
    class LocationHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewCoordinates;

        public LocationHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textLocationName);
            textViewCoordinates = itemView.findViewById(R.id.textCoordinates);

            // 4. Set the standard click listener (Tap to Navigate)
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onLocationClick(locations.get(position));
                    }
                }
            });

            // 5. Set the long click listener (Press and Hold to Edit/Delete) <-- NEW
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                        longClickListener.onLocationLongClick(locations.get(position));
                        return true; // 'true' tells Android we successfully handled the long click
                    }
                    return false;
                }
            });
        }
    }
}