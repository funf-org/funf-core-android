package edu.mit.media.funf.probe.builtin;

import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.ProbeTestCase;

public class RunningApplicationsProbeTest extends ProbeTestCase<RunningApplicationsProbe> implements DataListener {

	public RunningApplicationsProbeTest() {
		super(RunningApplicationsProbe.class);
	}

	public void testApps() throws InterruptedException {
		RunningApplicationsProbe probe = getProbe(null);
		probe.registerListener(this);
		Thread.sleep(100000L);
		probe.unregisterListener(this);
		
	}
	

	@Override
	public void onDataReceived(Uri completeProbeUri, JsonObject data) {
		Log.i("MyData", data.toString());
	}

	@Override
	public void onDataCompleted(Uri completeProbeUri, JsonElement checkpoint) {
		// TODO Auto-generated method stub
		
	}
}
