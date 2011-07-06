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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.probe.Probe;

public class AccelerometerProbe extends Probe {
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	private BlockingQueue<SensorEvent> recentEvents;
	private Timer senderTimer;


	@Override
	protected void onEnable() {
		sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		recentEvents = new LinkedBlockingQueue<SensorEvent>();
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				recentEvents.offer(event);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	

	@Override
	protected void onDisable() {
		// Nothing to do
	}

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(SystemParameter.DURATION, 5L),
				new Parameter(SystemParameter.PERIOD, 60L)
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}
	
	public String[] getRequiredFeatures() {
		return new String[]{
			"android.hardware.sensor.accelerometer"
		};
	}
	
	@Override
	public void sendProbeData() {
		Log.i(TAG, "RecentEvents before send:" + recentEvents.size());
		if (!recentEvents.isEmpty()) {
			Bundle data = new Bundle();
			List<SensorEvent> events = new ArrayList<SensorEvent>();
			recentEvents.drainTo(events);
			long[] timestamp = new long[events.size()];
			int[] accuracy = new int[events.size()];
			float[] x = new float[events.size()];
			float[] y = new float[events.size()];
			float[] z = new float[events.size()];
			for (int i=0; i<events.size(); i++) {
				SensorEvent event = events.get(i);
				timestamp[i] = event.timestamp;
				accuracy[i] = event.accuracy;
				x[i] = event.values[0];
				y[i] = event.values[1];
				z[i] = event.values[2];
			}
 			data.putLongArray("EVENT_TIMESTAMP", timestamp);
			data.putIntArray("ACCURACY", accuracy);
			data.putFloatArray("X", x);
			data.putFloatArray("Y", y);
			data.putFloatArray("Z", z);
			sendProbeData(System.currentTimeMillis(), new Bundle(), data);
		} else {
			Log.i(TAG, "Recent events is empty.");
		}
	}

	@Override
	public void onRun(Bundle params) {
		Log.i(TAG, "Sensor listener:" + sensorListener + " Sensor:" + sensor + " SensorManager:" + sensorManager);
		sensorManager.registerListener(sensorListener,sensor, SensorManager.SENSOR_DELAY_NORMAL);
		Log.i(TAG, "RecentEvents before clear:" + recentEvents.size());
		recentEvents.clear();
		Log.i(TAG, "Creating thread");
		senderTimer = new Timer();
		senderTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendProbeData();
			}
		}, 1000L, 1000L);
	}

	@Override
	public void onStop() {
		sensorManager.unregisterListener(sensorListener);
		senderTimer.cancel();
		if (!recentEvents.isEmpty()) {
			sendProbeData();
		}
	}

}
