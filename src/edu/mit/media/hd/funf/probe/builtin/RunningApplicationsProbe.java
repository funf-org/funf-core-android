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
import edu.mit.media.hd.funf.probe.SynchronousProbe;

public class RunningApplicationsProbe extends SynchronousProbe {
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.GET_TASKS	
		};
	}

	@Override
	protected Bundle getData() {
		ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
		ArrayList<RunningTaskInfo> mostRecentRunningTaks = new ArrayList<RunningTaskInfo>(am.getRunningTasks(100000000));
		Bundle data = new Bundle();
		data.putParcelableArrayList("RUNNING_TASKS", mostRecentRunningTaks);
		return data;
	}

}
