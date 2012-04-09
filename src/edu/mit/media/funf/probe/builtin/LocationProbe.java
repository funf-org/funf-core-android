package edu.mit.media.funf.probe.builtin;

import java.lang.reflect.Field;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.DecimalTimeUnit;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;

/**
 * Sends all location points gathered by system.
 * @author alangardner
 *
 */
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@DefaultSchedule(period=1800)
public class LocationProbe extends Base implements ContinuousProbe, PassiveProbe, LocationKeys {

	@Configurable
	private boolean useGps = true;
	
	@Configurable
	private boolean useNetwork = true;
	
	@Configurable
	private boolean useCache = true;
	
	private Gson gson;
	private LocationManager mLocationManager;
	private LocationListener listener = new ProbeLocationListener();
	private LocationListener passiveListener = new ProbeLocationListener();
	
	@Override
	protected void onEnable() {
		super.onEnable();
		gson = getGson();
		mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		String passiveProvider = getPassiveProvider();
		if (passiveProvider != null) {
			mLocationManager.requestLocationUpdates(getPassiveProvider(), 0, 0, passiveListener);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (useGps) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
		}
		if (useNetwork) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
		}
		if (useCache) {
			listener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
			listener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		}
		if (!useGps && ! useNetwork) {
			stop();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mLocationManager.removeUpdates(listener);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		mLocationManager.removeUpdates(passiveListener);
	}

	private class ProbeLocationListener implements LocationListener{

		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				String provider = location.getProvider();
				if (provider == null 
						|| (useGps && LocationManager.GPS_PROVIDER.equals(provider))
						|| (useNetwork && LocationManager.NETWORK_PROVIDER.equals(provider))) {
					JsonObject data = gson.toJsonTree(location).getAsJsonObject();
					data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(data.get("mTime").getAsBigDecimal()));
					sendData(gson.toJsonTree(location).getAsJsonObject());
				}
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
		
	}
	
	/**
	 * Supporting API level 7 which does not have PASSIVE provider
	 * @return
	 */
	private String getPassiveProvider() {
		try {
			Field passiveProviderField = LocationManager.class.getDeclaredField("PASSIVE_PROVIDER");
			return (String)passiveProviderField.get(null);
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}
	
}
