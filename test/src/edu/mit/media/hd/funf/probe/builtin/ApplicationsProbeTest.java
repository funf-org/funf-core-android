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
import android.util.Log;

public class ApplicationsProbeTest extends ProbeTestCase<ApplicationsProbe> {

	public ApplicationsProbeTest() {
		super(ApplicationsProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		Log.i(TAG, "1");
		startProbe(params);
		Log.i(TAG, "2");
		Bundle data = getData(20);
		assertNotNull(data.get("INSTALLED_APPLICATIONS"));
		assertNotNull(data.get("UNINSTALLED_APPLICATIONS"));
	}
}
