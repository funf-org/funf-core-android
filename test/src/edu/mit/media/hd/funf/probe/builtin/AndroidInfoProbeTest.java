package edu.mit.media.hd.funf.probe.builtin;

import android.os.Bundle;

public class AndroidInfoProbeTest extends ProbeTestCase<HardwareInfoProbe> {

	public AndroidInfoProbeTest() {
		super(HardwareInfoProbe.class);
	}

	public void testData() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(5);
		String[] keysToCheck = new String[] {
				"WIFI_MAC",
				"ANDROID_ID",
				"BLUETOOTH_MAC",
				"BRAND",
				"MODEL",
				"DEVICE_ID"
		};
		for (String key : keysToCheck) {
			assertNotNull(data.getString(key));
			System.out.println(key + ": " + data.getString(key));
		}
	}
	
}
