package edu.mit.media.funf.probe.builtin;

import static edu.mit.media.funf.Utils.TAG;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@DisplayName("Running Applications")
@Description("Emits the applications the user is running using a polling method.")
@RequiredPermissions(android.Manifest.permission.GET_TASKS)
@DefaultSchedule(period=0, duration=Double.MAX_VALUE, opportunistic=true)
public class RunningApplicationsProbe extends SimpleProbe<RunningTaskInfo> implements ContinuousProbe, PassiveProbe {

	@Configurable
	private double pollInterval = 1.0;
	
	@Configurable
	private int maxTasksToCompare = 1000;
	
	private class RunningAppsPoller implements Runnable {
		
		private List<RunningTaskInfo> lastSentRunningTasks = null;
		
		@Override
		public void run() {
			if (am != null) {
				Log.d(TAG, "Last sent running tasks " + lastSentRunningTasks);
				if (lastSentRunningTasks == null) {
					Log.d(TAG, "Initial send");
					lastSentRunningTasks =  am.getRunningTasks(maxTasksToCompare);
					sendData(lastSentRunningTasks.get(0));
				} else {
					Log.d(TAG, "Polling running apps.");
					List<RunningTaskInfo> currentTasks = am.getRunningTasks(maxTasksToCompare);
					List<RunningTaskInfo> sentTasks = new ArrayList<ActivityManager.RunningTaskInfo>(); 
					List<RunningTaskInfo> unchangedTasks = new ArrayList<ActivityManager.RunningTaskInfo>();
					// Diff the two lists, looking for order changes as well
					for (RunningTaskInfo taskInfo : currentTasks) {
						Log.d(TAG, "Testing task: " + taskInfo.baseActivity.getPackageName());
						if (isChanged(taskInfo, unchangedTasks.size())) {
							Log.d(TAG, "Changed task: " + taskInfo.baseActivity.getPackageName());
							// Send all ones though unchanged until this point, as their order has changed
							for (RunningTaskInfo incorrectlyThoughtUnchangedTaskInfo : unchangedTasks) {
								sentTasks.add(incorrectlyThoughtUnchangedTaskInfo);
								sendData(incorrectlyThoughtUnchangedTaskInfo);
							}
							unchangedTasks.clear();
							sentTasks.add(taskInfo);
							sendData(taskInfo);
						} else {
							Log.d(TAG, "Unchanged task: " + taskInfo.baseActivity.getPackageName());
							// Keep track of unchanged tasks in case their order has changed
							unchangedTasks.add(taskInfo);
						}
					}
					lastSentRunningTasks = sentTasks;
				}
				getHandler().postDelayed(this, Utils.secondsToMillis(pollInterval));
			}
		}
		
		public void reset() {
			lastSentRunningTasks = null;
		}
		
		private boolean isChanged(RunningTaskInfo taskInfo, int existingTaskIndex) {
			if (lastSentRunningTasks == null) {
				Log.d(TAG, "Last running tasks are null");
				return true;
			}
			if (lastSentRunningTasks.size() <= existingTaskIndex) {
				Log.d(TAG, "Last running tasks size " + lastSentRunningTasks.size() + " is lte index " + existingTaskIndex);
				return false; // If we go past the end of our list, assume unchanged.  Shouldn't happen in practice.
			}
			RunningTaskInfo sentTask = lastSentRunningTasks.get(existingTaskIndex);
			if (sentTask.id != taskInfo.id) {
				Log.d(TAG, "Task ids are different");
			}
			if (sentTask.numActivities != taskInfo.numActivities) {
				Log.d(TAG, "Num activities are different");
			}
			if (!sentTask.topActivity.equals(taskInfo.topActivity)) {
				Log.d(TAG, "Top activity is different");
			}
			return sentTask.id != taskInfo.id
					|| sentTask.numActivities != taskInfo.numActivities
					|| !sentTask.topActivity.equals(taskInfo.topActivity);
		}
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
					start();
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
		Log.d(TAG, "Running applications: Enable passive.");
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
		am = null;
		getHandler().removeCallbacks(runningAppsPoller);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		runningAppsPoller.reset();
		getProbeFactory().getProbe(ScreenProbe.class, null).unregisterListener(screenListener);
	}
	
	
	
}
