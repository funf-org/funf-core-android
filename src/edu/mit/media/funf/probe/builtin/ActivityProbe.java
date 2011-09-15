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

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import edu.mit.media.funf.OppProbe;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.client.ProbeCommunicator;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ActivityKeys;

public class ActivityProbe extends Probe implements ActivityKeys {

	private static long DEFAULT_DURATION = 5L;
	private static long DEFAULT_PERIOD = 60L;
	private static long INTERVAL = 1000L;  // Interval over which we calculate activity
	
	private long duration = 0L;
	private IntentFilter accelerometerProbeBroadcastFilter;
	private BroadcastReceiver accelerometerProbeListener;
	private Handler handler;

	private long startTime;
	private int intervalCount;
	private int extremeIntervalCount;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Probe.SystemParameter.DURATION, DEFAULT_DURATION, true),
			new Parameter(Probe.SystemParameter.PERIOD, DEFAULT_PERIOD, true),
			new Parameter(SystemParameter.START, 0L, true),
			new Parameter(SystemParameter.END, 0L, true)
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
	protected void onEnable() {
		handler = new Handler();
		accelerometerProbeBroadcastFilter = new IntentFilter(OppProbe.getDataAction(AccelerometerSensorProbe.class));
		accelerometerProbeListener = new BroadcastReceiver() {

			private long intervalStartTime;
			private float varianceSum;
			private float avg;
			private float sum;
			private int count;
			
			private Runnable sendRunnable;
			
			private void reset(long timestamp) {
				Log.d(TAG, "RESET:" + timestamp);
				// If more than an INTERVAL away, start a new scan
				startTime = intervalStartTime = timestamp;
				varianceSum = avg = sum = count = 0;
				intervalCount = 1;
				extremeIntervalCount = 0;
				
				// start timer to send results
				if (sendRunnable == null) {
					Log.d(TAG, "CREATING SEND DATA RUNNABLE");
					sendRunnable = new Runnable() {
						public void run() {
							Log.d(TAG, "SENDING DATA");
							sendProbeData();
							sendRunnable = null;
							stop();
						}
					};
					handler.postDelayed(sendRunnable, Utils.secondsToMillis(duration));
				}
			}
			
			private void intervalReset() {
				Log.d(TAG, "INTERVAL RESET");
				// Calculate activity and reset
				intervalCount++;
				if (varianceSum >= 10.0f) {
					extremeIntervalCount++;
				}
				intervalStartTime += 1000; // Ensure 1 second intervals
				varianceSum = avg = sum = count = 0;
			}
			
			private void update(float x, float y, float z) {
				Log.d(TAG, "UPDATE");
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
			}
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle data = intent.getExtras();

				long timestamp = intent.getLongExtra("TIMESTAMP", 0);
				Log.d(TAG, "RECEIVED:" + timestamp);
				if (sendRunnable == null
						|| timestamp > startTime + duration * 1000
						|| timestamp >= intervalStartTime + 2 * INTERVAL) {
					reset(timestamp);
				} else if (timestamp >= intervalStartTime + INTERVAL) {
					intervalReset();
				}
				
				long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
				//int[] accuracy = data.getIntArray("ACCURACY");
				float[] x = data.getFloatArray("X");
				float[] y = data.getFloatArray("Y");
				float[] z = data.getFloatArray("Z");
				for (int i=0; i<eventTimestamp.length; i++) {
					update(x[i], y[i], z[i]);
				}
			}
		};
		registerReceiver(accelerometerProbeListener, accelerometerProbeBroadcastFilter);
	}


	@Override
	protected void onDisable() {
		unregisterReceiver(accelerometerProbeListener);
		ProbeCommunicator probe = new ProbeCommunicator(this, AccelerometerSensorProbe.class);
		probe.unregisterDataRequest(getClass().getName());
	}
	
	@Override
	public void onRun(Bundle params) {
		long newDuration = Utils.getLong(params, SystemParameter.DURATION.name, DEFAULT_DURATION);
		duration = Math.max(newDuration, duration);
		ProbeCommunicator probe = new ProbeCommunicator(this, AccelerometerSensorProbe.class);
		List<Bundle> rawParams = new ArrayList<Bundle>(getAllRequests().getAll());
		Bundle[] completeParams = new Bundle[rawParams.size()];
		for (int i=0; i<rawParams.size(); i++) {
			completeParams[i] = getCompleteParams(rawParams.get(i));
		}
		probe.registerDataRequest(getClass().getName(), completeParams);
		// TODO: temporary solution to fix 0 PERIOD one shot requests from getting removed before data is sent
		if (Utils.getLong(params, SystemParameter.PERIOD.name, DEFAULT_PERIOD) != 0L) {
			stop();
		}
	}

	@Override
	public void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putInt(TOTAL_INTERVALS, intervalCount);
		data.putInt(ACTIVE_INTERVALS, extremeIntervalCount);
		sendProbeData(Utils.millisToSeconds(startTime), new Bundle(), data); // starTime converted last minute for precision
	}


}
