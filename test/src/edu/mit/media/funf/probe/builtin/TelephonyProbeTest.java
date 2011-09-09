package edu.mit.media.funf.probe.builtin;

import android.os.Bundle;

public class TelephonyProbeTest extends ProbeTestCase<TelephonyProbe> {

	public TelephonyProbeTest() {
		super(TelephonyProbe.class);
	}
	
	public void testData() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(5);
		String[] stringKeys = new String[] {
				"DEVICE_ID",
				"DEVICE_SOFTWARE_VERSION",
				"LINE_1_NUMBER",
				"NETWORK_COUNTRY_ISO",
				"NETWORK_OPERATOR",
				"NETWORK_OPERATOR_NAME",
				"SIM_COUNTRY_ISO",
				"SIM_OPERATOR",
				"SIM_OPERATOR_NAME",
				"SIM_SERIAL_NUMBER",
				"SUBSCRIBER_ID",
				"VOICEMAIL_ALPHA_TAG",
				"VOICEMAIL_NUMBER",
		};
		String[] otherKeys = new String[] {
				"CALL_STATE",
				"NETWORK_TYPE",
				"PHONE_TYPE",
				"SIM_STATE",
				"HAS_ICC_CARD"
		};
		for (String key : stringKeys) {
			assertNotNull(data.getString(key));
		}
		for (String key : otherKeys) {
			assertNotNull(data.get(key));
		}
	}

}
