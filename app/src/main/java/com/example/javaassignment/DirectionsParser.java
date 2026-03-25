package com.example.javaassignment;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Google Directions API JSON response to extract:
 *  - The list of LatLng points for drawing a polyline on the map.
 *  - Distance and duration text for display.
 */
public class DirectionsParser {

    private List<LatLng> points;
    private String distance;
    private String duration;

    public DirectionsParser(String jsonResponse) throws JSONException {
        points = new ArrayList<>();
        parse(jsonResponse);
    }

    private void parse(String jsonResponse) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        JSONArray routes = json.getJSONArray("routes");

        if (routes.length() == 0) {
            distance = "No route found";
            duration = "";
            return;
        }

        JSONObject route = routes.getJSONObject(0);

        // Get the overview polyline (encoded polyline string)
        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
        String encodedPolyline = overviewPolyline.getString("points");
        points = decodePolyline(encodedPolyline);

        // Get distance and duration from the first leg
        JSONArray legs = route.getJSONArray("legs");
        if (legs.length() > 0) {
            JSONObject leg = legs.getJSONObject(0);
            distance = leg.getJSONObject("distance").getString("text");
            duration = leg.getJSONObject("duration").getString("text");
        }
    }

    /**
     * Decodes an encoded polyline string into a list of LatLng points.
     * Based on Google's Polyline Algorithm.
     */
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }

        return poly;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }
}
