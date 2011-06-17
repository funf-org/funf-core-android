/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;

public class RunningApplicationsProbe extends Probe {

	private ArrayList<RunningTaskInfo> mostRecentRunningTaks;
	private long mostRecentScanTime;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 0L),	
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.GET_TASKS	
		};
	}

	@Override
	protected void onEnable() {
		// Nothing, only active
	}

	@Override
	protected void onDisable() {
		// Nothing, only active
	}

	@Override
	protected void onRun(Bundle params) {
		ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
		mostRecentRunningTaks = new ArrayList<RunningTaskInfo>(am.getRunningTasks(100000000));
		mostRecentScanTime = System.currentTimeMillis();
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		stopSelf(); // No need of service after stop
	}

	@Override
	public void sendProbeData() {
		if (mostRecentRunningTaks != null) {
			Bundle data = new Bundle();
			data.putParcelableArrayList("RUNNING_TASKS", mostRecentRunningTaks);
			sendProbeData(mostRecentScanTime, new Bundle(), data);
		}
	}

}
