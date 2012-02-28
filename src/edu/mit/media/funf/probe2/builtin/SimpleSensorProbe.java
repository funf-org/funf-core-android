package edu.mit.media.funf.probe2.builtin;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

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
@DefaultConfig("{\"sensorDelay\": \"FASTEST\", \"sensorInfo\": false}") 
public abstract class SimpleSensorProbe extends Base implements ContinuousProbe, SensorKeys {
	public static final String 
		SENSOR_DELAY = "sensorDelay";
	public static final String 
		SENSOR_DELAY_FASTEST = "FASTEST",
		SENSOR_DELAY_GAME = "GAME",
		SENSOR_DELAY_UI = "UI",
		SENSOR_DELAY_NORMAL = "NORMAL";

	
	private SensorManager sensorManager;
	private Sensor sensor;
	private JsonElement sensorDetails;
	private SensorEventListener sensorListener;
	private Gson gson;
	private long startMillis;
	
	@Override
	protected void onEnable() {
		super.onEnable();
		gson = getGson();
		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(getSensorType());
		sensorDetails = getGson().toJsonTree(sensor);
		final String[] valueNames = getValueNames();
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				JsonObject data = new JsonObject();
				data.addProperty(TIMESTAMP, Utils.uptimeNanosToTimestamp(event.timestamp));
				data.addProperty(ACCURACY, event.accuracy);
				for (int i = 0; i < event.values.length; i++) {
					String valueName = i < valueNames.length ? valueNames[i] : "value" + i;
				}
				sendData(data);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
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
	
	public abstract int getSensorType();
	public abstract String[] getValueNames();
}
