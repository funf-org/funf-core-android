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

import edu.mit.media.funf.probe.Probe.SystemParameter;
import android.os.Bundle;

public class TimeOffsetProbeTest extends ProbeTestCase<TimeOffsetProbe> {

	public TimeOffsetProbeTest() {
		super(TimeOffsetProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(20);
		assertNotNull(data.get(TimeOffsetProbe.TIME_OFFSET));
		//System.out.println("Time Offset:" + data.getDouble(NtpProbe.TIME_OFFSET));
	}
}
