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

import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.HardwareInfoKeys;

public class HardwareInfoProbe extends SynchronousProbe implements HardwareInfoKeys {

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.ACCESS_WIFI_STATE,
				android.Manifest.permission.BLUETOOTH
		};
	}
	
	@Override
	protected long getDefaultPeriod() {
		return 604800L;
	}


	protected Bundle getData() {
		Bundle data = new Bundle();
		data.putString(WIFI_MAC, ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo().getMacAddress());
		String bluetoothMac = getBluetoothMac();
		if (bluetoothMac != null) {
			data.putString(BLUETOOTH_MAC, bluetoothMac);
		}
		data.putString(ANDROID_ID, Secure.getString(getContentResolver(), Secure.ANDROID_ID));
		data.putString(BRAND, Build.BRAND);
		data.putString(MODEL, Build.MODEL);
		data.putString(DEVICE_ID, ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
		return data;
	}

	private String getBluetoothMac() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return (adapter != null) ? adapter.getAddress() : null;
	}
}
