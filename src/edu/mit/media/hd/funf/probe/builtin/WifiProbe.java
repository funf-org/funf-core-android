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
import edu.mit.media.hd.funf.probe.Probe;

public class WifiProbe extends Probe {

	public static final String DATA_SCAN_RESULTS = "SCAN_RESULTS";
	
	private static final String TAG = WifiProbe.class.getName();
	
	private WifiManager wifiManager;
	private int numberOfAttempts;
	private int previousWifiState;  // TODO: should this be persisted to disk?
	private BroadcastReceiver scanResultsReceiver;
	private WifiLock wifiLock;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(SystemParameter.START, 0L),  // No start by default
				new Parameter(SystemParameter.PERIOD, 5L),	// Run every 5 seconds by default
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
				"android.hardware.location"
		};
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putParcelableArrayList(DATA_SCAN_RESULTS, new ArrayList<ScanResult>(wifiManager.getScanResults()));
		sendProbeData(System.currentTimeMillis(), new Bundle(), data);
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
					unregisterReceiver(this);
					saveWifiStateAndRunScan();				
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
					unregisterReceiver(this);
					runScan();
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
