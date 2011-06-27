package edu.mit.media.hd.funf.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	
	public void requestStatusFrom(String probeName, boolean includeNonce) {
		final Intent i = new Intent(OppProbe.getPollAction(probeName));
		Log.i(TAG, "Sending intent '" + i.getAction() + "'");
		i.setPackage(context.getPackageName());
		i.putExtra(OppProbe.ReservedParamaters.REQUESTER.name, context.getPackageName());
		i.putExtra(OppProbe.ReservedParamaters.NONCE.name, includeNonce);
		context.sendBroadcast(i);
	}
	
	public void requestStatusFrom(String probeName) {
		requestStatusFrom(probeName, false);
	}
	
	public void requestStatusFrom(Class<? extends Probe> probeClass) {
		requestStatusFrom(probeClass.getName());
	}
	
	public void registerDataRequest(final String probeName, final Bundle... requests) {
		BroadcastReceiver statusReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long nonce = intent.getLongExtra(OppProbe.ReservedParamaters.NONCE.name, 0L);
				if (nonce != 0L) {
					final Intent i = new Intent(OppProbe.getGetAction(probeName));
					Log.i(TAG, "Sending intent '" + i.getAction() + "'");
					i.setPackage(context.getPackageName());
					i.putExtra(OppProbe.ReservedParamaters.REQUESTER.name, context.getPackageName());
					i.putExtra(OppProbe.ReservedParamaters.REQUESTS.name, requests);
					i.putExtra(OppProbe.ReservedParamaters.NONCE.name, nonce);
					context.sendBroadcast(i);
					ProbeCommunicator.this.context.unregisterReceiver(this);
				}
			}
		};
		context.registerReceiver(statusReceiver, new IntentFilter(OppProbe.getStatusAction(probeName)));
		requestStatusFrom(probeName, true);
	}
	
	public void registerDataRequest(Class<? extends Probe> probeClass, Bundle... requests) {
		registerDataRequest(probeClass.getName(), requests);
	}
	
	public void unregisterDataRequest(String probeName) {
		registerDataRequest(probeName); // Blank data request
	}
	
	public void unregisterDataRequest(Class<? extends Probe> probeClass) {
		unregisterDataRequest(probeClass.getName());
	}
}
