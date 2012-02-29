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
	private Thread thread;
	
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
		Log.v(TAG, "Binding: " + className);
        probe = ((Probe.LocalBinder)service).getService();
        if (probe != null) {
        	thread = new Thread(new Runnable() {
				@Override
				public void run() {
		        	runCommand();
		        	hasRun = true;
		            context.unbindService(ProbeCommandServiceConnection.this);
				}
			});
        	thread.start();
        }
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
    	Log.v(TAG, "Unbinding: " + className);
    }
}
