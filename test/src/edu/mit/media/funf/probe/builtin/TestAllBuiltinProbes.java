package edu.mit.media.funf.probe.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Debug;
import android.test.AndroidTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.State;
import edu.mit.media.funf.probe.Probe.StateListener;


/**
 * This class turns on and off all of the builtin probes.  
 * While it doesn't test any of the output, it does ensure that basic use of the probes does not crash the process.
 * @author alangardner
 *
 */
public class TestAllBuiltinProbes extends AndroidTestCase {

	public static final String TAG = "FunfTest";
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
			Log.i(TAG, "DATA: " + completeProbeUri.toString() + " " + data.toString());
		}

		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
			Log.i(TAG, "COMPLETE: " + completeProbeUri.toString());
		}
	};
	
	
	
	private StateListener stateListener = new StateListener() {

		@Override
		public void onStateChanged(Probe probe, State previousState) {
			Log.i(TAG, probe.getClass().getName() + ": " + probe.getState());
			Log.i(TAG, getGson().toJson(probe));
		}
		
	};
	

	private Gson gson;
	public Gson getGson() {
		if (gson == null) {
			gson = new GsonBuilder().registerTypeAdapterFactory(FunfManager.getProbeFactory(getContext())).create();
		}
		return gson;
	}
	
	@SuppressWarnings("rawtypes")
	public static final Class[] ALL_PROBES = {
		AccelerometerFeaturesProbe.class,
		AccelerometerSensorProbe.class,
		ApplicationsProbe.class,
		AudioFeaturesProbe.class,
		AudioMediaProbe.class,
		BatteryProbe.class,
		BluetoothProbe.class,
		BrowserBookmarksProbe.class,
		BrowserSearchesProbe.class,
		CallLogProbe.class,
		CellTowerProbe.class,
		ContactProbe.class,
		GravitySensorProbe.class,
		GyroscopeSensorProbe.class,
		HardwareInfoProbe.class,
		ImageMediaProbe.class,
		LightSensorProbe.class,
		LinearAccelerationSensorProbe.class,
		LocationProbe.class,
		MagneticFieldSensorProbe.class,
		OrientationSensorProbe.class,
		PressureSensorProbe.class,
		ProcessStatisticsProbe.class,
		ProximitySensorProbe.class,
		RotationVectorSensorProbe.class,
		RunningApplicationsProbe.class,
		ServicesProbe.class,
		SimpleLocationProbe.class,
		ScreenProbe.class,
		SmsProbe.class,
		TelephonyProbe.class,
		TemperatureSensorProbe.class,
		TimeOffsetProbe.class,
		VideoMediaProbe.class,
		WifiProbe.class
	};
	
	
	
	@SuppressWarnings("unchecked")
	public void testAll() throws ClassNotFoundException, IOException, InterruptedException {
		Log.i(TAG,"Running");
		Debug.startMethodTracing("calc");
		List<Class<? extends Probe>> allProbeClasses = Arrays.asList((Class<? extends Probe>[])ALL_PROBES);
		
		// Run one at a time
		Gson gson = getGson();
		for (Class<? extends Probe> probeClass : allProbeClasses) {
			JsonObject config = new JsonObject();
			config.addProperty("sensorDelay", SensorProbe.SENSOR_DELAY_NORMAL);
			config.addProperty("asdf", 1);
			config.addProperty("zzzz", "__");
			Probe probe = gson.fromJson(config, probeClass);
			probe.addStateListener(stateListener);
			probe.registerListener(listener);
			Thread.sleep(100L);
			if (probe instanceof ContinuousProbe) {
				((ContinuousProbe)probe).unregisterListener(listener);
			}
		}
		// Run simultaneously
		List<Probe> probes = new ArrayList<Probe>();
		for (Class<? extends Probe> probeClass : allProbeClasses) {
			probes.add(gson.fromJson(Probe.DEFAULT_CONFIG, probeClass));
		}
		for (Probe probe : probes) {
			probe.addStateListener(stateListener);
			probe.registerListener(listener);
		}
		Thread.sleep(10000L);
		for (Probe probe : probes) {
			if (probe instanceof ContinuousProbe) {
				((ContinuousProbe)probe).unregisterListener(listener);
			}
		}
		
		Thread.sleep(1000L); // Give probes time stop

		Debug.stopMethodTracing();
	}
}
