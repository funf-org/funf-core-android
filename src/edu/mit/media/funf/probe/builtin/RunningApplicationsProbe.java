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
