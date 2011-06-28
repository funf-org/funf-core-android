package edu.mit.media.hd.funf.client;

import java.util.Timer;
import java.util.TimerTask;

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
	private final String probeName;
	
	public ProbeCommunicator(Context context, String probeName) {
		this.context = context.getApplicationContext();
		this.probeName = probeName;
	}
	public ProbeCommunicator(Context context, Class<? extends Probe> probeClass) {
		this(context, probeClass.getName());
	}
	
	public static void requestStatusFromAll(Context context) {
		Intent i = new Intent(OppProbe.getGlobalPollAction());
		context.sendBroadcast(i);
	}
	
	public void requestStatus(boolean includeNonce) {
		final Intent i = new Intent(OppProbe.getPollAction(probeName));
		Log.i(TAG, "Sending intent '" + i.getAction() + "'");
		i.setPackage(context.getPackageName());
		i.putExtra(OppProbe.ReservedParamaters.REQUESTER.name, context.getPackageName());
		i.putExtra(OppProbe.ReservedParamaters.NONCE.name, includeNonce);
		context.sendBroadcast(i);
	}
	
	public void requestStatus() {
		requestStatus(false);
	}
	
	public void registerDataRequest(final Bundle... requests) {
		registerDataRequest("", requests);
	}
	
	public void registerDataRequest(final String requestId, final Bundle... requests) {
		DataResponder statusReceiver = new DataResponder(requestId, requests);
		context.registerReceiver(statusReceiver, new IntentFilter(OppProbe.getStatusAction(probeName)));
		requestStatus(true);
	}
	
	public void unregisterDataRequest(String requestId) {
		registerDataRequest(requestId); // Blank data request
	}
	
	public void unregisterDataRequest() {
		unregisterDataRequest("");
	}
	
	/**
	 * Helper class to respond with data request once we get a nonce from a status request
	 *
	 */
	private class DataResponder extends BroadcastReceiver {

		private final String requestId;
		private final Bundle[] requests;
		private final Timer expirationTimer;
		
		public DataResponder(String requestId, Bundle... requests) {
			this.requestId = requestId;
			this.requests = requests;
			this.expirationTimer = new Timer();
			expirationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					context.unregisterReceiver(DataResponder.this);
				}
			}, 1000);
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			long nonce = intent.getLongExtra(OppProbe.ReservedParamaters.NONCE.name, 0L);
			if (nonce != 0L) {
				expirationTimer.cancel();
				ProbeCommunicator.this.context.unregisterReceiver(this);
				final Intent i = new Intent(OppProbe.getGetAction(probeName));
				Log.i(TAG, "Sending intent '" + i.getAction() + "'");
				i.setPackage(context.getPackageName());
				i.putExtra(OppProbe.ReservedParamaters.REQUESTER.name, context.getPackageName());
				if (requestId != null && !"".equals(requestId)) {
					i.putExtra(OppProbe.ReservedParamaters.REQUEST_ID.name, requestId);
				}
				i.putExtra(OppProbe.ReservedParamaters.REQUESTS.name, requests);
				i.putExtra(OppProbe.ReservedParamaters.NONCE.name, nonce);
				context.sendBroadcast(i);
			}
		}
		
	}
}
