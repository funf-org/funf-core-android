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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeScheduler;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ActivityKeys;

public class ActivityProbe extends Probe implements ActivityKeys {

	private static final long DEFAULT_DURATION = 15L;
	private static final long DEFAULT_PERIOD = 120L;
	private static final long INTERVAL = 1L;
	private static final String DELEGATE_PROBE_NAME = AccelerometerSensorProbe.class.getName();
	
	//private long duration = 0L;
	private Handler handler;

	private long startTime;
	private int intervalCount;
	private int lowActivityIntervalCount;
	private int highActivityIntervalCount;
	
	private ActivityCounter activityCounter;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.DURATION, DEFAULT_DURATION),
			new Parameter(Parameter.Builtin.PERIOD, DEFAULT_PERIOD),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return new String[]{
				"android.hardware.sensor.accelerometer"
			};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}

	@Override
	protected String getDisplayName() {
		return "Activity Level Probe";
	}
	
	@Override
	protected void onHandleCustomIntent(Intent intent) {
		if (Probe.ACTION_DATA.equals(intent.getAction()) && DELEGATE_PROBE_NAME.equals(intent.getStringExtra(PROBE))) {
			if (handler == null) {
				handler = new Handler(); // Make sure handler is created on message thread
			}
			if (activityCounter == null) {
				activityCounter = new ActivityCounter();
			}
			if (!activityCounter.isRunning()) { // Ensure  probe stays alive
				run();
			}
			activityCounter.handleAccelerometerData(intent.getExtras());
		}
	}
	
	private class ActivityCounter {
		private long intervalStartTime;
		private float varianceSum;
		private float avg;
		private float sum;
		private int count;
		
		private Runnable disableRunnable;
		
		public boolean isRunning() {
			return disableRunnable != null;
		}
		
		private void reset(long timestamp) {
			Log.d(TAG, "RESET:" + timestamp);
			// If more than an INTERVAL away, start a new scan
			startTime = intervalStartTime = timestamp;
			varianceSum = avg = sum = count = 0;
			intervalCount = 1;
			lowActivityIntervalCount = 0;
			highActivityIntervalCount = 0;
		}
		
		private void resetDisableTimer() {
			if (disableRunnable == null) {
				Log.d(TAG, "CREATING SEND DATA RUNNABLE");
				disableRunnable = new Runnable() {
					public void run() {
						Log.d(TAG, "SENDING DATA");
						sendProbeData();
						disableRunnable = null;
						disable();
					}
				};
			}
			handler.removeCallbacks(disableRunnable);
			handler.postDelayed(disableRunnable, Utils.secondsToMillis(2 * INTERVAL));
		}
		
		private void intervalReset() {
			Log.d(TAG, "INTERVAL RESET");
			// Calculate activity and reset
			intervalCount++;
			if (varianceSum >= 10.0f) {
				highActivityIntervalCount++;
			} else if (varianceSum < 10.0f && varianceSum > 3.0f) {
				lowActivityIntervalCount++;
			}
			intervalStartTime += INTERVAL; // Ensure 1 second intervals
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
		
		public void handleAccelerometerData(Bundle data) {
			long timestamp = data.getLong(TIMESTAMP, 0L);
			Log.d(TAG, "Starttime: " + startTime + " IntervalStartTime: " + intervalStartTime);
			Log.d(TAG, "RECEIVED:" + timestamp);
			if (!this.isRunning()
					//|| timestamp > startTime + duration // we don't know how to determine duration anymore, since onRun is never called
					|| timestamp >= intervalStartTime + 2 * INTERVAL) {
				Log.d(TAG, "RESET:" + timestamp);
				reset(timestamp);
			} else if (timestamp >= intervalStartTime + INTERVAL) {
				Log.d(TAG, "Interval Reset:" + timestamp);
				intervalReset();
			}
			resetDisableTimer();
			
			long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
			//int[] accuracy = data.getIntArray("ACCURACY");
			float[] x = data.getFloatArray("X");
			float[] y = data.getFloatArray("Y");
			float[] z = data.getFloatArray("Z");
			for (int i=0; i<eventTimestamp.length; i++) {
				update(x[i], y[i], z[i]);
			}
		}
	}

	@Override
	protected void onEnable() {
		// Nothing
	}


	@Override
	protected void onDisable() {
		// Nothing
	}

	
	@Override
	public void onRun(Bundle params) {
		// Nothing
	}

	@Override
	public void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putInt(TOTAL_INTERVALS, intervalCount);
		data.putInt(LOW_ACTIVITY_INTERVALS, lowActivityIntervalCount);
		data.putInt(HIGH_ACTIVITY_INTERVALS, highActivityIntervalCount);
		Log.d(TAG, "(" + lowActivityIntervalCount + " Low Active / " + intervalCount + "Total)");
		Log.d(TAG, "(" + highActivityIntervalCount + " High Active / " + intervalCount + "Total)");
		sendProbeData(startTime, data); // Timestamp already in seconds
	}
	
	@Override
	protected ProbeScheduler getScheduler() {
		return new DelegateProbeScheduler(AccelerometerSensorProbe.class);
	}

	@Override
	public boolean isEnabled() {
		return activityCounter != null && activityCounter.isRunning();
	}

	@Override
	public boolean isRunning() {
		return activityCounter != null && activityCounter.isRunning();
	}
	

}
