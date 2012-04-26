package edu.mit.media.funf.probe.builtin;



import java.math.BigDecimal;

import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
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
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@DefaultSchedule(period=1800)
@RequiredProbes(LocationProbe.class)
public class SimpleLocationProbe extends Base implements PassiveProbe, LocationKeys {

	@ConfigurableField
	private BigDecimal maxWaitTime = BigDecimal.valueOf(120);
	
	@ConfigurableField
	private BigDecimal maxAge =  BigDecimal.valueOf(120); 
	
	@ConfigurableField
	private BigDecimal goodEnoughAccuracy = BigDecimal.valueOf(80);

	@ConfigurableField
	private boolean useGps = true;
	
	@ConfigurableField
	private boolean useNetwork = true;


	private LocationProbe locationProbe;
	
	private BigDecimal startTime;
	private JsonObject bestLocation;
	
	private Runnable sendLocationRunnable = new Runnable() {
		@Override
		public void run() {
			sendCurrentBestLocation();
		}
	};
	
	private DataListener listener = new DataListener() {
		
		@Override
		public void onDataReceived(Uri completeProbeUri, JsonObject data) {
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
		public void onDataCompleted(Uri completeProbeUri, JsonElement checkpoint) {
		}
	};
	
	private void sendCurrentBestLocation() {
		Log.d(LogUtil.TAG, "SimpleLocationProbe sending current best location.");
		if (bestLocation != null) {
			bestLocation.remove(PROBE); // Remove probe so that it fills with our probe name
			sendData(bestLocation);
		}
		startTime = null;
		bestLocation = null;
	}
	
	private boolean isBetterThanCurrent(JsonObject newLocation) {
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
		locationProbe = getProbeFactory().getProbe(LocationProbe.class, config);
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
