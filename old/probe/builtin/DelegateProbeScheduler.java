package edu.mit.media.funf.probe.builtin;

import java.util.ArrayList;
import java.util.Collection;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.DefaultProbeScheduler;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeCommandServiceConnection;
import edu.mit.media.funf.probe.ProbeScheduler;

public class DelegateProbeScheduler implements ProbeScheduler {

	private Class<? extends Probe> delegateProbeClass;
	private PendingIntent callback;
	
	public DelegateProbeScheduler(Class<? extends Probe> delegateProbeClass) {
		this(delegateProbeClass, null);
	}
	
	public DelegateProbeScheduler(Class<? extends Probe> delegateProbeClass, PendingIntent callback) {
		this.delegateProbeClass = delegateProbeClass;
		this.callback = callback;
	}
	
	
	@Override
	public Long scheduleNextRun(Probe probe, Collection<Intent> requests) {
		PendingIntent theCallback = callback;
		if (theCallback == null) {
			Intent callbackIntent = new Intent(probe, probe.getClass());
			theCallback = PendingIntent.getService(probe, 0, callbackIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		Intent delegateRequest = new Intent(Probe.ACTION_REQUEST, null, probe, delegateProbeClass);
		delegateRequest.putExtra(Probe.CALLBACK_KEY, theCallback);
		ArrayList<Bundle> dataRequests = new ArrayList<Bundle>();
		for (Intent request : requests) {
			ArrayList<Bundle> individualDataRequests = Utils.getArrayList(request.getExtras(), Probe.REQUESTS_KEY);
			ArrayList<Bundle> individualDataRequestsWithDefaults = new ArrayList<Bundle>();
			for (Bundle individualDataRequest : individualDataRequests) {
				Bundle individualDataRequestWithDefaults = DefaultProbeScheduler.getCompleteParams(probe, individualDataRequest);
				individualDataRequestsWithDefaults.add(individualDataRequestWithDefaults);
			}
			dataRequests.addAll(individualDataRequestsWithDefaults);
		}
		Log.d("TEST_TEST_TEST", "Requests from " + probe.getClass().getName() + " to " + delegateProbeClass.getName() + ": " + dataRequests);
		delegateRequest.putExtra(Probe.REQUESTS_KEY, dataRequests);
		probe.startService(delegateRequest);
		
		// Figure out the next run time of the delegate probe
		// TODO: may want to remove this and return an estimate value, then wait for a status update from the delegate probe
		final long[] nextRunTime = new long[] { 0L };
		/*
		ProbeCommandServiceConnection delegateProbe = new ProbeCommandServiceConnection(probe, delegateProbeClass) {
			@Override
			public void runCommand() {
				nextRunTime[0] = getProbe().getNextRunTime();
			}
		};
		try {
			delegateProbe.join();
		} catch (InterruptedException e) {
		}*/
		return nextRunTime[0];
	}



	@Override
	public Bundle startRunningNow(Probe probe, Collection<Intent> requests) {
		return null;  // Never run directly, always run based off of other data
	}



	@Override
	public boolean shouldBeEnabled(Probe probe, Collection<Intent> requests) {
		return true; // Only the delegate needs to be enabled
	}

}
