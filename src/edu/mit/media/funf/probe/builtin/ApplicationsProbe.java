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



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ApplicationsKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

public class ApplicationsProbe extends ImpulseProbe implements PassiveProbe, ApplicationsKeys{
	
	private PackageManager pm;
	
	private BroadcastReceiver packageChangeListener = new BroadcastReceiver()  {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			try {
				if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
					ApplicationInfo info = pm.getApplicationInfo(intent.getDataString(), 0);
					sendData(info, true, TimeUtil.getTimestamp());
				} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					ApplicationInfo info = pm.getApplicationInfo(intent.getDataString(), PackageManager.GET_UNINSTALLED_PACKAGES);
					sendData(info, false, TimeUtil.getTimestamp());
				}
			} catch (NameNotFoundException e) {
				Log.w(LogUtil.TAG, "ApplicationsProbe: Package not found '" + intent.getDataString() + "'");
			}
		}
	};
	
	@Override
	protected void onEnable() {
		super.onEnable();
		pm = getContext().getPackageManager();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		getContext().registerReceiver(packageChangeListener, filter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		List<ApplicationInfo> allApplications = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		List<ApplicationInfo> installedApplications = new ArrayList<ApplicationInfo>(pm.getInstalledApplications(0));
		List<ApplicationInfo> uninstalledApplications = getUninstalledApps(allApplications, installedApplications);
		for (ApplicationInfo info : installedApplications) {
			sendData(info, true, null);
		}
		for (ApplicationInfo info : uninstalledApplications) {
			sendData(info, false, null);
		}
		stop();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		getContext().unregisterReceiver(packageChangeListener);
	}

	private void sendData(ApplicationInfo info, boolean installed, BigDecimal installedTimestamp) {
		JsonObject data = getGson().toJsonTree(info).getAsJsonObject();
		data.addProperty(INSTALLED, installed);
		data.add(INSTALLED_TIMESTAMP, getGson().toJsonTree(installedTimestamp));
		sendData(data);
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
}
