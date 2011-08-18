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
import edu.mit.media.hd.funf.probe.builtin.ProbeKeys.RotationVectorSensorKeys;

public class RotationVectorSensorProbe extends SensorProbe implements RotationVectorSensorKeys {

	public int getSensorType() {
		return 11;  //SensorKeys.TYPE_ROTATION_VECTOR; // API Level 9
	}

	public String[] getRequiredFeatures() {
		return new String[]{
			"android.hardware.sensor.gyroscope"
		};
	}
	
	public int getSensorDelay(Bundle params) {
		return SensorManager.SENSOR_DELAY_NORMAL;
	}
	
	public String[] getValueNames() {
		return new String[] {
			X_SIN_THETA_OVER_2, Y_SIN_THETA_OVER_2, Z_SIN_THETA_OVER_2, COS_THETA_OVER_2
		};
	}


}
