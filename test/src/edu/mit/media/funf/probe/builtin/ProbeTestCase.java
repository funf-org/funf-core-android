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
package edu.mit.media.funf.probe.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ServiceTestCase;
import android.util.Log;
import edu.mit.media.funf.OppProbe;
import edu.mit.media.funf.client.ProbeCommunicator;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeCommandServiceConnection;
import edu.mit.media.funf.probe.ProbeController;
import edu.mit.media.funf.probe.ProbeRequests;
import edu.mit.media.funf.probe.ProbeUtils;

public abstract class  ProbeTestCase<T extends Probe> extends ServiceTestCase<T> {

	public static final String TAG = "FunfTest";
	
	private static final String TEST_ID = "test.id";
	
	private final Class<T> probeClass;
	private final DataReceiver receiver;
	private BlockingQueue<Bundle> dataBundles;
	private Timer timer;
	
	public ProbeTestCase(Class<T> probeClass) {
		super(probeClass);
		this.probeClass = probeClass;
		receiver = new DataReceiver();
		probeControllerStarted = false;
	}

	private void clean() throws InterruptedException {
		// Remove all current state
		List<ProbeCommandServiceConnection> connections = new ArrayList<ProbeCommandServiceConnection>();
		for (Class<? extends Probe> probeClass : ProbeUtils.getAvailableProbeClasses(getContext())) {
			ProbeRequests requests = ProbeRequests.getRequestsForProbe(getContext(), probeClass.getName());
			requests.getSharedPreferences().edit().clear().commit();
			ProbeCommandServiceConnection probeConn = new ProbeCommandServiceConnection(getContext(), probeClass) {
				@Override
				public void runCommand() {
					getProbe().reset();
				}
			};
			connections.add(probeConn);
		}
		for(ProbeCommandServiceConnection conn : connections) {
			conn.join();
		}
	}
	
	@Override
	protected  void setUp() throws Exception {
		super.setUp();
		clean();
		probeControllerStarted = false;
		timer = new Timer();
		dataBundles = new LinkedBlockingQueue<Bundle>();
		getContext().registerReceiver(receiver, new IntentFilter(OppProbe.getDataAction(probeClass)));
	}
	
	@Override
	protected void tearDown() throws Exception {
		getContext().unregisterReceiver(receiver);
		timer.cancel();
		clean();
		super.tearDown();
	}
	
	protected Bundle getData(int timeoutSeconds) {
		Bundle data = null;
		try {
			data = dataBundles.poll(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("Was interrupted while waiting for data");
		}
		if (data == null) {
			fail("Did not return data within max wait time.");
		}
		return data;
	}
	
	protected Timer getTimer() {
		return timer;
	}
	
	protected String getTestRequester() {
		return getContext().getPackageName();
	}
	protected String getTestRequestId() {
		return TEST_ID;
	}
	
	private boolean probeControllerStarted;
	protected void sendDataRequestBroadcast(final Class<? extends Probe> aProbeClass, final Bundle... params) {
		long timeToWait = 0;
		if (!probeControllerStarted) {
			timeToWait = 3000;
			// Start probe controller to listen to broadcasts
			getContext().startService(new Intent(getContext(), ProbeController.class));
		}
		// Wait for probe controller to startup
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				probeControllerStarted = true;
				ProbeCommunicator probe = new ProbeCommunicator(getContext(), aProbeClass);
				probe.registerDataRequest(TEST_ID, params);
			}
		}, timeToWait);
	}
	
	protected void sendDataRequestBroadcast(final Bundle... params) {
		sendDataRequestBroadcast(probeClass, params);
	}
	
	protected void startProbe(final Bundle... params) {
		Intent i = new Intent(getContext(), probeClass);
		i.putExtra(OppProbe.ReservedParamaters.PACKAGE.name, getTestRequester());
		i.putExtra(OppProbe.ReservedParamaters.REQUEST_ID.name, getTestRequestId());
		i.putExtra(OppProbe.ReservedParamaters.REQUESTS.name, params);
		getContext().startService(i);
	}
	
	protected void stopProbe() {
		new ProbeCommandServiceConnection(getContext(), probeClass) {
			@Override
			public void runCommand() {
				getProbe().stop();
			}
		};
	}
	
	protected void sendStatusRequest() {
		// TODO: implement
	}
	
	
	public class DataReceiver extends BroadcastReceiver {

		private String probeDataAction = OppProbe.getDataAction(probeClass);
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "Recieved: " + intent.getAction());
			if (probeDataAction.equals(intent.getAction())) {
				Log.i(TAG, "Adding data:" + intent.getExtras());
				dataBundles.offer(intent.getExtras());
			}
		}
		
	}
}
