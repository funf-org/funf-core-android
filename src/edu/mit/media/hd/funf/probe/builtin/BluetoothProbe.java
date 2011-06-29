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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;


public class BluetoothProbe extends Probe {

	private BluetoothAdapter adapter;
	private BluetoothScanReceiver receiver;
	private ArrayList<Bundle> deviceDiscoveries;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 0L),
			new Parameter(SystemParameter.DURATION, 0L),
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
			data.putParcelableArrayList("DEVICES", deviceDiscoveries);
			sendProbeData(System.currentTimeMillis(), new Bundle(), data);
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
