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

public class ActivityProbeTest extends ProbeTestCase<ActivityProbe> {

	public ActivityProbeTest() {
		super(ActivityProbe.class);
	}

	public void testData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 5L);
		params.putLong(SystemParameter.PERIOD.name, 0L);
		sendDataRequestBroadcast(params);
		Bundle data = getData(20);
		assertTrue(data.containsKey("TOTAL_INTERVALS"));
		assertTrue(data.containsKey("ACTIVE_INTERVALS"));
		//System.out.println("I: " + data.getInt("TOTAL_INTERVALS") + " A:" + data.getInt("ACTIVE_INTERVALS"));
	}
	
	public void testWithAccelerometerBroadcast() throws InterruptedException {
		sendDataRequestBroadcast(AccelerometerSensorProbe.class, new Bundle());
		
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 2L);
		params.putLong(SystemParameter.PERIOD.name, 0L);
		sendDataRequestBroadcast(params);
		Bundle data = getData(20);
		assertTrue(data.containsKey("TOTAL_INTERVALS"));
		assertTrue(data.containsKey("ACTIVE_INTERVALS"));
	}
}
