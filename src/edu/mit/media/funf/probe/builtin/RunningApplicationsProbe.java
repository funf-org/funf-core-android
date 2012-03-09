package edu.mit.media.funf.probe.builtin;

import static edu.mit.media.funf.Utils.TAG;

import java.math.BigDecimal;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.RunningApplicationsKeys;

@DisplayName("Running Applications")
@Description("Emits the applications the user is running using a polling method.")
@RequiredPermissions(android.Manifest.permission.GET_TASKS)
@DefaultSchedule(period=0, duration=Double.MAX_VALUE, opportunistic=true)
public class RunningApplicationsProbe extends Base implements ContinuousProbe, PassiveProbe, RunningApplicationsKeys {

	@Configurable
	private double pollInterval = 1.0;
	
	
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
						currentRunningTaskStartTime = Utils.getTimestamp();
					} 
				}
				getHandler().postDelayed(this, Utils.secondsToMillis(pollInterval));
			}
		}
		
		public void endCurrentTask() {
			if (currentRunningTask != null && currentRunningTaskStartTime != null) {
				BigDecimal duration = Utils.getTimestamp().subtract(currentRunningTaskStartTime);
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
		public void onDataReceived(Uri completeProbeUri, JsonObject data) {
			Log.d(TAG, "RunningApplications: " + data);
			if (ScreenProbe.class.getName().equals(Probe.Identifier.getProbeName(completeProbeUri))) {
				boolean screenOn = data.get(ScreenProbe.SCREEN_ON).getAsBoolean();
				if (screenOn) {
					if (!getDataListeners().isEmpty()) { // Start only if we have listeners
						start();
					}
				} else {
					stop();
				}
			}
		}
		
		@Override
		public void onDataCompleted(Uri completeProbeUri) {
			// Unused
		}
	};
	
	@Override
	protected synchronized void onEnable() {
		super.onEnable();
		Log.d(TAG, "RunningApplicationsProbe: onEnable");
		getProbeFactory().getProbe(ScreenProbe.class, null).registerListener(screenListener);

		// Set for current state
		PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn()) {
			start();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "RunningApplicationsProbe: onStart");
		PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn()) {
			am = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
			getHandler().post(runningAppsPoller);
		} else {
			stop();
		}
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "RunningApplicationsProbe: onStop");
		am = null;
		getHandler().removeCallbacks(runningAppsPoller);
		runningAppsPoller.endCurrentTask();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		Log.d(TAG, "RunningApplicationsProbe: onDisable");
		runningAppsPoller.reset();
		getProbeFactory().getProbe(ScreenProbe.class, null).unregisterListener(screenListener);
	}
	
	
	
}
