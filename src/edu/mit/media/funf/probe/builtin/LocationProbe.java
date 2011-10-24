/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe.builtin;

import java.lang.reflect.Field;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;

public class LocationProbe extends Probe implements LocationKeys {

	public static final long SIGNIFICANT_TIME_DIFFERENCE = 2*60*1000; // 2 minutes
	// TODO: May turn MAX_DURATION into duration parameter
	public static final long DEFAULT_DURATION = 2*60; // 2 minutes
	public static final long DEFAULT_PERIOD = 60L * 30L; // 30 min;
	// TODO: Turn GOOD_ENOUGH_ACCURACY into a parameter
	public static final float GOOD_ENOUGH_ACCURACY = 80.0f;
	
	private LocationManager mLocationManager;
	private ProbeLocationListener listener;
	private ProbeLocationListener passiveListener;
	private Location latestLocation;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.PERIOD, DEFAULT_PERIOD),
			new Parameter(Parameter.Builtin.DURATION, DEFAULT_DURATION),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L)
			// TODO: come back to configuration parameters such as desiredAccuracy or duration
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
	
	private Location bestCachedLocation() {
		Location lastKnownGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastKnownNetLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location bestCachedLocation = lastKnownGpsLocation;
		if (bestCachedLocation == null || 
				(lastKnownNetLocation != null && lastKnownNetLocation.getTime() > bestCachedLocation.getTime())) {
			bestCachedLocation = lastKnownNetLocation;
		}
		return bestCachedLocation;
	}

	@Override
	protected void onEnable() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		latestLocation = bestCachedLocation();
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
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
	}
	

	@Override
	public void onStop() {
		mLocationManager.removeUpdates(listener);
		sendProbeData();
	}

	@Override
	public void sendProbeData() {
		if (latestLocation != null) {
			Bundle data = new Bundle();
			data.putParcelable(LOCATION, latestLocation);
			sendProbeData(Utils.millisToSeconds(latestLocation.getTime()), data);
		}
	}

	private class ProbeLocationListener implements LocationListener{
		
		public void onLocationChanged(Location newLocation) { 
			if (newLocation == null || (newLocation.getLatitude() == 0.0 && newLocation.getLongitude() == 0.0)){ 
			// Hack to filter out 0.0,0.0 locations 
			    return; 
			} 
			Log.i(TAG, "New location to be evaluated: " + newLocation.getAccuracy() + "m @ " + newLocation.getTime() );
			if (isBetterThanCurrent(newLocation)) {
				latestLocation = newLocation;
				// If not running then start a timer to send out the best location get in the next default duration
				Log.i(TAG, "Is Running: " + isRunning());
				if (latestLocation.hasAccuracy() && latestLocation.getAccuracy() < GOOD_ENOUGH_ACCURACY) {
					if (isRunning()) {
						Log.i(TAG, "Good enough stop");
						stop();
					} else {
						// TODO: set a timer for passive listened locations so they have a DURATION aspect like active scans
						Log.i(TAG, "Passive location data send");
						sendProbeData();
					}
				}
			}
			
		} 
		
		private boolean isBetterThanCurrent(Location newLocation) {
			if (latestLocation == null) {
				return true;
			}
			long timeDiff = newLocation.getTime() - latestLocation.getTime();
			Log.i(TAG, "TIME DIFFERENCE: " + timeDiff);
			Log.i(TAG, "Old accuracy: " + latestLocation + " New Accuracy: " + newLocation.getAccuracy());
			return timeDiff > SIGNIFICANT_TIME_DIFFERENCE || 
				(newLocation.getAccuracy() <= latestLocation.getAccuracy());
		}
		
		public void onProviderEnabled(String provider) { 
		} 

		public void onProviderDisabled(String provider) { 
		} 

		public void onStatusChanged(String provider, int status, Bundle extras){ 
			if (status == LocationProvider.OUT_OF_SERVICE) { 
		        Log.i(TAG, "location provider out of service: "+provider);
		    }else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE){
		    	Log.i(TAG, "location provider temp unavailable: "+provider);
		    } 
		} 
	}



}
