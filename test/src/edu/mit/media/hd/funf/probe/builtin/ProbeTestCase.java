/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ServiceTestCase;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.client.ProbeCommunicator;
import edu.mit.media.hd.funf.probe.Probe;
import edu.mit.media.hd.funf.probe.ProbeCommandServiceConnection;
import edu.mit.media.hd.funf.probe.ProbeController;
import edu.mit.media.hd.funf.probe.ProbeRequests;
import edu.mit.media.hd.funf.probe.ProbeUtils;

public abstract class  ProbeTestCase<T extends Probe> extends ServiceTestCase<T> {

	private static final String TEST_REQUESTER = "test.requester";
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
		dataBundles = new ArrayBlockingQueue<Bundle>(5000);
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
		return TEST_REQUESTER;
	}
	protected String getTestRequestId() {
		return TEST_ID;
	}
	
	private boolean probeControllerStarted;
	protected void sendDataRequestBroadcast(final Bundle... params) {
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
				ProbeCommunicator probe = new ProbeCommunicator(getContext(), probeClass);
				probe.registerDataRequest(TEST_ID, params);
			}
		}, timeToWait);
	}
	
	protected void startProbe(final Bundle... params) {
		Intent i = new Intent(getContext(), probeClass);
		i.putExtra(OppProbe.ReservedParamaters.REQUESTER.name, getTestRequester());
		i.putExtra(OppProbe.ReservedParamaters.REQUEST_ID.name, getTestRequestId());
		i.putExtra(OppProbe.ReservedParamaters.REQUESTS.name, params);
		startService(i);
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
			if (probeDataAction.equals(intent.getAction())) {
				dataBundles.add(intent.getExtras());
			}
		}
		
	}
}
