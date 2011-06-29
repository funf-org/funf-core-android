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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Convenience class for binding to a probe service performing an action and immediately unbinding.
 * @author alangardner
 *
 */
public abstract class ProbeCommandServiceConnection implements ServiceConnection {
	private static final String TAG = ProbeCommandServiceConnection.class.getName();
	private Context context;
	private Probe probe;
	private boolean hasRun;
	
	
	public ProbeCommandServiceConnection(Context context, Class<? extends Probe> probeClass) {
		this.context = context;
		Intent i = new Intent(context, probeClass);
		context.bindService(i, this, Context.BIND_AUTO_CREATE);
		hasRun = false;
	}
	
	public Probe getProbe() {
		return probe;
	}
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(TAG, "Binding: " + className);
        probe = ((Probe.LocalBinder)service).getService();
        if (probe != null) {
        	runCommand();
        	hasRun = true;
        }
        context.unbindService(this);
    }
	
	/**
	 * Delegate method to run needed commands on probe.  Use getProbe() to get access to probe.
	 * getProbe() is guaranteed to return a probe if runCommand is called.
	 */
	public abstract void runCommand();
	
	public void join() throws InterruptedException {
		join(5000);
	}
	
	public void join(long timeout) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while(startTime + timeout > System.currentTimeMillis()) {
			if (hasRun) {
				return;
			}
			Thread.sleep(100);
		}
	}
	
    public void onServiceDisconnected(ComponentName className) {
    	Log.i(TAG, "Unbinding: " + className);
    }
}
