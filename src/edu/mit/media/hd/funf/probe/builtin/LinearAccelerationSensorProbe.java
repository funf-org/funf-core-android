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

import android.hardware.SensorManager;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.SensorProbe;

public class LinearAccelerationSensorProbe extends SensorProbe {

	public int getSensorType() {
		return 10;  //Sensor.TYPE_LINEAR_ACCELERATION; // API Level 9
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
			"X", "Y", "Z"	
		};
	}


}
