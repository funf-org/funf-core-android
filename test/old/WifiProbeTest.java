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

import java.util.ArrayList;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Parcelable;
import edu.mit.media.funf.opp.OppProbe;
import edu.mit.media.funf.probe.ProbeTestCase;

public class WifiProbeTest extends ProbeTestCase<WifiProbe> {

	private static final int FUDGE_FACTOR = 20;
	
	public WifiProbeTest() {
		super(WifiProbe.class);
	}
	
	public void testWifiProbe() throws InterruptedException {
		Bundle params = new Bundle();
		params.putString(OppProbe.ReservedParamaters.PACKAGE.name, getTestRequester());
		startProbe(params);
		Bundle data = getData(10 + FUDGE_FACTOR);
		ArrayList<Parcelable> scanResults = data.getParcelableArrayList(WifiProbe.SCAN_RESULTS);
		assertNotNull(scanResults);
		assertFalse(scanResults.isEmpty());
		for (Parcelable result : scanResults) {
			System.out.println("SSID: " + ((ScanResult)result).SSID);
		}
	}
	
}
