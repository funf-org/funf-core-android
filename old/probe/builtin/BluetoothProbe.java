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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BluetoothKeys;


public class BluetoothProbe extends Probe implements BluetoothKeys {

	private BluetoothAdapter adapter;
	private BluetoothScanReceiver receiver;
	private ArrayList<Bundle> deviceDiscoveries;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.PERIOD, 300L),
			new Parameter(Parameter.Builtin.DURATION, 30L),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return new String[] {
			"android.hardware.bluetooth"
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.BLUETOOTH,
			android.Manifest.permission.BLUETOOTH_ADMIN,	
		};
	}
	
	@Override
	protected String getDisplayName() {
		return "Nearby Bluetooth Devices Probe";
	}

	@Override
	protected void onEnable() {
		adapter = BluetoothAdapter.getDefaultAdapter();
		deviceDiscoveries = null;
		receiver = new BluetoothScanReceiver();
		IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, intentFilter);
	}
	
	@Override
	protected void onDisable() {
		unregisterReceiver(receiver);
	}

	@Override
	protected void onRun(Bundle params) {
		if (deviceDiscoveries == null) {
			deviceDiscoveries = new ArrayList<Bundle>();
			startDiscovery();
		}
	}
	
	private void startDiscovery() {
		if (adapter.isEnabled()) {
			adapter.startDiscovery();
		} else {
			// TODO: save reference to this receiver to unregister on stop
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
					if (newState == BluetoothAdapter.STATE_ON) {
						context.unregisterReceiver(this);
						startDiscovery();
					}
				}
			}, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
			adapter.enable();
		}
	}

	@Override
	protected void onStop() {
		if (adapter.isDiscovering()) {
			adapter.cancelDiscovery();
		}
		resetDiscoveries();
	}
	
	private void resetDiscoveries() {
		deviceDiscoveries = null;
	}

	@Override
	public void sendProbeData() {
		if (deviceDiscoveries != null) {
			Bundle data = new Bundle();
			data.putParcelableArrayList(DEVICES, deviceDiscoveries);
			sendProbeData(Utils.getTimestamp(), data);
		}
	}

	private class BluetoothScanReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				if (deviceDiscoveries == null) {
					deviceDiscoveries = new ArrayList<Bundle>();
				}
				deviceDiscoveries.add(intent.getExtras());
			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				sendProbeData();
				if (isRunning()) {
					stop();
				} else {
					resetDiscoveries();
				}
			}
		}
		
	}
	
}
