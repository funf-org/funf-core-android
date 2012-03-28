package edu.mit.media.funf.probe.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.StateListener;
import edu.mit.media.funf.probe.ProbeFactory;


public class TestAllBuiltinProbes extends AndroidTestCase {

	public static final String TAG = "FunfTest";
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataReceived(Uri completeProbeUri, JsonObject data) {
			Log.i(TAG, completeProbeUri.toString() + " " + data.toString());
		}

		@Override
		public void onDataCompleted(Uri completeProbeUri) {
			Log.i(TAG, completeProbeUri.toString() + " Data complete!");
		}
	};
	
	private StateListener stateListener = new StateListener() {

		@Override
		public void onStateChanged(Probe probe) {
			Log.i(TAG, probe.getClass().getName() + ": " + probe.getState());
			Log.i(TAG, Probe.Identifier.getCompleteProbeUri(probe).toString());
		}
		
	};
	
	@SuppressWarnings("rawtypes")
	public static final Class[] ALL_PROBES = {
		AccelerometerFeaturesProbe.class,
		AccelerometerSensorProbe.class,
		ApplicationsProbe.class,
		AudioFeaturesProbe.class,
		AudioMediaProbe.class,
		GravitySensorProbe.class,
		GyroscopeSensorProbe.class,
		LightSensorProbe.class,
		LinearAccelerationSensorProbe.class,
		MagneticFieldSensorProbe.class,
		OrientationSensorProbe.class,
		PressureSensorProbe.class,
		ProximitySensorProbe.class,
		RotationVectorSensorProbe.class,
		RunningApplicationsProbe.class,
		ScreenProbe.class,
		TemperatureSensorProbe.class
	};
	
	
	@SuppressWarnings("unchecked")
	public void testAll() throws ClassNotFoundException, IOException, InterruptedException {
		Log.i(TAG,"Running");
		List<Class<? extends Probe>> allProbeClasses = Arrays.asList((Class<? extends Probe>[])ALL_PROBES);
		
		// Run one at a time
		ProbeFactory factory = ProbeFactory.BasicProbeFactory.getInstance(getContext());
		for (Class<? extends Probe> probeClass : allProbeClasses) {
			JsonObject config = new JsonObject();
			config.addProperty("sensorDelay", SensorProbe.SENSOR_DELAY_NORMAL);
			config.addProperty("asdf", 1);
			config.addProperty("zzzz", "__");
			Probe probe = factory.getProbe(probeClass, config);
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
			probes.add(factory.getProbe(probeClass, null));
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
	}
}
