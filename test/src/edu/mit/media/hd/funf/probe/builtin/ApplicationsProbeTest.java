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

public class ApplicationsProbeTest extends ProbeTestCase<ApplicationsProbe> {

	public ApplicationsProbeTest() {
		super(ApplicationsProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(20);
		assertNotNull(data.get("INSTALLED_APPLICATIONS"));
		assertNotNull(data.get("UNINSTALLED_APPLICATIONS"));
	}
}
