/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe;

import java.util.Set;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

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
		registerReceiver(probeMessageReceiver, new IntentFilter(Utils.getStatusRequestAction()));
		for (Class<? extends Probe> probeClass : getAvailableProbeClasses()) {
			new ProbeCommandServiceConnection(this, probeClass) {
				@Override
				public void runCommand() {
					// TODO: check that the required permissions and features exist for device and app
					// TODO: loop over probeInterfaces
					//for (String probeInterface : nonNullStrings(getProbe().getProbeInterfaces())) {
			        	registerReceiver(probeMessageReceiver, new IntentFilter(Utils.getDataRequestAction(getProbe().getClass())));
			        	registerReceiver(probeMessageReceiver, new IntentFilter(Utils.getStatusRequestAction(getProbe().getClass())));
			        //}
				}
			};
			// TODO: register with permissions
		}
		// TODO: Discover other controllers
	}
	
	
	@Override
	public void onDestroy() {
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
			final String action = intent.getAction();
			if (Utils.isStatusRequest(action)) {
				for (Class<? extends Probe> probeClass : getAvailableProbeClasses()) {
					new ProbeCommandServiceConnection(ProbeController.this, probeClass) {
						@Override
						public void runCommand() {
							getProbe().sendProbeStatus();
						}
					};
				}
			} else if (Utils.isDataRequest(action)) {
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
		return Utils.getAvailableProbeClasses(this);
	}
	
	private Class<? extends Probe> getProbeClass(final String action) {
		return Utils.getProbeClass(getAvailableProbeClasses(), action);
	}
	


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Need to figure out the best way for IPC between ProbeControllers
		return null;
	}
}
