package edu.mit.media.hd.funf.probe.builtin;

import android.os.Bundle;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.probe.Probe;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class CellProbeTest extends ProbeTestCase<CellProbe> {

	public CellProbeTest() {
		super(CellProbe.class);
	}
	
	public void testProbe() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(5);
		assertNotNull(data.get(Probe.TIMESTAMP));
		System.out.println(Utils.join(Utils.getValues(data).keySet(), ", "));
	}

}
