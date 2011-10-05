/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe.builtin;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import edu.mit.media.funf.probe.SensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccelerometerSensorKeys;

/**
 * Measures the acceleration applied to the device.  All values are in SI units (m/s^2).
 * 
 * More information in the Android Reference http://developer.android.com/reference/android/hardware/SensorEvent.html
 *
 */
public class AccelerometerSensorProbe extends SensorProbe implements AccelerometerSensorKeys {

	public int getSensorType() {
		return Sensor.TYPE_ACCELEROMETER;
	}

	public String[] getRequiredFeatures() {
		return new String[]{
			"android.hardware.sensor.accelerometer"
		};
	}
	
	
	public String[] getValueNames() {
		return new String[] {
				X, Y, Z
		};
	}

}
