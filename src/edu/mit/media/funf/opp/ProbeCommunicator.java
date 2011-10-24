/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.opp;

import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.probe.Probe;

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
		i.putExtra(OppProbe.ReservedParamaters.PACKAGE.name, context.getPackageName());
		context.sendBroadcast(i);
	}
	
	public void requestStatus(boolean includeNonce) {
		final Intent i = new Intent(OppProbe.getPollAction(probeName));
		Log.i(TAG, "Sending intent '" + i.getAction() + "'");
		i.setPackage(context.getPackageName());
		i.putExtra(OppProbe.ReservedParamaters.PACKAGE.name, context.getPackageName());
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
		private boolean sent;
		
		public DataResponder(String requestId, Bundle... requests) {
			this.requestId = requestId;
			this.requests = requests;
			this.expirationTimer = new Timer();
			expirationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						context.unregisterReceiver(DataResponder.this);
						Log.e(TAG, "Probe '" + probeName + "' never responded with nonce.  Does the apk have the necessary permissions?");
					} catch (IllegalArgumentException e) {
						// already removed;
					}
				}
			}, 10000);
			sent = false;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.v(TAG, "Receiving: " + intent.getAction());
			if (intent.getAction().equals(OppProbe.getStatusAction(probeName))) {
				OppProbe.Status status = new OppProbe.Status(intent.getExtras());
				if (probeName.equals(status.getName())) {
					Log.i(TAG, "Is a status action for " + probeName);
					long nonce = intent.getLongExtra(OppProbe.ReservedParamaters.NONCE.name, 0L);
					Log.i(TAG, "Nonce is " + nonce + "'");
					if (!sent && nonce != 0L) {
						sent = true;
						expirationTimer.cancel();
						try {
							context.unregisterReceiver(DataResponder.this);
						} catch (IllegalArgumentException e) {
							// already removed;
						}
						final Intent i = new Intent(OppProbe.getGetAction(probeName));
						Log.i(TAG, "Sending intent '" + i.getAction() + "'");
						i.setPackage(context.getPackageName()); // Send only to this app for right now
						i.putExtra(OppProbe.ReservedParamaters.PACKAGE.name, context.getPackageName());
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
		
	}
}
