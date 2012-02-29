package edu.mit.media.funf.probe2.builtin;

import static edu.mit.media.funf.Utils.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SensorKeys;
import edu.mit.media.funf.probe2.Probe.Base;
import edu.mit.media.funf.probe2.Probe.ContinuousProbe;
import edu.mit.media.funf.probe2.Probe.DefaultConfig;
import edu.mit.media.funf.probe2.Probe.DefaultSchedule;

@DefaultSchedule("{\"period\": 3600, \"duration\": 60}")
@DefaultConfig("{\"sendInterval\": 1.0, \"sensorDelay\": \"FASTEST\"}") // By default send once a second
public abstract class SensorProbe extends Base implements ContinuousProbe, SensorKeys {
	
	public static final String 
		SEND_INTERVAL = "sendInterval",
		SENSOR_DELAY = "sensorDelay";
	public static final String 
		SENSOR_DELAY_FASTEST = "FASTEST",
		SENSOR_DELAY_GAME = "GAME",
		SENSOR_DELAY_UI = "UI",
		SENSOR_DELAY_NORMAL = "NORMAL";
	
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	private JsonElement sensorDetails;
	
	private BlockingQueue<SensorEventCopy> recentEvents;
	
	private Runnable sendDataRunnable = new Runnable() {
		@Override
		public void run() {
			sendSensorData();
			if (getState() == State.RUNNING) {
				getHandler().postDelayed(this, 1000L);
			}
		}
	};
	
	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}
	
	@Override
	protected void onEnable() {
		super.onEnable();
		sensor = getSensorManager().getDefaultSensor(getSensorType());
		sensorDetails = getGson().toJsonTree(sensor);
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
	protected void onStart() {
		super.onStart();
		recentEvents.clear();
		JsonObject config = getCompleteConfig();
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(config));
		getHandler().postDelayed(sendDataRunnable, getSendIntervalMillis(config));
	}
	
	protected long getSendIntervalMillis(JsonObject config) {
		double sendInterval = 1.0;
		try { 
			sendInterval = config.get(SEND_INTERVAL).getAsDouble(); 
		} catch (ClassCastException e) {} catch (IllegalStateException e) {}
		return Utils.secondsToMillis(sendInterval);
	}
	
	protected int getSensorDelay(JsonObject config) {
		int sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
		try { 
			int sensorDelayInt = config.get(SENSOR_DELAY).getAsInt();
			if (sensorDelayInt == SensorManager.SENSOR_DELAY_FASTEST 
					|| sensorDelayInt == SensorManager.SENSOR_DELAY_GAME
					|| sensorDelayInt == SensorManager.SENSOR_DELAY_UI
					|| sensorDelayInt == SensorManager.SENSOR_DELAY_NORMAL) {
				sensorDelay = sensorDelayInt;
			}
		} catch (ClassCastException e) {
			try {
				String sensorDelayString = config.get(SENSOR_DELAY).getAsString().toUpperCase();
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
			}
		} catch (IllegalStateException e) {
			
		}
		return sensorDelay;
	}

	@Override
	protected void onStop() {
		super.onStop();
		getSensorManager().unregisterListener(sensorListener);
		getHandler().removeCallbacks(sendDataRunnable);
		if (!recentEvents.isEmpty()) {
			sendSensorData();
		}
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		// Nothing to do
	}

	public void sendSensorData() {
		Log.i(TAG, "RecentEvents before send:" + recentEvents.size());
		if (!recentEvents.isEmpty()) {
			JsonObject data = new JsonObject();
			List<SensorEventCopy> events = new ArrayList<SensorEventCopy>();
			recentEvents.drainTo(events);
			
			if (!events.isEmpty()) {
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
	
				data.add(SENSOR, sensorDetails);
				Gson gson = getGson();
				data.add(EVENT_TIMESTAMP, gson.toJsonTree(timestamp));
				data.add(ACCURACY, gson.toJsonTree(accuracy));
				for (int valueIndex=0; valueIndex<valuesLength; valueIndex++) {
					data.add(valueNames[valueIndex], gson.toJsonTree(values[valueIndex]));
				}
				sendData(data);
			} else {
				Log.d(TAG, "Recent events is empty.");
			}
		} else {
			Log.d(TAG, "Recent events is empty.");
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
		
		public SensorEventCopy(SensorEvent event) {
			this.timestamp = event.timestamp;
			this.accuracy = event.accuracy;
			this.values = new float[event.values.length];
			System.arraycopy(event.values, 0, this.values, 0, event.values.length);
		}
		
	}
	
	
	public abstract int getSensorType();
	
	
	public abstract String[] getValueNames();
}
