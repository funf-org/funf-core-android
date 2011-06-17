package edu.mit.media.hd.funf.probe;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ProbeCommunicator {
	
	private final Context context;
	
	public ProbeCommunicator(Context context) {
		this.context = context;
	}
	
	public void requestStatusFromAll() {
		Intent i = new Intent(Utils.getStatusRequestAction());
		context.sendBroadcast(i);
	}
	
	public void requestStatusFrom(Class<? extends Probe> probeClass) {
		// TODO: to be implemented in ProbeController first
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public void registerDataRequest(Class<? extends Probe> probeClass, Bundle... params) {
		Intent i = new Intent(probeClass.getName());
		i.setPackage(context.getPackageName());
		i.putExtra("PARAMETERS", params);
		context.sendBroadcast(i);
	}
	
	public void unregisterDataRequest(Class<? extends Probe> probeClass) {
		registerDataRequest(probeClass); // Blank data request
	}
}
