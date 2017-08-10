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
