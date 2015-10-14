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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ScreenKeys;

@DisplayName("Screen On/Off")
@Description("Records when the screen turns off and on.")
@Schedule.DefaultSchedule(interval=0, duration=0, opportunistic=true)
public class ScreenProbe extends Base implements ContinuousProbe, ScreenKeys  {

	private BroadcastReceiver screenReceiver;
	
	@Override
	protected void onEnable() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		screenReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();

				JsonObject data = new JsonObject();
				if (Intent.ACTION_SCREEN_OFF.equals(action)) {
					data.addProperty(SCREEN_ON, false);
					sendData(data);
				} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
					data.addProperty(SCREEN_ON, true);
					sendData(data);
				}
			}
		};
		getContext().registerReceiver(screenReceiver, filter);
	}
	
	

	@Override
	protected void onStart() {
		super.onStart();
	}



	@Override
	protected void onDisable() {
		getContext().unregisterReceiver(screenReceiver);
	}



	@Override
	protected boolean isWakeLockedWhileRunning() {
		return false;
	}

	
	
}
