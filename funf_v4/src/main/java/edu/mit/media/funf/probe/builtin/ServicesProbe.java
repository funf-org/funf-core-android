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

import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

import com.google.gson.Gson;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ServicesKeys;

public class ServicesProbe extends Base implements ServicesKeys {

	/**
	 * The array of packages from which service info will be emitted. 
	 * If this parameter is null, will return every service.
	 */
	@Configurable
	private String[] packages = null;  
	
	@Override
	protected void onStart() {
		super.onStart();
		Gson gson = getGson();
		ActivityManager am = (ActivityManager)getContext().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<String> packageList = packages == null ? null : Arrays.asList(packages);
		for (RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
			String packageName = info.service.getPackageName();
			if (packageList == null || packageList.contains(packageName)) {
				sendData(gson.toJsonTree(info).getAsJsonObject());
			}
		}
		stop();
	}
	
}
