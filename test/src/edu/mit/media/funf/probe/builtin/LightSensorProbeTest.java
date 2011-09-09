/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.funf.probe.builtin;

import android.os.Bundle;
import edu.mit.media.funf.probe.Probe.SystemParameter;

public class LightSensorProbeTest extends ProbeTestCase<LightSensorProbe> {

	public LightSensorProbeTest() {
		super(LightSensorProbe.class);
	}
	
	
	public void testData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 10L);
		params.putLong(SystemParameter.PERIOD.name, 10L);
		startProbe(params);
		
		Bundle data = getData(10);
		assertTrue(data.containsKey("SENSOR"));
		assertTrue(data.containsKey("EVENT_TIMESTAMP"));
		assertTrue(data.containsKey("ACCURACY"));
		assertTrue(data.containsKey("LUX"));
		assertTrue(data.containsKey("TIMESTAMP"));
		long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
		int[] accuracy = data.getIntArray("ACCURACY");
		float[] lux = data.getFloatArray("LUX");
		int numEvents = eventTimestamp.length;
		assertEquals(numEvents, accuracy.length);
		assertEquals(numEvents, lux.length);
		System.out.println("@" + data.getLong("TIMESTAMP") + " - " +
				"LUX Count:" + lux.length
		);
	}
	

}
