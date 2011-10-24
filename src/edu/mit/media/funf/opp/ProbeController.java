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

import java.util.Set;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeCommandServiceConnection;
import edu.mit.media.funf.probe.ProbeUtils;

/**
 * Discovers probes that are defined in the app manifest, 
 * and dynamically creates broadcast receivers to listen for OPP requests.
 * 
 * TODO: eventually will communicate with funf controllers in other APKs
 * @author alangardner
 *
 */
public class ProbeController extends Service  {

	private final static String TAG = ProbeController.class.getName();
	
	private BroadcastReceiver probeMessageReceiver;
	
	@Override
	public void onCreate() {
		this.probeMessageReceiver = new ProbeMessageReceiver();
		// Discover probes and create listeners for probes in manifest
		// TODO: need to coordinate with other Funf apps to determine which probes this app is in charge of
		registerReceiver(probeMessageReceiver, new IntentFilter(OppProbe.getGlobalPollAction()));
		Log.i(TAG, "Registering for " + OppProbe.getGlobalPollAction());
		for (Class<? extends Probe> probeClass : getAvailableProbeClasses()) {
			new ProbeCommandServiceConnection(this, probeClass) {
				@Override
				public void runCommand() {
					// TODO: check that the required permissions and features exist for device and app
					// TODO: loop over probeInterfaces
					//for (String probeInterface : nonNullStrings(getProbe().getProbeInterfaces())) {
					Log.i(TAG, "Registering for " + OppProbe.getGetAction(getProbe().getClass()));
					Log.i(TAG, "Registering for " + OppProbe.getPollAction(getProbe().getClass()));
			        	registerReceiver(probeMessageReceiver, new IntentFilter(OppProbe.getGetAction(getProbe().getClass())));
			        	registerReceiver(probeMessageReceiver, new IntentFilter(OppProbe.getPollAction(getProbe().getClass())));
			        //}
				}
			};
			// TODO: register with permissions
		}
		// TODO: Discover other controllers
	}
	
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "Unregistering receiver");
		unregisterReceiver(probeMessageReceiver);
	}


	/**
	 * Class for receiving OPP requests
	 * @author alangardner
	 *
	 */
	private class ProbeMessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String requestingPackage = intent.getStringExtra(OppProbe.ReservedParamaters.PACKAGE.name);
			final String action = intent.getAction();
			Log.i(TAG, "Receiving action " + action);
			if (requestingPackage == null) {
				Log.i(TAG, "No requester package specified.  Ignoring.");
			} else if (OppProbe.isPollAction(action)) {
				for (Class<? extends Probe> probeClass : getAvailableProbeClasses()) {
					if (action.equals(OppProbe.getGlobalPollAction()) 
							|| probeClass.getName().equals(OppProbe.getProbeName(action))) {
						Intent statusIntent = new Intent(Probe.ACTION_SEND_STATUS, null, getApplicationContext(), probeClass);
						startService(statusIntent);
						// boolean includeNonce = intent.getBooleanExtra(OppProbe.ReservedParamaters.NONCE.name, false);
						// TODO: have opp communicator deal with nonces.
					}
				}
			} else if (OppProbe.isGetAction(action)) {
				Class<? extends Probe> probeClass = getProbeClass(action);
				// TODO: may need to coordinate schedule with master Funf controller
				// TODO: may need to run remotely in other Funf app
				
				// Local running case
				if (probeClass != null) {
					Intent probeServiceIntent = new Intent(context, probeClass);
					probeServiceIntent.putExtras(intent.getExtras());
					startService(probeServiceIntent);
				}
			}
		}
	}
	
	
	private Set<Class<? extends Probe>> getAvailableProbeClasses() {
		return ProbeUtils.getAvailableProbeClasses(this);
	}
	
	private Class<? extends Probe> getProbeClass(final String action) {
		return ProbeUtils.getProbeClass(getAvailableProbeClasses(), action);
	}
	


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Need to figure out the best way for IPC between ProbeControllers
		return null;
	}
}
