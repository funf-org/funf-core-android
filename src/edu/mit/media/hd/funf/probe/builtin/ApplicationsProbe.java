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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.SynchronousProbe;
import edu.mit.media.hd.funf.probe.builtin.ProbeKeys.ApplicationsKeys;

public class ApplicationsProbe extends SynchronousProbe implements ApplicationsKeys {
	
	@Override
	public String[] getRequiredPermissions() {
		return null;
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

}
