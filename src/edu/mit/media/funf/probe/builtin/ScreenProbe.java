/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.funf.probe.builtin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ScreenKeys;

public class ScreenProbe extends Probe implements ScreenKeys {
	
	private BroadcastReceiver screenReceiver;
	private Boolean screenOn;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[]{};
	}

	@Override
	public String[] getRequiredFeatures() {
		return new String[]{};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}

	@Override
	protected void onEnable() {
		screenOn = null;
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		screenReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				if (Intent.ACTION_SCREEN_OFF.equals(action)) {
					screenOn = false;
					sendProbeData();
				} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
					screenOn = true;
					sendProbeData();
				}
			}
		};
		registerReceiver(screenReceiver, filter);
	}

	@Override
	protected void onDisable() {
		unregisterReceiver(screenReceiver);
	}


	@Override
	protected void onRun(Bundle params) {
		sendProbeData();
	}

	@Override
	protected void onStop() {
		// Only passive listener
	}

	@Override
	public void sendProbeData() {
		if (screenOn != null) {
			Bundle data = new Bundle();
			data.putBoolean(SCREEN_ON, screenOn);
			sendProbeData(Utils.getTimestamp(), new Bundle(), data);
		}
	}
	

}
