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

import android.hardware.Sensor;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.builtin.ProbeKeys.GyroscopeSensorKeys;

/**
 * Measures angular speed around each axis.
 * 
 * All values are in radians/second and measure the rate of rotation around the X, Y and Z axis. 
 * The coordinate system is the same as is used for the acceleration sensor. 
 * Rotation is positive in the counter-clockwise direction. That is, an observer looking from some positive location on the x, y. or z axis at a device positioned on the origin would report positive rotation if the device appeared to be rotating counter clockwise. 
 * Note that this is the standard mathematical definition of positive rotation and does not agree with the definition of roll given earlier.
 *
 * Android Reference http://developer.android.com/reference/android/hardware/SensorEvent.html
 */
@Description("Measures angular speed around each axis.")
@Schedule.DefaultSchedule(interval=1800, duration = 60)
@RequiredFeatures("android.hardware.sensor.gyroscope")
public class GyroscopeSensorProbe extends SensorProbe implements GyroscopeSensorKeys {

	public int getSensorType() {
		return Sensor.TYPE_GYROSCOPE;
	}
	
	public String[] getValueNames() {
		return new String[] {
			X, Y, Z
		};
	}

}
