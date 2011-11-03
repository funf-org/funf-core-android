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

import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.probe.ProbeTestCase;

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
		Log.i(TAG, String.valueOf(data));
		for (String key : keysToCheck) {
			assertNotNull("Key does not exist: " + key, data.getString(key));
			System.out.println(key + ": " + data.getString(key));
		}
	}
	
}
