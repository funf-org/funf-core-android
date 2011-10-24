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
import java.util.List;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.WifiKeys;

public class WifiProbe extends Probe implements WifiKeys {

	public static final long DEFAULT_PERIOD = 60L * 5L;
	
	private static final String TAG = WifiProbe.class.getName();
	
	private WifiManager wifiManager;
	private int numberOfAttempts;
	private int previousWifiState;  // TODO: should this be persisted to disk?
	private BroadcastReceiver scanResultsReceiver;
	private WifiLock wifiLock;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.PERIOD, DEFAULT_PERIOD),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.CHANGE_WIFI_STATE,
		};
	}
	

	@Override
	public String[] getRequiredFeatures() {
		return new String[] {
				"android.hardware.wifi"
		};
	}
	
	@Override
	protected String getDisplayName() {
		return "Nearby Wifi Devices Probe";
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		List<ScanResult> results = wifiManager.getScanResults();
		ArrayList<ScanResult> nonNullResults = new ArrayList<ScanResult>();
		if (results != null) {
			nonNullResults.addAll(results);
		}
		data.putParcelableArrayList(SCAN_RESULTS, nonNullResults);
		sendProbeData(Utils.getTimestamp(), data);
	}
	
	@Override
	protected void onEnable() {
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		numberOfAttempts = 0;
		scanResultsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
					sendProbeData();
					if (isRunning()) {
						stop();
					}
				}
			}
		};
		registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}
	
	@Override
	protected void onDisable() {
		unregisterReceiver(scanResultsReceiver);
	}

	@Override
	public void onRun(Bundle params) {
		acquireWifiLock();
		saveWifiStateAndRunScan();
	}
	
	private void saveWifiStateAndRunScan() {
		int state = wifiManager.getWifiState();
		if(state==WifiManager.WIFI_STATE_DISABLING ||state==WifiManager.WIFI_STATE_ENABLING){
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context ctx, Intent i) {
					if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
						try {
							unregisterReceiver(this);  // TODO: sometimes this throws an IllegalArgumentException
							saveWifiStateAndRunScan();
						} catch (IllegalArgumentException e) {
							Log.e(TAG, "Unregistered WIFIE_STATE_CHANGED receiver more than once.");
						}
					}
				}
			}, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		} else {
			previousWifiState = state;
			runScan();
		}
	}
	
	private void loadPreviousWifiState() {
		// Enable wifi if previous sate was enabled, otherwise disable
		wifiManager.setWifiEnabled(previousWifiState == WifiManager.WIFI_STATE_ENABLED);
	}
	
	private void runScan() {
		numberOfAttempts += 1; 
		int state = wifiManager.getWifiState();
		if (state == WifiManager.WIFI_STATE_ENABLED) {
			boolean successfulStart = wifiManager.startScan();
			if (successfulStart) {
				Log.i(TAG, "WIFI scan started succesfully");
			} else {
				Log.e(TAG, "WIFI scan failed.");
			}
			numberOfAttempts = 0;
		} else if (numberOfAttempts <= 3) { 
			// Prevent infinite recursion by keeping track of number of attempts to change wifi state
			// TODO: investigate what is needed to keep Service alive while waiting for wifi state
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context ctx, Intent i) {
					if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
						try {
							unregisterReceiver(this);
							runScan();
						} catch (IllegalArgumentException e) {
							// Not sure why, but sometimes this is not registered
							// Probably two intents at once
						}
					}
				}
			}, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
			wifiManager.setWifiEnabled(true);
		} else {  // After 3 attempts stop trying
			// TODO: possibly send error
			stop();
		}
		
	}

	@Override
	public void onStop() {
		releaseWifiLock();
		loadPreviousWifiState();
	}

	private void acquireWifiLock() {
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
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

}
