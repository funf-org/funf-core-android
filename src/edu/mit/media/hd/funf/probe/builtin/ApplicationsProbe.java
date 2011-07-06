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
import edu.mit.media.hd.funf.probe.Probe;

public class ApplicationsProbe extends Probe {

	private long mostRecentScanTimestamp;
	private ArrayList<ApplicationInfo> installedApplications;
	private ArrayList<ApplicationInfo> uninstalledApplications;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 3600L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

	@Override
	protected void onEnable() {
		// Active only, nothing to do 
	}
	
	@Override
	protected void onDisable() {
		// Active only, nothing to do 
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
	protected void onRun(Bundle params) {
		PackageManager pm = this.getApplicationContext().getPackageManager();
		List<ApplicationInfo> allApplications = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		installedApplications = new ArrayList<ApplicationInfo>(pm.getInstalledApplications(0));
		uninstalledApplications = getUninstalledApps(allApplications, installedApplications);
		mostRecentScanTimestamp = System.currentTimeMillis();
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		// Nothing to do
	}

	@Override
	public void sendProbeData() {
		if (installedApplications != null & uninstalledApplications != null) {
			Bundle data = new Bundle();
			data.putParcelableArrayList("INSTALLED_APPLICATIONS", installedApplications);
			data.putParcelableArrayList("UNINSTALLED_APPLICATIONS", uninstalledApplications);
			sendProbeData(mostRecentScanTimestamp, new Bundle(), data);
		}
	}

}
