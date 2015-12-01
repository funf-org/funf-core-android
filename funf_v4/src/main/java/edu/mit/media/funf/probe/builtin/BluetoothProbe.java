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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
	private BigDecimal maxScanTime = BigDecimal.valueOf(60.0);

	@Configurable
	private boolean include_scan_started = true;

	@Configurable
	private boolean keepBluetoothVisible = false;
	
	private BluetoothAdapter adapter;	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				Gson gson = getGsonBuilder().addSerializationExclusionStrategy(new BluetoothExclusionStrategy()).create();
				sendData(gson.toJsonTree(intent.getExtras()).getAsJsonObject());
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

	private JsonObject createScanStartedData() {
		JsonObject data = new JsonObject();
		JsonObject addressData = new JsonObject();
		addressData.addProperty("mAddress", "00:00:00:00:00:00");
		data.add("android.bluetooth.device.extra.DEVICE", addressData);
		data.addProperty("android.bluetooth.device.extra.NAME", "DUMMY_SCAN_STARTED");
		return data;
	}

	private void sendScanStartedData() {
		JsonObject data = createScanStartedData();
		sendData(data);
	}
	
	private boolean shouldDisableOnFinish = false; // Keeps track of previous bluetooth state
	private void startDiscovery() {
		if (adapter.isEnabled()) {
			adapter.startDiscovery();
			if (include_scan_started) sendScanStartedData();
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
		if (keepBluetoothVisible) {
			makeVisible();
		}
	}

	private void makeVisible() {
		if(adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
			discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.getContext().startActivity(discoverableIntent);
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

	public class BluetoothExclusionStrategy implements ExclusionStrategy {
		public boolean shouldSkipClass(Class<?> cls) {
			return false;
		}
		public boolean shouldSkipField(FieldAttributes f) {
			String name = f.getName();
			return (f.getDeclaringClass() == BluetoothDevice.class &&
					(name.equals("XXX")
							//here we can remove fields from the scan result if we want to
					)
			);
		}
	}
	
}
