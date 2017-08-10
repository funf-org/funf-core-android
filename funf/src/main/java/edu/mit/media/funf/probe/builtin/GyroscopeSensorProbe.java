/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
