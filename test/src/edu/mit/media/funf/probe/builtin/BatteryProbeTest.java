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

import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

public class BatteryProbeTest extends ProbeTestCase<BatteryProbe> {

	public BatteryProbeTest() {
		super(BatteryProbe.class);
	}
	
	public void testProbe() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(20);
		assertNotNull(data.get(BatteryManager.EXTRA_LEVEL));
	}

}
