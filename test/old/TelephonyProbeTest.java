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

import edu.mit.media.funf.probe.ProbeTestCase;
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
