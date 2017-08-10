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


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ActivityKeys;

@Schedule.DefaultSchedule(interval=120, duration=15)
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
public class ActivityProbe extends Base implements ContinuousProbe, PassiveProbe, ActivityKeys {

	@Configurable
	private double interval = 1.0;
	
	//private long startTime;
	private ActivityCounter activityCounter = new ActivityCounter();
	
	@Override
	protected void onEnable() {
		super.onEnable();
		getAccelerometerProbe().registerPassiveListener(activityCounter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getAccelerometerProbe().registerListener(activityCounter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getAccelerometerProbe().unregisterListener(activityCounter);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		getAccelerometerProbe().unregisterPassiveListener(activityCounter);
	}

	private AccelerometerSensorProbe getAccelerometerProbe() {
		return getGson().fromJson(DEFAULT_CONFIG, AccelerometerSensorProbe.class);
	}

	
	private class ActivityCounter implements DataListener {
		private double intervalStartTime;
		private float varianceSum;
		private float avg;
		private float sum;
		private int count;
		
		private void reset() {
			// If more than an interval away, start a new scan
			varianceSum = avg = sum = count = 0;
		}
		
		private void intervalReset() {
			//Log.d(LogUtil.TAG, "interval RESET");
			// Calculate activity and reset
			JsonObject data = new JsonObject();
			if (varianceSum >= 10.0f) {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_HIGH);
			} else if (varianceSum < 10.0f && varianceSum > 3.0f) {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_LOW);
			} else {
				data.addProperty(ACTIVITY_LEVEL, ACTIVITY_LEVEL_NONE);
			}
			sendData(data);
			varianceSum = avg = sum = count = 0;
		}
		
		private void update(float x, float y, float z) {
			//Log.d(TAG, "UPDATE:(" + x + "," + y + "," + z + ")");
			// Iteratively calculate variance sum
			count++;
			float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
			float newAvg = (count - 1)*avg/count + magnitude/count;
			float deltaAvg = newAvg - avg;
			varianceSum += (magnitude - newAvg) * (magnitude - newAvg)
				- 2*(sum - (count-1)*avg) 
				+ (count - 1) *(deltaAvg * deltaAvg);
			sum += magnitude;
			avg = newAvg;
			//Log.d(TAG, "UPDATED VALUES:(count, varianceSum, sum, avg) " + count + ", " + varianceSum+ ", " + sum+ ", " + avg);
		}
		

		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
			double timestamp = data.get(TIMESTAMP).getAsDouble();
			//Log.d(LogUtil.TAG, "IntervalStartTime: " + intervalStartTime);
			//Log.d(LogUtil.TAG, "RECEIVED:" + timestamp);
			if (intervalStartTime == 0.0 || (timestamp >= intervalStartTime + 2 * interval)) {
				//Log.d(LogUtil.TAG, "RESET:" + timestamp);
				reset();
				intervalStartTime = timestamp;
			} else if (timestamp >= intervalStartTime + interval) {
				//Log.d(LogUtil.TAG, "interval Reset:" + timestamp);
				intervalReset();
				intervalStartTime = timestamp;
			}
			float x = data.get(AccelerometerSensorProbe.X).getAsFloat();
			float y = data.get(AccelerometerSensorProbe.Y).getAsFloat();
			float z = data.get(AccelerometerSensorProbe.Z).getAsFloat();
			update(x, y, z);
		}

		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
			// Do nothing
		}
	}
}
