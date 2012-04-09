package edu.mit.media.funf.probe.builtin;

import static edu.mit.media.funf.Utils.TAG;
import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ActivityKeys;

@DefaultSchedule(period=120, duration=15)
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
public class ActivityProbe extends Base implements ContinuousProbe, PassiveProbe, ActivityKeys {

	@Configurable
	private double interval = 1.0;
	
	private long startTime;
	private ActivityCounter activityCounter = new ActivityCounter();
	
	@Override
	protected void onEnable() {
		super.onEnable();
		getAccelerometerProbe().registerPassiveListener(activityCounter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getAccelerometerProbe().registerListener(activityCounter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getAccelerometerProbe().unregisterListener(activityCounter);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		getAccelerometerProbe().unregisterPassiveListener(activityCounter);
	}

	private AccelerometerSensorProbe getAccelerometerProbe() {
		return (AccelerometerSensorProbe)getProbeFactory().getProbe(AccelerometerSensorProbe.class, null);
	}

	
	private class ActivityCounter implements DataListener {
		private double intervalStartTime;
		private float varianceSum;
		private float avg;
		private float sum;
		private int count;
		
		private void reset() {
			// If more than an interval away, start a new scan
			varianceSum = avg = sum = count = 0;
		}
		
		private void intervalReset() {
			Log.d(TAG, "interval RESET");
			// Calculate activity and reset
			JsonObject data = new JsonObject();
			if (varianceSum >= 10.0f) {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_HIGH);
			} else if (varianceSum < 10.0f && varianceSum > 3.0f) {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_LOW);
			} else {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_NONE);
			}
			sendData(data);
			varianceSum = avg = sum = count = 0;
		}
		
		private void update(float x, float y, float z) {
			//Log.d(TAG, "UPDATE:(" + x + "," + y + "," + z + ")");
			// Iteratively calculate variance sum
			count++;
			float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
			float newAvg = (count - 1)*avg/count + magnitude/count;
			float deltaAvg = newAvg - avg;
			varianceSum += (magnitude - newAvg) * (magnitude - newAvg)
				- 2*(sum - (count-1)*avg) 
				+ (count - 1) *(deltaAvg * deltaAvg);
			sum += magnitude;
			avg = newAvg;
			//Log.d(TAG, "UPDATED VALUES:(count, varianceSum, sum, avg) " + count + ", " + varianceSum+ ", " + sum+ ", " + avg);
		}
		

		@Override
		public void onDataReceived(Uri completeProbeUri, JsonObject data) {
			double timestamp = data.get(TIMESTAMP).getAsDouble();
			Log.d(TAG, "Starttime: " + startTime + " intervalStartTime: " + intervalStartTime);
			Log.d(TAG, "RECEIVED:" + timestamp);
			if (timestamp >= intervalStartTime + 2 * interval) {
				Log.d(TAG, "RESET:" + timestamp);
				reset();
			} else if (timestamp >= intervalStartTime + interval) {
				Log.d(TAG, "interval Reset:" + timestamp);
				intervalReset();
			}
			float x = data.get(AccelerometerSensorProbe.X).getAsFloat();
			float y = data.get(AccelerometerSensorProbe.Y).getAsFloat();
			float z = data.get(AccelerometerSensorProbe.Z).getAsFloat();
			update(x, y, z);
		}

		@Override
		public void onDataCompleted(Uri completeProbeUri, JsonElement checkpoint) {
			// Do nothing
		}
	}
}
