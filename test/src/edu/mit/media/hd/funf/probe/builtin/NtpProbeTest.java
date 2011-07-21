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

import edu.mit.media.hd.funf.probe.Probe.SystemParameter;
import android.os.Bundle;

public class NtpProbeTest extends ProbeTestCase<NtpProbe> {

	public NtpProbeTest() {
		super(NtpProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(20);
		assertNotNull(data.get(NtpProbe.TIME_OFFSET));
		//System.out.println("Time Offset:" + data.getDouble(NtpProbe.TIME_OFFSET));
	}
}
