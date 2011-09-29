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
package edu.mit.media.funf.probe;

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
import edu.mit.media.funf.Utils;


public abstract class SensorProbe extends Probe {
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	private BlockingQueue<SensorEvent> recentEvents;
	private Timer senderTimer;


	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}
	

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(SystemParameter.DURATION, 60L),
				new Parameter(SystemParameter.PERIOD, 3600L),
				new Parameter(SystemParameter.START, 0L),
				new Parameter(SystemParameter.END, 0L)
		};
	}
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}

	@Override
	protected void onEnable() {
		sensor = getSensorManager().getDefaultSensor(getSensorType());
		if (sensor == null) {
			disable();
			return;
		}
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
	public void onRun(Bundle params) {
		if (sensor == null) {
			disable();
			return;
		}
		Log.i(TAG, "SensorKeys listener:" + sensorListener + " SensorKeys:" + sensor + " SensorManager:" + getSensorManager());
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(params));
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
		if (sensor == null) {
			return;
		}
		getSensorManager().unregisterListener(sensorListener);
		senderTimer.cancel();
		if (!recentEvents.isEmpty()) {
			sendProbeData();
		}
	}
	

	@Override
	public void sendProbeData() {
		Log.i(TAG, "RecentEvents before send:" + recentEvents.size());
		if (!recentEvents.isEmpty()) {
			Bundle data = new Bundle();
			List<SensorEvent> events = new ArrayList<SensorEvent>();
			recentEvents.drainTo(events);
			
			if (!events.isEmpty()) {
				Sensor sensor  = events.get(0).sensor;
				Bundle sensorBundle = new Bundle();
				sensorBundle.putFloat("MAXIMUM_RANGE", sensor.getMaximumRange());
				sensorBundle.putString("NAME", sensor.getName());
				sensorBundle.putFloat("POWER", sensor.getPower());
				sensorBundle.putFloat("RESOLUTION", sensor.getResolution());
				sensorBundle.putInt("TYPE", sensor.getType());
				sensorBundle.putString("VENDOR", sensor.getVendor());
				sensorBundle.putInt("VERSION", sensor.getVersion());
				
				String[] valueNames = getValueNames();
				long[] timestamp = new long[events.size()];
				int[] accuracy = new int[events.size()];
				int valuesLength = Math.min(valueNames.length, events.get(0).values.length); // Accounts for optional values
				float[][] values = new float[valuesLength][events.size()];
				long previousTimestamp = 0L;
				for (int i=0; i<events.size(); i++) {
					SensorEvent event = events.get(i);
					// We see repeating events from 
					if (event.timestamp == previousTimestamp) {
						continue;
					}
					previousTimestamp = event.timestamp;
					timestamp[i] = event.timestamp;
					accuracy[i] = event.accuracy;
					for (int valueIndex=0; valueIndex<valuesLength; valueIndex++) {
						values[valueIndex][i] = event.values[valueIndex];
					}
				}
	
				data.putBundle("SENSOR", sensorBundle);
				data.putLongArray("EVENT_TIMESTAMP", timestamp);
				data.putIntArray("ACCURACY", accuracy);
				for (int valueIndex=0; valueIndex<valuesLength; valueIndex++) {
					data.putFloatArray(valueNames[valueIndex], values[valueIndex]);
				}
				
				sendProbeData(Utils.getTimestamp(), new Bundle(), data);
			} else {
				Log.i(TAG, "Recent events is empty.");
			}
		} else {
			Log.i(TAG, "Recent events is empty.");
		}
	}
	

	public abstract int getSensorType();
	
	public int getSensorDelay(Bundle params) {
		return SensorManager.SENSOR_DELAY_GAME;
	}
	
	public abstract String[] getValueNames();
	
}
