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


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SensorKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

@Schedule.DefaultSchedule(interval=SensorProbe.DEFAULT_PERIOD, duration=SensorProbe.DEFAULT_DURATION)
public abstract class SensorProbe extends Base implements ContinuousProbe, SensorKeys {
	
	public static final double DEFAULT_PERIOD = 3600;
	public static final double DEFAULT_DURATION = 60;
	
	@Configurable
	private String sensorDelay = SENSOR_DELAY_FASTEST;
	
	public static final String 
		SENSOR_DELAY_FASTEST = "FASTEST",
		SENSOR_DELAY_GAME = "GAME",
		SENSOR_DELAY_UI = "UI",
		SENSOR_DELAY_NORMAL = "NORMAL";
	
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	@Override
	protected void onEnable() {
		super.onEnable();
		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(getSensorType());
		final String[] valueNames = getValueNames();
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				JsonObject data = new JsonObject();
				data.addProperty(TIMESTAMP, TimeUtil.uptimeNanosToTimestamp(event.timestamp));
				data.addProperty(ACCURACY, event.accuracy);
				int valuesLength = Math.min(event.values.length, valueNames.length);
				for (int i = 0; i < valuesLength; i++) {
					String valueName = valueNames[i];
					data.addProperty(valueName, event.values[i]);
				}
				sendData(data);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(sensorDelay));
	}
	
	@Override
	protected void onStop() {
		getSensorManager().unregisterListener(sensorListener);
	}
	
	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}
	
	protected int getSensorDelay(String specifiedSensorDelay) {
		int sensorDelay = -1;
		JsonElement el =  getGson().toJsonTree(specifiedSensorDelay);
		if (!el.isJsonNull()) {
			try { 
				int sensorDelayInt = el.getAsInt();
				if (sensorDelayInt == SensorManager.SENSOR_DELAY_FASTEST 
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_GAME
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_UI
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_NORMAL) {
					sensorDelay = sensorDelayInt;
				}
			} catch (NumberFormatException e) {
			} catch (ClassCastException e) {
			} catch (IllegalStateException e) {
			}
		}
		
		if (sensorDelay < 0) {
			try {
				String sensorDelayString = el.getAsString().toUpperCase().replace("SENSOR_DELAY_", "");
				if (SENSOR_DELAY_FASTEST.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
				} else if (SENSOR_DELAY_GAME.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_GAME;
				} else if (SENSOR_DELAY_UI.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_UI;
				} else if (SENSOR_DELAY_NORMAL.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
				}
			} catch (ClassCastException cce) {
				Log.w(LogUtil.TAG, "Unknown sensor delay value: " + specifiedSensorDelay);
			}
		}
		
		if (sensorDelay < 0) {
			sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
		}
		
		return sensorDelay;
	}
	
	public abstract int getSensorType();
	public abstract String[] getValueNames();
}
