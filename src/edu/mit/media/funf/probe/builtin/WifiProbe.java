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



import java.util.List;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.util.LogUtil;

@Schedule.DefaultSchedule(interval=300)
@RequiredPermissions({Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE})
@RequiredFeatures("android.hardware.wifi")
@DisplayName("Nearby Wifi Devices Probe")
public class WifiProbe extends Base {

    public static final String TSF = "tsf";
  
	private static final String LOCK_KEY = WifiProbe.class.getName();
	
	private WifiManager wifiManager;
	private int numberOfAttempts;
	private int previousWifiState;  // TODO: should this be persisted to disk?
	private WifiLock wifiLock;
	private BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
				List<ScanResult> results = wifiManager.getScanResults();
				if (results != null) {
					Gson gson = getGson();
					for (ScanResult result : results) {
					  JsonObject data = gson.toJsonTree(result).getAsJsonObject();
					  if (data.has(TIMESTAMP)) {
					    JsonElement el = data.remove(TIMESTAMP);
					    data.add(TSF, el);
					  }
					  sendData(data);
					}
				}
				if (getState() == State.RUNNING) {
					stop();
				}
			}
		}
	};
	
	private BroadcastReceiver waitingToStartScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent i) {
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
				try {
					getContext().unregisterReceiver(this);  // TODO: sometimes this throws an IllegalArgumentException
					saveWifiStateAndRunScan();
				} catch (IllegalArgumentException e) {
					Log.e(LogUtil.TAG, "Unregistered WIFIE_STATE_CHANGED receiver more than once.");
				}
			}
		}
	};
	
	private BroadcastReceiver retryScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent i) {
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
				try {
					getContext().unregisterReceiver(this);
					runScan();
				} catch (IllegalArgumentException e) {
					// Not sure why, but sometimes this is not registered
					// Probably two intents at once
				}
			}
		}
	};
	
	
	
	@Override
	protected void onEnable() {
		super.onEnable();
		wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
		numberOfAttempts = 0;
		getContext().registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}

	@Override
	protected void onStart() {
		super.onStart();
		acquireWifiLock();
		saveWifiStateAndRunScan();
	}

	@Override
	protected void onStop() {
		super.onStop();
		releaseWifiLock();
		loadPreviousWifiState();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		getContext().unregisterReceiver(scanResultsReceiver);
	}

	private void loadPreviousWifiState() {
		// Enable wifi if previous sate was enabled, otherwise disable
		wifiManager.setWifiEnabled(previousWifiState == WifiManager.WIFI_STATE_ENABLED);
	}
	
	private void saveWifiStateAndRunScan() {
		int state = wifiManager.getWifiState();
		if(state==WifiManager.WIFI_STATE_DISABLING ||state==WifiManager.WIFI_STATE_ENABLING){
			// Wait until the Wifi state stabilizes, then run
			getContext().registerReceiver(waitingToStartScanReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		} else {
			previousWifiState = state;
			runScan();
		}
	}
	
	private void acquireWifiLock() {
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, LOCK_KEY);
		wifiLock.setReferenceCounted(false);
		wifiLock.acquire();
	}
	
	private void releaseWifiLock() {
		if (wifiLock != null) {
			if (wifiLock.isHeld()) {
				wifiLock.release();
			}
			wifiLock = null;
		}
	}
	
	private void runScan() {
		numberOfAttempts += 1; 
		int state = wifiManager.getWifiState();
		if (state == WifiManager.WIFI_STATE_ENABLED) {
			boolean successfulStart = wifiManager.startScan();
			if (successfulStart) {
				Log.i(LogUtil.TAG, "WIFI scan started succesfully");
			} else {
				Log.e(LogUtil.TAG, "WIFI scan failed.");
			}
			numberOfAttempts = 0;
		} else if (numberOfAttempts <= 3) { 
			// Prevent infinite recursion by keeping track of number of attempts to change wifi state
			getContext().registerReceiver(retryScanReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
			wifiManager.setWifiEnabled(true);
		} else {  // After 3 attempts stop trying
			// TODO: possibly send error
			stop();
		}
		
	}
}
