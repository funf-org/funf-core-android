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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.client.ProbeCommunicator;
import edu.mit.media.hd.funf.probe.Probe;

public class ActivityProbe extends Probe {

	private static long DEFAULT_DURATION = 5L;
	private static long INTERVAL = 1000L;  // Interval over which we calculate activity
	
	private long duration;
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
			new Parameter(Probe.SystemParameter.PERIOD, 10L, true)	
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
		accelerometerProbeBroadcastFilter = new IntentFilter(OppProbe.getDataAction(AccelerometerProbe.class));
		accelerometerProbeListener = new BroadcastReceiver() {

			private long intervalStartTime;
			private float varianceSum;
			private float avg;
			private float sum;
			private int count;
			
			private Runnable sendRunnable;
			
			private void reset(long timestamp) {
				// If more than an INTERVAL away, start a new scan
				startTime = intervalStartTime = timestamp;
				varianceSum = avg = sum = count = 0;
				intervalCount = 1;
				extremeIntervalCount = 0;
				
				// start timer to send results
				if (sendRunnable == null) {
					sendRunnable = new Runnable() {
						public void run() {
							sendProbeData();
							sendRunnable = null;
						}
					};
					handler.postDelayed(sendRunnable, duration*1000);
				}
			}
			
			private void intervalReset() {
				// Calculate activity and reset
				intervalCount++;
				if (varianceSum >= 10.0f) {
					extremeIntervalCount++;
				}
				intervalStartTime += 1000; // Ensure 1 second intervals
				varianceSum = avg = sum = count = 0;
			}
			
			private void update(float x, float y, float z) {
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
				long timestamp = intent.getLongExtra("TIMESTAMP", 0);
				if (timestamp > startTime + duration * 1000
						|| timestamp >= intervalStartTime + 2 * INTERVAL) {
					reset(timestamp);
				} else if (timestamp >= intervalStartTime + INTERVAL) {
					intervalReset();
				}
				
				float x = intent.getFloatExtra("X", 0.0f);
				float y = intent.getFloatExtra("Y", 0.0f);
				float z = intent.getFloatExtra("Z", 0.0f);
				
				update(x, y, z);
			}
		};
		registerReceiver(accelerometerProbeListener, accelerometerProbeBroadcastFilter);
	}


	@Override
	protected void onDisable() {
		unregisterReceiver(accelerometerProbeListener);
		ProbeCommunicator probe = new ProbeCommunicator(this, AccelerometerProbe.class);
		probe.unregisterDataRequest(getClass().getName());
	}
	
	@Override
	public void onRun(Bundle params) {
		long newDuration = params.getLong(SystemParameter.DURATION.name, DEFAULT_DURATION);
		duration = Math.max(newDuration, duration);
		ProbeCommunicator probe = new ProbeCommunicator(this, AccelerometerProbe.class);
		probe.registerDataRequest(getClass().getName(), params);
		stop();
	}

	@Override
	public void onStop() {
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putInt("TOTAL_INTERVALS", intervalCount);
		data.putInt("ACTIVE_INTERVALS", extremeIntervalCount);
		sendProbeData(startTime, new Bundle(), data);
	}


}
