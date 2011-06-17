/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe.builtin;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;

public class LocationProbe extends Probe {

	private LocationManager mLocationManager;
	private ProbeLocationListener listener;
	private ProbeLocationListener passiveListener;
	private Location latestLocation;
	private Timer timer;

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.START, 0L),  // No start by default
			new Parameter(SystemParameter.PERIOD, 0L),//*60L),	// Run every 30 min by default
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{
			android.Manifest.permission.ACCESS_COARSE_LOCATION,
			android.Manifest.permission.ACCESS_FINE_LOCATION
		};
	}
	

	@Override
	public String[] getRequiredFeatures() {
		return new String[]{};
	}
	

	@Override
	protected void onEnable() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		listener = new ProbeLocationListener();
		passiveListener = new ProbeLocationListener();
		String passiveProvider = getPassiveProvider();
		if (passiveProvider != null) {
			mLocationManager.requestLocationUpdates(getPassiveProvider(), 0, 0, passiveListener);
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

	@Override
	protected void onDisable() {
		mLocationManager.removeUpdates(passiveListener);
	}

	public void onRun(Bundle params) {
		if (timer == null) {
			timer = new Timer();
			// Stop after 10 seconds
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					stop();
				}
			}, 10000);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
		}
	}
	

	@Override
	public void onStop() {
		timer.cancel();
		mLocationManager.removeUpdates(listener);
		sendProbeData();
		timer = null;
	}

	@Override
	public void sendProbeData() {
		if (latestLocation != null) {
			Bundle data = new Bundle();
			data.putParcelable("LOCATION", latestLocation);
			sendProbeData(latestLocation.getTime(), new Bundle(), data);
		}
	}

	private class ProbeLocationListener implements LocationListener{
		
		public void onLocationChanged(Location newLocation) { 
			if (newLocation.getLatitude() == 0.0 && newLocation.getLongitude() == 0.0){ 
			// Hack to filter out 0.0,0.0 locations 
			    return; 
			} 
			newLocation.setTime(System.currentTimeMillis());
			// Ignorant version that just takes the latest
			latestLocation = newLocation;
		} 
		
		public void onProviderEnabled(String provider) { 
		} 

		public void onProviderDisabled(String provider) { 
		} 

		public void onStatusChanged(String provider, int status, Bundle extras){ 
		} 
	}



}
