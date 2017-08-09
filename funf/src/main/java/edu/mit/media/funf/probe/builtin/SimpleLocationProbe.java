/**
 * 
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
 * 
 */
package edu.mit.media.funf.probe.builtin;



import java.math.BigDecimal;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

/**
 * Filters the verbose location set for the most accurate location within a max wait time,
 * ending early if it finds a location that has at most the goodEnoughAccuracy.
 * Useful for sparse polling of location to limit battery usage.
 * @author alangardner
 *
 */
@DisplayName("Simple Location Probe")
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval=1800)
@RequiredProbes(LocationProbe.class)
public class SimpleLocationProbe extends Base implements PassiveProbe, LocationKeys {

	@Configurable
	private BigDecimal maxWaitTime = BigDecimal.valueOf(120);
	
	@Configurable
	private BigDecimal maxAge =  BigDecimal.valueOf(120); 
	
	@Configurable
	private BigDecimal goodEnoughAccuracy = BigDecimal.valueOf(80);

	@Configurable
	private boolean useGps = true;
	
	@Configurable
	private boolean useNetwork = true;


	private LocationProbe locationProbe;
	
	private BigDecimal startTime;
	private IJsonObject bestLocation;
	
	private Runnable sendLocationRunnable = new Runnable() {
		@Override
		public void run() {
			sendCurrentBestLocation();
		}
	};
	
	private DataListener listener = new DataListener() {
		
		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
			Log.d(LogUtil.TAG, "SimpleLocationProbe received data: " + data.toString());
			if (startTime == null) {
				startTime = TimeUtil.getTimestamp();
				getHandler().postDelayed(sendLocationRunnable, TimeUtil.secondsToMillis(maxWaitTime));
			}
			if (isBetterThanCurrent(data)) {
				Log.d(LogUtil.TAG, "SimpleLocationProbe evaluated better location.");
				bestLocation = data;
			}
			if (goodEnoughAccuracy != null && bestLocation.get(ACCURACY).getAsDouble() < goodEnoughAccuracy.doubleValue()) {
				Log.d(LogUtil.TAG, "SimpleLocationProbe evaluated good enough location.");
				if (getState() == State.RUNNING) { // Actively Running
					stop();
				} else if (getState() == State.ENABLED) { // Passive listening
					// TODO: do we want to prematurely end this, or wait for the full duration
					// Things to consider: 
					// - the device falling to sleep before we send
					// - too much unrequested data if we send all values within accuracy limits 
					// (this will restart immediately if more passive data continues to come in)
					getHandler().removeCallbacks(sendLocationRunnable);
					sendCurrentBestLocation();
				}
			}
		}
		
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
		}
	};
	
	private void sendCurrentBestLocation() {
		Log.d(LogUtil.TAG, "SimpleLocationProbe sending current best location.");
		if (bestLocation != null) {
			JsonObject data = bestLocation.getAsJsonObject();
			data.remove(PROBE); // Remove probe so that it fills with our probe name
			sendData(data);
		}
		startTime = null;
		bestLocation = null;
	}
	
	private boolean isBetterThanCurrent(IJsonObject newLocation) {
		BigDecimal age = startTime.subtract(newLocation.get(TIMESTAMP).getAsBigDecimal());
		return bestLocation == null || 
				(age.doubleValue() < maxAge.doubleValue() && 
						bestLocation.get(ACCURACY).getAsDouble() > newLocation.get(ACCURACY).getAsDouble());
	}
	
	@Override
	protected void onEnable() {
		super.onEnable();
		JsonObject config = new JsonObject();
		if (!useGps) {
			config.addProperty("useGps", false);
		}
		if (!useNetwork) {
			config.addProperty("useNetwork", false);
		}
		locationProbe = getGson().fromJson(config, LocationProbe.class);
		locationProbe.registerPassiveListener(listener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LogUtil.TAG, "SimpleLocationProbe starting, registering listener");
		startTime = TimeUtil.getTimestamp();
		locationProbe.registerListener(listener);
		getHandler().sendMessageDelayed(getHandler().obtainMessage(STOP_MESSAGE), TimeUtil.secondsToMillis(maxWaitTime));
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LogUtil.TAG, "SimpleLocationProbe stopping");
		getHandler().removeMessages(STOP_MESSAGE);
		locationProbe.unregisterListener(listener);
		sendCurrentBestLocation();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		locationProbe.unregisterPassiveListener(listener);
	}
	
	
	
}
