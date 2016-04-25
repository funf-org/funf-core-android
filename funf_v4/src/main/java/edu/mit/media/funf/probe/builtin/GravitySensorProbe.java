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

import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.builtin.ProbeKeys.GravitySensorKeys;

/**
 * A three dimensional vector indicating the direction and magnitude of gravity. Units are m/s^2. The coordinate system is the same as is used by the acceleration sensor.
 * Note: When the device is at rest, the output of the gravity sensor should be identical to that of the accelerometer.
 * 
 * Android Reference http://developer.android.com/reference/android/hardware/SensorEvent.html
 *
 */
@Description("A three dimensional vector indicating the direction and magnitude of gravity. Units are m/s^2. The coordinate system is the same as is used by the acceleration sensor.")
@RequiredFeatures({"android.hardware.sensor.accelerometer", "android.hardware.sensor.gyroscope"})
public class GravitySensorProbe extends SensorProbe implements GravitySensorKeys {

	public int getSensorType() {
		return 9;  //SensorKeys.TYPE_GRAVITY; // API Level 9
	}
	
	public String[] getValueNames() {
		return new String[] {
			X, Y, Z
		};
	}
}
