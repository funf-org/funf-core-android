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
package edu.mit.media.funf.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ServiceTestCase;
import android.util.Log;

public abstract class  ProbeTestCase<T extends Probe> extends ServiceTestCase<T> {

	public static final String TAG = "FunfTest";
	
	private final Class<T> probeClass;
	private BlockingQueue<Bundle> dataBundles;
	private DataReceiver dataReceiver;
	private Timer timer;
	
	public ProbeTestCase(Class<T> probeClass) {
		super(probeClass);
		this.probeClass = probeClass;
	}

	private void clean() throws InterruptedException {
		// Cancel callback
		PendingIntent callback = PendingIntent.getBroadcast(getContext(), 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
		clearData();
		// Remove all current state
		List<ProbeCommandServiceConnection> connections = new ArrayList<ProbeCommandServiceConnection>();
		for (Class<? extends Probe> probeClass : getProbesAffected()) {
			ProbeCommandServiceConnection probeConn = new ProbeCommandServiceConnection(getContext(), probeClass) {
				@Override
				public void runCommand() {
					getProbe().reset();
					getProbe().stopSelf();
				}
			};
			connections.add(probeConn);
		}
		for(ProbeCommandServiceConnection conn : connections) {
			conn.join();
		}
	}
	
	protected List<Class<? extends Probe>> getProbesAffected() {
		List<Class<? extends Probe>> list = new ArrayList<Class<? extends Probe>>();
		list.add(probeClass);
		return list;
	}
	
	@Override
	protected  void setUp() throws Exception {
		super.setUp();
		clean();
		timer = new Timer();
		dataBundles = new LinkedBlockingQueue<Bundle>();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Probe.ACTION_DETAILS);
		filter.addAction(Probe.ACTION_STATUS);
		filter.addAction(Probe.ACTION_DATA);
		dataReceiver = new DataReceiver();
		getContext().registerReceiver(dataReceiver, filter);
	}
	
	@Override
	protected void tearDown() throws Exception {
		timer.cancel();
		getContext().unregisterReceiver(dataReceiver);
		clean();
		super.tearDown();
	}
	
	protected void shouldNotReturnData(int timeoutSeconds) {
		Bundle data = null;
		try {
			data = dataBundles.poll(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		if (data != null) {
			fail("Returned data within max wait time.");
		}
	}
	
	protected void clearData() {
		if (dataBundles != null) {
			dataBundles.clear();
		}
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
	
	
	protected void startProbe(final Bundle... params) {
		startProbe(probeClass, params);
	}
	protected void startProbe(Class<? extends Probe> probeClass, final Bundle... params) {
		// TODO: figure out how to reliably get data
		// Maybe creating service for saving data to a static variable
		Intent i = new Intent(getContext(), probeClass);
		i.setAction(Probe.ACTION_REQUEST);
		PendingIntent callback = PendingIntent.getBroadcast(getContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		i.putExtra(Probe.CALLBACK_KEY, callback);
		i.putExtra(Probe.REQUESTS_KEY, params);
		getContext().startService(i);
	}
	
	protected void stopProbe() {
		Intent i = new Intent(getContext(), probeClass);
		i.setAction(Probe.ACTION_STOP);
		getContext().startService(i);
	}
	
	protected void sendStatusRequest() {
		// TODO: implement
	}
	
	
	public class DataReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "Recieved: " + intent.getAction());
			if (Probe.ACTION_DATA.equals(intent.getAction()) 
					&& probeClass.getName().equals(intent.getStringExtra(Probe.PROBE))) {
				Log.i(TAG, "Adding data:" + intent.getExtras());
				dataBundles.offer(intent.getExtras());
			}
		}
		
	}
}
