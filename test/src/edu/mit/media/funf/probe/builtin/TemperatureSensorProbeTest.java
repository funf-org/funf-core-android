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

import junit.framework.AssertionFailedError;
import android.os.Bundle;
import edu.mit.media.funf.probe.Probe.SystemParameter;

public class TemperatureSensorProbeTest extends ProbeTestCase<TemperatureSensorProbe> {

	public TemperatureSensorProbeTest() {
		super(TemperatureSensorProbe.class);
	}
	
	
	public void testData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 10L);
		params.putLong(SystemParameter.PERIOD.name, 10L);
		startProbe(params);
		try {
			Bundle data = getData(10);
			fail("Should only fail if temperature probe is present on device.");
		} catch (AssertionFailedError e) {
			// Uncommon probe should not start
		}
	}
	

}
