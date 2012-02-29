package edu.mit.media.funf.probe.builtin;

import android.hardware.Sensor;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccelerometerSensorKeys;

@RequiredFeatures("android.hardware.sensor.accelerometer")
public class AccelerometerSensorProbe extends SensorProbe implements AccelerometerSensorKeys {

	@Override
	public int getSensorType() {
		return Sensor.TYPE_ACCELEROMETER;
	}

	@Override
	public String[] getValueNames() {
		return new String[] {
				X, Y, Z
		};
	}

}
