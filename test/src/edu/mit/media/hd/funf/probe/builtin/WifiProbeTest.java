/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Parcelable;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.probe.builtin.WifiProbe;

public class WifiProbeTest extends ProbeTestCase<WifiProbe> {

	private static final int FUDGE_FACTOR = 3;
	
	public WifiProbeTest() {
		super(WifiProbe.class);
	}
	
	public void testWifiProbe() throws InterruptedException {
		Bundle params = new Bundle();
		params.putString(OppProbe.ReservedParamaters.REQUESTER.name, getTestRequester());
		startProbe(params);
		Bundle data = getData(10 + FUDGE_FACTOR);
		ArrayList<Parcelable> scanResults = data.getParcelableArrayList(WifiProbe.DATA_SCAN_RESULTS);
		assertNotNull(scanResults);
		assertFalse(scanResults.isEmpty());
		for (Parcelable result : scanResults) {
			System.out.println("SSID: " + ((ScanResult)result).SSID);
		}
	}
	
}
