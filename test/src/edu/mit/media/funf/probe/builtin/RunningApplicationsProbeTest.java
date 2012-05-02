package edu.mit.media.funf.probe.builtin;

import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.json.IJsonObject;
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
	public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
		Log.i("MyData", data.toString());
	}

	@Override
	public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
		// TODO Auto-generated method stub
		
	}
}
