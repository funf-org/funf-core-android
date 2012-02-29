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

import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.os.Bundle;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.RunningApplicationsKeys;

public class RunningApplicationsProbe extends SynchronousProbe implements RunningApplicationsKeys {
	
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
		data.putParcelableArrayList(RUNNING_TASKS, mostRecentRunningTaks);
		return data;
	}

	@Override
	protected long getDefaultPeriod() {
		return 30L;
	}
}
