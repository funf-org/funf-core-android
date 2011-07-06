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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;

public class BatteryProbe extends Probe {

	private BroadcastReceiver receiver;
	private Bundle mostRecentData;
	private long mostRecentTimestamp;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return null;
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.BATTERY_STATS,	
		};
	}

	@Override
	protected void onEnable() {
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mostRecentTimestamp = System.currentTimeMillis();
				mostRecentData = intent.getExtras();
				sendProbeData();
			}
		};
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}
	
	@Override
	protected void onDisable() {
		unregisterReceiver(receiver);
	}


	@Override
	protected void onRun(Bundle params) {
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		// Nothing to stop, passive only
	}

	@Override
	public void sendProbeData() {
		if (mostRecentData != null) {
			sendProbeData(mostRecentTimestamp, new Bundle(), mostRecentData);
		}
	}

}
