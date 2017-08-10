/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
