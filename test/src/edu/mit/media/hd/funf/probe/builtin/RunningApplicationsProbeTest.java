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
import edu.mit.media.hd.funf.OppProbe;

public class RunningApplicationsProbeTest extends ProbeTestCase<RunningApplicationsProbe> {

	public RunningApplicationsProbeTest() {
		super(RunningApplicationsProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		params.putString(OppProbe.ReservedParamaters.REQUESTER.name, getTestRequester());
		startProbe(params);
		Bundle data = getData(20);
		assertNotNull(data.get("RUNNING_TASKS"));
	}
}
