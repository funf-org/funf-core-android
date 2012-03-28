package edu.mit.media.funf.probe.builtin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@DisplayName("Nearby Bluetooth Devices Probe")
@DefaultSchedule(period=300, duration=30)
@RequiredFeatures("android.hardware.bluetooth")
@RequiredPermissions({android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN})
public class BluetoothProbe extends Base implements ContinuousProbe, PassiveProbe {
	
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
	}

	@Override
	protected void onStop() {
		super.onStop();
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
		getContext().unregisterReceiver(receiver);
	}
	
	
}
