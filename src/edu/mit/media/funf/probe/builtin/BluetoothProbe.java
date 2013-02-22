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

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.math.BigDecimal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.time.TimeUtil;

@DisplayName("Nearby Bluetooth Devices Probe")
@Schedule.DefaultSchedule(interval=300)
@RequiredFeatures("android.hardware.bluetooth")
@RequiredPermissions({android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN})
public class BluetoothProbe extends Base implements PassiveProbe {
	
	@Configurable
	private BigDecimal maxScanTime = BigDecimal.valueOf(30.0);
	
	private BluetoothAdapter adapter;	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				sendData(getGson().toJsonTree(intent.getExtras()).getAsJsonObject());
			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				stop();
			}
		}
	};
	private BroadcastReceiver stateChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			if (newState == BluetoothAdapter.STATE_ON) {
				try { 
					getContext().unregisterReceiver(this);
					startDiscovery();
				} catch (IllegalArgumentException e) {
					// Already called this code, or probe has stopped
				}
			}
		}
	};
	
	private boolean shouldDisableOnFinish = false; // Keeps track of previous bluetooth state
	private void startDiscovery() {
		if (adapter.isEnabled()) {
			adapter.startDiscovery();
		} else {
			shouldDisableOnFinish = true;
			getContext().registerReceiver(stateChangedReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
			adapter.enable();
		}
	}
	
	@Override
	protected void onEnable() {
		adapter = BluetoothAdapter.getDefaultAdapter();
		IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		getContext().registerReceiver(receiver, intentFilter);
	}
	
	
	
	@Override
	protected void onStart() {
		super.onStart();
		startDiscovery();
		if (maxScanTime != null) {
			getHandler().sendMessageDelayed(getHandler().obtainMessage(STOP_MESSAGE), TimeUtil.secondsToMillis(maxScanTime));
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		getHandler().removeMessages(STOP_MESSAGE);
		try { 
			getContext().unregisterReceiver(stateChangedReceiver);
		} catch (IllegalArgumentException e) {
			// Previously stopped
		}
		if (adapter.isDiscovering()) {
			adapter.cancelDiscovery();
		}
		if (shouldDisableOnFinish) {
			adapter.disable();
			shouldDisableOnFinish = false;
		}
	}

	@Override
	protected void onDisable() {
		try {
			getContext().unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			Log.w(TAG, getClass().getName() + "Broadcast receiver not registered.", e);
		}
	}
	
	
}
