/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.media.funf.probe.builtin;

import java.lang.reflect.Field;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;

/**
 * Sends all location points gathered by system.
 * @author alangardner
 *
 */
@DisplayName("Continuous Location Probe")
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval=1800)
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
		gson = getGsonBuilder().addSerializationExclusionStrategy(new LocationExclusionStrategy()).create();
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
	
    public class LocationExclusionStrategy implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> cls) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
        	String name = f.getName();
            return (f.getDeclaringClass() == Location.class && 
            			(name.equals("mResults") 
            				|| name.equals("mDistance") 
            				|| name.equals("mInitialBearing") 
            				|| name.equals("mLat1") 
            				|| name.equals("mLat2") 
            				|| name.equals("mLon1") 
            				|| name.equals("mLon2") 
            				|| name.equals("mLon2") 
            				)
            		);
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
