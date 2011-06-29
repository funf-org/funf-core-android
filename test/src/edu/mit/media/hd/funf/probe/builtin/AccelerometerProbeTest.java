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

import android.os.Bundle;

public class AccelerometerProbeTest extends ProbeTestCase<AccelerometerProbe> {

	public AccelerometerProbeTest() {
		super(AccelerometerProbe.class);
	}
	
	
	public void testAccelerometerData() throws InterruptedException {
		Bundle params = new Bundle();
		startProbe(params);
		for (int i=0; i<10; i++) {
			Bundle data = getData(100);
			assertTrue(data.containsKey("X"));
			assertTrue(data.containsKey("Y"));
			assertTrue(data.containsKey("Z"));
			assertTrue(data.containsKey("TIMESTAMP"));
//			System.out.println("@" + data.getLong("TIMESTAMP") + " - " +
//					"X:" + data.getFloat("X") +
//					"Y:" + data.getFloat("Y") +
//					"Z:" + data.getFloat("Z")
//			);
		}
		stopProbe();
	}
	
	public void testBroadcast() throws InterruptedException {
		Bundle params = new Bundle();
		sendDataRequestBroadcast(params);
		Bundle data = getData(10);
		assertNotNull(data);
	}

}
