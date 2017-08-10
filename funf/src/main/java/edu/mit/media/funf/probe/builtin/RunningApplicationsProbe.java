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



import java.math.BigDecimal;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.RunningApplicationsKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

@DisplayName("Running Applications")
@Description("Emits the applications the user is running using a polling method.")
@RequiredPermissions(android.Manifest.permission.GET_TASKS)
@Schedule.DefaultSchedule(interval=0, duration=0.0, opportunistic=true)
public class RunningApplicationsProbe extends Base implements ContinuousProbe, PassiveProbe, RunningApplicationsKeys {

	@Configurable
	private double pollInterval = 1.0;
	
	// Used as the flag for polling vs paused
	private PowerManager pm;
	
	private class RunningAppsPoller implements Runnable {
		
		private RecentTaskInfo currentRunningTask = null;
		private BigDecimal currentRunningTaskStartTime = null;
		
		@Override
		public void run() {
			if (am != null) {
				List<RecentTaskInfo> currentTasks = am.getRecentTasks(1, ActivityManager.RECENT_WITH_EXCLUDED);
				if (!currentTasks.isEmpty()) {
					RecentTaskInfo updatedTask = currentTasks.get(0);
					if (currentRunningTask == null || !currentRunningTask.baseIntent.filterEquals(updatedTask.baseIntent)) {
						endCurrentTask();
						currentRunningTask = updatedTask;
						currentRunningTaskStartTime = TimeUtil.getTimestamp();
					} 
				}
				getHandler().postDelayed(this, TimeUtil.secondsToMillis(pollInterval));
			}
		}
		
		public void endCurrentTask() {
			if (currentRunningTask != null && currentRunningTaskStartTime != null) {
				BigDecimal duration = TimeUtil.getTimestamp().subtract(currentRunningTaskStartTime);
				sendData(currentRunningTask, currentRunningTaskStartTime, duration);
				reset();
			}
		}
		
		public void reset() {
			currentRunningTask = null;
			currentRunningTaskStartTime = null;
		}
	}
	
	private void sendData(RecentTaskInfo taskInfo, BigDecimal timestamp, BigDecimal duration) {
		Gson gson = getGson();
		JsonObject data = new JsonObject();
		data.add(TIMESTAMP, gson.toJsonTree(timestamp));
		data.add(DURATION, gson.toJsonTree(duration));
		data.add(TASK_INFO, gson.toJsonTree(taskInfo));
		sendData(data);
	}
	
	private ActivityManager am;
	private RunningAppsPoller runningAppsPoller = new RunningAppsPoller();
	private DataListener screenListener = new DataListener() {
		
		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
			Log.d(LogUtil.TAG, "RunningApplications: " + data);
			String type = completeProbeUri.get(RuntimeTypeAdapterFactory.TYPE).getAsString();
			if (ScreenProbe.class.getName().equals(type)) {
				boolean screenOn = data.get(ScreenProbe.SCREEN_ON).getAsBoolean();
				if (screenOn) {
					onContinue();
				} else {
					onPause();
				}
			}
		}
		
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
			// Shuts this down if something unregisters the listener
			disable();
		}
	};
	
	@Override
	protected synchronized void onEnable() {
		super.onEnable();
		Log.d(LogUtil.TAG, "RunningApplicationsProbe: onEnable");
		pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
	}

	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LogUtil.TAG, "RunningApplicationsProbe: onStart");
		getGson().fromJson(DEFAULT_CONFIG, ScreenProbe.class).registerListener(screenListener);
		if (pm.isScreenOn()) {
			onContinue();
		}
	}

	protected void onContinue() { 
		if (State.RUNNING.equals(getState()) && am == null) {
			Log.d(LogUtil.TAG, "RunningApplicationsProbe: onContinue");
			am = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
			getHandler().post(runningAppsPoller);
		}
	}
	
	protected void onPause() { 
		if (am != null) {
			Log.d(LogUtil.TAG, "RunningApplicationsProbe: onPause");
			am = null;
			getHandler().removeCallbacks(runningAppsPoller);
			runningAppsPoller.endCurrentTask();
		}
	}
	

	@Override
	protected void onStop() {
		super.onStop();
		onPause();
		getGson().fromJson(DEFAULT_CONFIG, ScreenProbe.class).unregisterListener(screenListener);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		Log.d(LogUtil.TAG, "RunningApplicationsProbe: onDisable");
		runningAppsPoller.reset();
	}
	
	protected boolean isWakeLockedWhileRunning() {
		return false;
	}
	
}
