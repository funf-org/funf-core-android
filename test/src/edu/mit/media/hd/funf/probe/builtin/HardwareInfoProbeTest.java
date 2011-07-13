package edu.mit.media.hd.funf.probe.builtin;

import android.os.Bundle;

public class HardwareInfoProbeTest extends ProbeTestCase<AndroidInfoProbe> {

	public HardwareInfoProbeTest() {
		super(AndroidInfoProbe.class);
	}

	public void testData() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(5);
		String[] keysToCheck = new String[] {
				"FIRMWARE_VERSION",
				"BUILD_NUMBER",
				"SDK",
		};
		for (String key : keysToCheck) {
			assertNotNull(data.get(key));
			System.out.println(key + ": " + String.valueOf(data.get(key)));
		}
	}
	
}
