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
		return new Parameter[] {
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
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
	protected String getDisplayName() {
		return "Screen On/Off State Probe";
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
			sendProbeData(Utils.getTimestamp(), data);
		}
	}
	

}
