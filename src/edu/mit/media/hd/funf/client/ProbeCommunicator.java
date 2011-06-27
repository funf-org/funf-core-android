package edu.mit.media.hd.funf.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.probe.Probe;

public class ProbeCommunicator {
	public static final String TAG = ProbeCommunicator.class.getName();
	
	private final Context context;
	
	public ProbeCommunicator(Context context) {
		this.context = context;
	}
	
	public void requestStatusFromAll() {
		Intent i = new Intent(OppProbe.getGlobalPollAction());
		context.sendBroadcast(i);
	}
	
	public void requestStatusFrom(Class<? extends Probe> probeClass) {
		// TODO: to be implemented in ProbeController first
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public void registerDataRequest(String probeName, Bundle... params) {
		Intent i = new Intent(OppProbe.getGetAction(probeName));
		Log.i(TAG, "Sending intent '" + i.getAction() + "'");
		i.setPackage(context.getPackageName());
		i.putExtra("PARAMETERS", params);
		i.putExtra("REQUESTER", context.getPackageName());
		context.sendBroadcast(i);
	}
	
	public void registerDataRequest(Class<? extends Probe> probeClass, Bundle... params) {
		registerDataRequest(probeClass.getName(), params);
	}
	
	public void unregisterDataRequest(String probeName) {
		registerDataRequest(probeName); // Blank data request
	}
	
	public void unregisterDataRequest(Class<? extends Probe> probeClass) {
		unregisterDataRequest(probeClass.getName());
	}
}
