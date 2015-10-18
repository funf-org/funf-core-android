package edu.mit.media.funf.data;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created by arks on 10/16/15.
 */
public class Geofencer {

    //TODO broadcast recording/not recording

    @Configurable
    protected Integer version = 0;

    @Configurable
    protected List<JsonElement> fences = new ArrayList<JsonElement>();

    @Configurable
    protected Double minLocationAccuracy = 80.0;

    @Configurable
    protected Long minLocationFreshness = 1L;

    @Configurable
    protected Long timeWindow = 10L;


   private Set<String> acceptedLocationProbes =
           new HashSet<String>(Arrays.asList("edu.mit.media.funf.probe.builtin.LocationProbe", "edu.mit.media.funf.probe.builtin.SimpleLocationProbe"));



    public Geofencer() {
        Log.i(LogUtil.TAG, "Creating geofencer " + version);
    }

    public boolean shouldSaveData(String name, IJsonObject data) {
        if (acceptedLocationProbes.contains(name)) {
            updateVisits(name, data);
        }

        return checkFences(timestampToMillis(data.get("timestamp").getAsLong()));
    }

    public boolean shouldSaveData(Long timestamp) {
        return checkFences(timestamp);
    }


    private void updateVisits(String name, IJsonObject data) {

        if (name.equals("edu.mit.media.funf.probe.builtin.LocationProbe")) {
            Double accuracy = data.get("mAccuracy").getAsDouble();
            Double lat = data.get("mLatitude").getAsDouble();
            Double lon = data.get("mLongitude").getAsDouble();
            Long timestamp = timestampToMillis(data.get("mTime").getAsLong());

            //TODO discard mock locations
            if (accuracy > minLocationAccuracy) return;
            if ((System.currentTimeMillis()) > timestamp + minLocationFreshness*60*1000) return;

            //TODO graceful error handling
            for (JsonElement fence: fences) {
                JsonObject fenceObject = fence.getAsJsonObject();
                Double fenceLatitude = fenceObject.get("latitude").getAsDouble();
                Double fenceLongitude = fenceObject.get("longitude").getAsDouble();
                Double fenceRadius = fenceObject.get("radius").getAsDouble();

                Double distance = haversine(lat, lon, fenceLatitude, fenceLongitude);

                if (distance < fenceRadius) {
                    updateVisit(fenceLatitude, fenceLongitude, fenceRadius, timestamp);
                }
            }

        }
    }

    private void updateVisit(Double fenceLatitude, Double fenceLongitude, Double fenceRadius, Long timestamp) {
        String fenceID = fenceToString(fenceLatitude, fenceLongitude, fenceRadius);
        SharedPreferences sharedPreferences = FunfManager.context.getSharedPreferences("funf_geofence", FunfManager.context.MODE_PRIVATE);

        Long previousTimestamp = sharedPreferences.getLong(fenceID, 0);

        if (timestamp > previousTimestamp) {
            sharedPreferences.edit().putLong(fenceID, timestamp).commit();
        }

    }

    private String fenceToString(Double fenceLatitude, Double fenceLongitude, Double fenceRadius) {
        return IOUtil.md5(""+fenceLatitude + ";"+fenceLongitude+";"+fenceRadius);
    }

    private boolean checkFences(Long timestamp) {
        //TODO in the future this should keep the last 24h or so of the fence history so we can
        //put data from the past

        if (fences.size() == 0) return true;

        for (JsonElement fence: fences) {
            JsonObject fenceObject = fence.getAsJsonObject();
            Double fenceLatitude = fenceObject.get("latitude").getAsDouble();
            Double fenceLongitude = fenceObject.get("longitude").getAsDouble();
            Double fenceRadius = fenceObject.get("radius").getAsDouble();

            String fenceID = fenceToString(fenceLatitude, fenceLongitude, fenceRadius);
            SharedPreferences sharedPreferences = FunfManager.context.getSharedPreferences("funf_geofence", FunfManager.context.MODE_PRIVATE);
            Long lastVisitTimestamp = sharedPreferences.getLong(fenceID, 0);

            if ((timestamp < (lastVisitTimestamp + timeWindow*60*1000)) && (timestamp > (lastVisitTimestamp - timeWindow*60*1000)) ) return true;

        }

        return false;
    }

    public Integer getVersion() {
        return this.version;
    }

    private Long timestampToMillis(Long timestamp) {
        if (timestamp < 9999999999L) return timestamp*1000;
        return timestamp;
    }

    private Double haversine(double lat1, double lon1, double lat2, double lon2) {
        Double R = 6372.8; // In kilometers
        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        Double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        Double c = 2 * Math.asin(Math.sqrt(a));
        return R * c * 1000;
    }

    public List<JsonElement> getFences() {
        return this.fences;
    }

}
