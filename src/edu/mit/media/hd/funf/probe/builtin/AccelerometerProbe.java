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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;

public class AccelerometerProbe extends Probe {
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	private SensorEvent mostRecentEvent;


	@Override
	protected void onEnable() {
		sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				mostRecentEvent = event;
				sendProbeData();
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	

	@Override
	protected void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(SystemParameter.DURATION, 5L)
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
		Bundle data = new Bundle();
		data.putLong("ACCURACY", mostRecentEvent.accuracy);
		data.putFloat("X", mostRecentEvent.values[0]);
		data.putFloat("Y", mostRecentEvent.values[1]);
		data.putFloat("Z", mostRecentEvent.values[2]);
		sendProbeData(mostRecentEvent.timestamp/1000000, new Bundle(), data); // Convert from nano to milli seconds
	}

	@Override
	public void onRun(Bundle params) {
		sensorManager.registerListener(sensorListener,sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onStop() {
		sensorManager.unregisterListener(sensorListener);
		mostRecentEvent = null;
	}

}
