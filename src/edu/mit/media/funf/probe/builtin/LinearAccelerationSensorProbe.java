/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.funf.probe.builtin;

import android.hardware.SensorManager;
import android.os.Bundle;
import edu.mit.media.funf.probe.SensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LinearAccelerationSensorKeys;

public class LinearAccelerationSensorProbe extends SensorProbe implements LinearAccelerationSensorKeys {

	public int getSensorType() {
		return 10;  //SensorKeys.TYPE_LINEAR_ACCELERATION; // API Level 9
	}

	public String[] getRequiredFeatures() {
		return new String[]{
			"android.hardware.sensor.accelerometer",
			"android.hardware.sensor.gyroscope"
		};
	}
	
	public int getSensorDelay(Bundle params) {
		return SensorManager.SENSOR_DELAY_NORMAL;
	}
	
	public String[] getValueNames() {
		return new String[] {
			X, Y, Z
		};
	}


}
