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
import android.app.ActivityManager.RunningServiceInfo;
import android.os.Bundle;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ServicesKeys;

/**
 * Returns deatails about the running services on the device.
 * 
 * @author alangardner
 *
 */
public class ServicesProbe extends SynchronousProbe implements ServicesKeys {

	private static final int MAX_NUM_SERVICES = Integer.MAX_VALUE;
	
	@Override
	protected Bundle getData() {
		// TODO: make work for only this package
		// TODO: allow parameter to customize package scanned
		ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> serviceInfos = new ArrayList<RunningServiceInfo>(am.getRunningServices(MAX_NUM_SERVICES));
		Bundle data = new Bundle();
		data.putParcelableArrayList(RUNNING_SERVICES, serviceInfos);
		return data;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

}
