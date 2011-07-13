package edu.mit.media.hd.funf.probe.builtin;

import edu.mit.media.hd.funf.probe.SynchronousProbe;
import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class HardwareInfoProbe extends SynchronousProbe {

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.ACCESS_WIFI_STATE,
				android.Manifest.permission.BLUETOOTH
		};
	}

	
	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putString("WIFI_MAC", ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo().getMacAddress());
		String bluetoothMac = getBluetoothMac();
		if (bluetoothMac != null) {
			data.putString("BLUETOOTH_MAC", bluetoothMac);
		}
		data.putString("ANDROID_ID", Secure.getString(getContentResolver(), Secure.ANDROID_ID));
		data.putString("BRAND", Build.BRAND);
		data.putString("MODEL", Build.MODEL);
		data.putString("DEVICE_ID", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
		sendProbeData(System.currentTimeMillis(), new Bundle(), data);
	}

	private String getBluetoothMac() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return (adapter != null) ? adapter.getAddress() : null;
	}
}
