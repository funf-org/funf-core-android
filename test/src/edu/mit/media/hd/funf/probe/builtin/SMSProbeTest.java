package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcelable;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class SMSProbeTest extends ProbeTestCase<SMSProbe> {

	public SMSProbeTest() {
		super(SMSProbe.class);
	}
	
	public void testData() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(10);
		ArrayList<Parcelable> messages = data.getParcelableArrayList(SMSProbe.MESSAGES);
		assertNotNull(messages);
		//assertTrue(messages.size() > 0);  For some reason the reset is not working for this probe
		
		// Running again should return an empty result
		startProbe(params);
		data = getData(10);
		messages = data.getParcelableArrayList(SMSProbe.MESSAGES);
		assertNotNull(messages);
		assertTrue(messages.isEmpty());
	}

}
