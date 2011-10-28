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
import android.os.Handler;
import android.util.Log;
import edu.mit.media.funf.Utils;


public abstract class SensorProbe extends Probe {
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	private BlockingQueue<SensorEventCopy> recentEvents;
	private Timer senderTimer;

	private Handler handler;
	private Runnable sendDataRunnable = new Runnable() {
		@Override
		public void run() {
			sendProbeData();
			if (handler != null) {
				handler.postDelayed(this, 1000L);
			}
		}
	};

	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}
	

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.DURATION, getDefaultDuration()),
				new Parameter(Parameter.Builtin.PERIOD, getDefaultPeriod()),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
	}
	
	protected long getDefaultPeriod() {
		return 1800L;
	}
	
	protected long getDefaultDuration() {
		return 60;
	}
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}

	@Override
	public boolean isAvailableOnDevice() {
		return getSensorManager().getDefaultSensor(getSensorType()) != null;
	}


	@Override
	protected void onEnable() {
		handler = new Handler();
		sensor = getSensorManager().getDefaultSensor(getSensorType());
		recentEvents = new LinkedBlockingQueue<SensorEventCopy>();
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO: the same event objects are reused
				recentEvents.offer(new SensorEventCopy(event));
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
		Log.i(TAG, "SensorKeys listener:" + sensorListener + " SensorKeys:" + sensor + " SensorManager:" + getSensorManager());
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(params));
		Log.i(TAG, "RecentEvents before clear:" + recentEvents.size());
		recentEvents.clear();
		Log.i(TAG, "Creating thread");
		
		handler.postDelayed(sendDataRunnable, 1000L);
		//senderTimer = new Timer();
		//senderTimer.schedule(new TimerTask() {
		//	@Override
		//	public void run() {
		//		sendProbeData();
		//	}
		//}, 1000L, 1000L);
	}

	@Override
	public void onStop() {
		if (sensor == null) {
			return;
		}
		getSensorManager().unregisterListener(sensorListener);
		handler.removeCallbacks(sendDataRunnable);
		//senderTimer.cancel();
		if (!recentEvents.isEmpty()) {
			sendProbeData();
		}
	}
	

	@Override
	public void sendProbeData() {
		Log.i(TAG, "RecentEvents before send:" + recentEvents.size());
		if (!recentEvents.isEmpty()) {
			Bundle data = new Bundle();
			List<SensorEventCopy> events = new ArrayList<SensorEventCopy>();
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

				for (int i=0; i<events.size(); i++) {
					SensorEventCopy event = events.get(i);
					
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
				sendProbeData(Utils.getTimestamp(), data);
			} else {
				Log.i(TAG, "Recent events is empty.");
			}
		} else {
			Log.i(TAG, "Recent events is empty.");
		}
	}
	
	/**
	 * Local copy of sensor data to process.
	 * TODO: May want to resuse these objects
	 */
	private class SensorEventCopy {
		
		public final long timestamp;
		public final int accuracy;
		public final float[] values;
		public final Sensor sensor;
		
		public SensorEventCopy(SensorEvent event) {
			this.timestamp = event.timestamp;
			this.accuracy = event.accuracy;
			this.values = new float[event.values.length];
			System.arraycopy(event.values, 0, this.values, 0, event.values.length);
			this.sensor = event.sensor;
		}
		
	}
	

	public abstract int getSensorType();
	
	public int getSensorDelay(Bundle params) {
		return SensorManager.SENSOR_DELAY_GAME;
	}
	
	public abstract String[] getValueNames();

	
}
