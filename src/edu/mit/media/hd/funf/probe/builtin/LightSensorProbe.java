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

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.SensorProbe;

public class LightSensorProbe extends SensorProbe {

	public int getSensorType() {
		return Sensor.TYPE_LIGHT;
	}

	public String[] getRequiredFeatures() {
		return new String[]{
			"android.hardware.sensor.light"
		};
	}
	
	public int getSensorDelay(Bundle params) {
		return SensorManager.SENSOR_DELAY_NORMAL;
	}
	
	public String[] getValueNames() {
		return new String[] {
			"LUX"	
		};
	}


}
