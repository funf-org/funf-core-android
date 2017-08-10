/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
