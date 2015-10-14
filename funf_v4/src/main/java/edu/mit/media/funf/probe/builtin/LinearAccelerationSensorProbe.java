/**
 * 
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
 * 
 */
package edu.mit.media.funf.probe.builtin;

import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LinearAccelerationSensorKeys;

/**
 * Records a three dimensional vector indicating acceleration along each device axis, not including gravity. All values have units of m/s^2. The coordinate system is the same as is used by the acceleration sensor.  
 * The output of the accelerometer, gravity and linear-acceleration sensors obey the following relation:
 * acceleration = gravity + linear-acceleration
 * 
 * Android Reference http://developer.android.com/reference/android/hardware/SensorEvent.html
 *
 */
@Description("Records a three dimensional vector indicating acceleration along each device axis, not including gravity.")
@RequiredFeatures({"android.hardware.sensor.accelerometer","android.hardware.sensor.gyroscope"})
public class LinearAccelerationSensorProbe extends SensorProbe implements LinearAccelerationSensorKeys {

	public int getSensorType() {
		return 10;  //SensorKeys.TYPE_LINEAR_ACCELERATION; // API Level 9
	}
	
	public String[] getValueNames() {
		return new String[] {
			X, Y, Z
		};
	}
}
