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
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class GravitySensorProbeTest extends ProbeTestCase<GravitySensorProbe> {

	public GravitySensorProbeTest() {
		super(GravitySensorProbe.class);
	}
	
	
	public void testData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 3L);
		params.putLong(SystemParameter.PERIOD.name, 10L);
		startProbe(params);
		for (int i=0; i<3; i++) {
			Bundle data = getData(10);
			assertTrue(data.containsKey("SENSOR"));
			assertTrue(data.containsKey("EVENT_TIMESTAMP"));
			assertTrue(data.containsKey("ACCURACY"));
			assertTrue(data.containsKey("X"));
			assertTrue(data.containsKey("Y"));
			assertTrue(data.containsKey("Z"));
			assertTrue(data.containsKey("TIMESTAMP"));
			long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
			int[] accuracy = data.getIntArray("ACCURACY");
			float[] x = data.getFloatArray("X");
			float[] y = data.getFloatArray("Y");
			float[] z = data.getFloatArray("Z");
			int numEvents = eventTimestamp.length;
			assertEquals(numEvents, accuracy.length);
			assertEquals(numEvents, x.length);
			assertEquals(numEvents, y.length);
			assertEquals(numEvents, z.length);
//			System.out.println("@" + data.getLong("TIMESTAMP") + " - " +
//					"X:" + data.getFloat("X") +
//					"Y:" + data.getFloat("Y") +
//					"Z:" + data.getFloat("Z")
//			);
		}
	}
	

}
