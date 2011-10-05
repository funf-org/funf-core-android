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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ApplicationsKeys;

public class ApplicationsProbe extends SynchronousProbe implements ApplicationsKeys {
	
	@Override
	public String[] getRequiredPermissions() {
		return null;
	}
	
	@Override
	protected String getDisplayName() {
		return "Installed Applications Probe";
	}
	
	private static Set<String> getInstalledAppPackageNames(List<ApplicationInfo> installedApps) {
		HashSet<String> installedAppPackageNames = new HashSet<String>();
		for (ApplicationInfo info : installedApps) {
			installedAppPackageNames.add(info.packageName);
		}
		return installedAppPackageNames;
	}
	
	private static ArrayList<ApplicationInfo> getUninstalledApps(List<ApplicationInfo> allApplications, List<ApplicationInfo> installedApps) {
		Set<String> installedAppPackageNames = getInstalledAppPackageNames(installedApps);
		ArrayList<ApplicationInfo> uninstalledApps = new ArrayList<ApplicationInfo>();
		for (ApplicationInfo info : allApplications) {
			if (!installedAppPackageNames.contains(info.packageName)) {
				uninstalledApps.add(info);
			}
		}
		return uninstalledApps;
	}

	@Override
	protected Bundle getData() {
		Bundle data = new Bundle();
		PackageManager pm = this.getApplicationContext().getPackageManager();
		List<ApplicationInfo> allApplications = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		ArrayList<ApplicationInfo> installedApplications = new ArrayList<ApplicationInfo>(pm.getInstalledApplications(0));
		data.putParcelableArrayList(INSTALLED_APPLICATIONS, installedApplications);
		data.putParcelableArrayList(UNINSTALLED_APPLICATIONS, getUninstalledApps(allApplications, installedApplications));
		return data;
	}
	
	@Override
	protected long getDefaultPeriod() {
		return 36000L;
	}

}
