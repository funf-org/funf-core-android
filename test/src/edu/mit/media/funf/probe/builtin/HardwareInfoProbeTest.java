package edu.mit.media.funf.probe.builtin;

import android.os.Bundle;

public class HardwareInfoProbeTest extends ProbeTestCase<HardwareInfoProbe> {

	public HardwareInfoProbeTest() {
		super(HardwareInfoProbe.class);
	}

	public void testData() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(5);
		String[] keysToCheck = new String[] {
				"WIFI_MAC",
				"ANDROID_ID",
				//"BLUETOOTH_MAC",  Does not exist on some phones (Cliq)
				"BRAND",
				"MODEL",
				"DEVICE_ID"
		};
		for (String key : keysToCheck) {
			assertNotNull("Key does not exist: " + key, data.getString(key));
			System.out.println(key + ": " + data.getString(key));
		}
	}
	
}
