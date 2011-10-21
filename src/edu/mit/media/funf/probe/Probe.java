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

import static edu.mit.media.funf.AsyncSharedPrefs.async;
import static edu.mit.media.funf.Utils.nonNullStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.funf.CustomizedIntentService;
import edu.mit.media.funf.OppProbe;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.ProbeExceptions.UnstorableTypeException;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;

public abstract class Probe extends CustomizedIntentService {


	protected final String TAG = getClass().getName();
	
	public static final String
	CALLBACK_KEY = "CALLBACK",
	ACTION_SEND_DETAILS = "PROBE_SEND_DETAILS",
	ACTION_SEND_CONFIGURATION = "PROBE_SEND_CONFIGURATION",
	ACTION_SEND_STATUS = "PROBE_SEND_STATUS",
	ACTION_REQUEST = "PROBE_REGISTER";
	
	private static final String 
	MOST_RECENT_RUN_KEY = "mostRecentTimeRun",
	MOST_RECENT_KEY = "mostRecentTimeDataSent",
	MOST_RECENT_PARAMS_KEY = "mostRecentParamsSent",
	NEXT_RUN_TIME_KEY = "nextRunTime",
	
	ACTION_INTERNAL_RUN = "PROBE_INTERNAL_RUN",
	ACTION_INTERNAL_STOP = "PROBE_INTERNAL_STOP",
	ACTION_INTERNAL_DISABLE = "PROBE_INTERNAL_DISABLE",
	
	INTERNAL_REQUESTS_KEY = "PROBE_INTERNAL_REQUESTS";
	
	private PowerManager.WakeLock lock;
	private Intent runIntent;
	private boolean enabled;
	private boolean running;
	private SharedPreferences historyPrefs;
	private Queue<Intent> pendingRequests;
	
	@Override
	public final void onCreate() {
		Log.v(TAG, "CREATED");
		super.onCreate();
        historyPrefs = async(getSharedPreferences("PROBE_" + getClass().getName(), MODE_PRIVATE));
		enabled = false;
		running = false;
		pendingRequests = new ConcurrentLinkedQueue<Intent>();
	}
	
	@Override
	public final void onDestroy() {
		Log.v(TAG, "DESTROYED");
		// Ensure disable happens on message thread
		queueIntent(new Intent(ACTION_INTERNAL_DISABLE, null, this, getClass()));
		super.onDestroy();
	}
	
	
    protected void onHandleIntent(Intent intent) {String action = intent.getAction();
		if (ACTION_INTERNAL_RUN.equals(action) || ACTION_INTERNAL_STOP.equals(action)) {
			if (runIntent == null) {
				runIntent = intent;
			}
			updateRequests();
			ProbeScheduler scheduler = getScheduler();
			ArrayList<Intent> requests = runIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
			if (scheduler.shouldBeEnabled(this, requests)) {
				if(ACTION_INTERNAL_RUN.equals(action)) {
					_run();
				} else {
					_stop();
				}
			} else {
				_disable();
			}
			Long nextScheduledTime = scheduler.scheduleNextRun(this, requests);
			// TODO: set next run time
		} else if (ACTION_REQUEST.equals(action) || action == null) {
			boolean succesfullyQueued = queueRequest(intent);
			if (succesfullyQueued) {
				run();
			}
		} else if(ACTION_SEND_DETAILS.equals(action)) {
			// TODO:
		} else if(ACTION_SEND_CONFIGURATION.equals(action)) {
			// TODO:
		} else if(ACTION_SEND_STATUS.equals(action)) {
			// TODO:
		} 
    }
    
	
	/**
	 * Updates request list with items in queue, replacing duplicate pending intents for this probe.
	 * @param requests
	 */
	private void updateRequests() {
		assert runIntent != null;
		ArrayList<Intent> requests = runIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
		if (requests == null) {
			requests = new ArrayList<Intent>();
		}
		Map<PendingIntent,Intent> existingCallbacksToRequests = new HashMap<PendingIntent,Intent>();
		for (Intent existingRequest : requests) {
			PendingIntent callback = existingRequest.getParcelableExtra(CALLBACK_KEY);
			existingCallbacksToRequests.put(callback, existingRequest);
		}
		for (Intent request = pendingRequests.poll(); request != null; request = pendingRequests.poll()) {
			PendingIntent callback = request.getParcelableExtra(CALLBACK_KEY);
			if (packageHasRequiredPermissions(this, callback.getTargetPackage(), getRequiredPermissions())) {
				existingCallbacksToRequests.containsKey(callback);
				int existingRequestIndex = requests.indexOf(existingCallbacksToRequests.get(callback));
				if (existingRequestIndex >= 0) {
					requests.set(existingRequestIndex, request);
				} else {
					requests.add(request);
				}
			} else {
				Log.w(TAG, "Package '" + callback.getTargetPackage() + "' does not have the required permissions to get data from this probe.");
			}
		}
		runIntent.putExtra(INTERNAL_REQUESTS_KEY, requests);
		PendingIntent.getService(this, 0, runIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	/**
	 * Returns true if request was successfully queued
	 * @param request
	 * @return
	 */
	private boolean queueRequest(Intent request) {
		// Check for pending intent
		PendingIntent callback = null;
		try {
			callback = request.getParcelableExtra(CALLBACK_KEY);
		} catch (Exception e) {
			Log.e(TAG, "Request sent invalid callback.");
		}

		if (callback == null) {
			Log.e(TAG, "Request did not send callback.");
		} else {
			boolean succesfullyQueued = pendingRequests.offer(request);
			if (succesfullyQueued) {
				Log.i(TAG, "Queued request from package '" + callback.getTargetPackage() + "'");
				return true;
			} else {
				Log.e(TAG, "Unable to queue request from package '" + callback.getTargetPackage() + "'");
			}
		}
		return false;
	}
	
	private void setHistory(Long previousDataSentTime, Long previousRunTime, Bundle previousRunParams, Long nextRunTime) {
		SharedPreferences.Editor editor = historyPrefs.edit();
		editor.clear();
		if (previousDataSentTime != null) editor.putLong(MOST_RECENT_KEY, previousDataSentTime);
		if (previousRunTime != null) editor.putLong(MOST_RECENT_RUN_KEY, previousRunTime);
		if (nextRunTime != null) editor.putLong(NEXT_RUN_TIME_KEY, nextRunTime);
		try {
			if (previousRunParams != null) Utils.putInPrefs(editor, MOST_RECENT_PARAMS_KEY, previousRunParams);
		} catch (UnstorableTypeException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		editor.commit();
	}
	
	/**
	 * Resets all of the run data information, and removes all requests
	 */
	public void reset() {
		setHistory(null, null, null, null);
		runIntent.removeExtra(INTERNAL_REQUESTS_KEY);
		Intent internalRunIntent = new Intent(ACTION_INTERNAL_RUN, null, this, getClass());
		PendingIntent selfLaunchingIntent = PendingIntent.getService(this, 0, internalRunIntent, PendingIntent.FLAG_NO_CREATE);
		if (selfLaunchingIntent != null) {
			selfLaunchingIntent.cancel();
		}
	}
	
	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe sent data
	 */
	public long getPreviousDataSentTime() {
		return historyPrefs.getLong(MOST_RECENT_KEY, 0L);
	}

	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe was run 
	 */
	public long getPreviousRunTime() {
		return historyPrefs.getLong(MOST_RECENT_RUN_KEY, 0L);
	}
	
	/**
	 * @return the bundle of params used to run the probe the most recent time it was run
	 */
	public Bundle getPreviousRunParams() {
		return Utils.getBundleFromPrefs(historyPrefs, MOST_RECENT_PARAMS_KEY);
	}
	
	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe was run 
	 */
	public long getNextRunTime() {
		return historyPrefs.getLong(NEXT_RUN_TIME_KEY, 0L);
	}
	
	/**
	 * Return the required set of permissions needed to run this probe
	 * @return
	 */
	public abstract String[] getRequiredPermissions();
	
	/**
	 * Return the required set of features needed to run this probe
	 * @return
	 */
	public abstract String[] getRequiredFeatures();
	
	/**
	 * @return Bundle of key value pairs that represent default parameters
	 */
	public abstract Parameter[] getAvailableParameters();


    /**
     * Safe method to run probe, which ensures that requests are preserved.
     */
    protected void run() {
    	// If we have not set runIntent yet, attempt to grab pending intent and send
		// Otherwise initialize runIntent and create corresponding PendingIntent
    	boolean internalRunSent = false;
    	if (runIntent == null) {
			Intent internalRunIntent = new Intent(ACTION_INTERNAL_RUN, null, this, getClass());
			PendingIntent selfLaunchingIntent = PendingIntent.getService(this, 0, internalRunIntent, PendingIntent.FLAG_NO_CREATE);
			if (selfLaunchingIntent == null) {
				runIntent = internalRunIntent;
			} else {
				try {
					selfLaunchingIntent.send();
					internalRunSent = true;
				} catch (CanceledException e) {
					runIntent = internalRunIntent;
				}
			}
		}
    	if (runIntent != null) {
			updateRequests(); // Creates pending intent if it doesn't exist
			if (!internalRunSent) {
				startService(runIntent);
			}
		}
    }
	
	
	protected final void stop() {
		startService(new Intent(ACTION_INTERNAL_STOP, null, this, getClass()));
	}
	
	/**
	 * Enables probe.  Can only be called from intent handler thread
	 */
	private void _enable() {
		assert isIntentHandlerThread();
		if (!enabled) {
			Log.i(TAG, "Enabling probe: " + getClass().getName());
			enabled = true;
			running = false;
			sendProbeStatus();
			onEnable();
		}
	}
	
	/**
	 * Runs probe.  Can only be called from intent handler thread
	 */
	private void _run() {
		assert isIntentHandlerThread();
		if (!enabled) {
			_enable();
		}
		ProbeScheduler scheduler = getScheduler();
		ArrayList<Intent> requests = runIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
		Bundle parameters = scheduler.startRunningNow(this, requests);
		if (parameters != null) {
			running = true;
			if (lock == null) {
				lock = Utils.getWakeLock(this);
			}
			sendProbeStatus();
			// TODO: set mostRecentRun time
			onRun(parameters);
		}
		// Schedule before stop to ensure schedule exists if crash happens, or if not run
		Long nextScheduledTime = scheduler.scheduleNextRun(this, requests);
		// TODO: set next run time
	}
	
	/**
	 * Stops probe.  Can only be called from intent handler thread
	 */
	private void _stop() {
		assert isIntentHandlerThread();
		if (enabled && running) {
			Log.i(TAG, "Stopping probe: " + getClass().getName());
			onStop();
			running = false;  // TODO: possibly this should go before onStop, to keep with convention
			sendProbeStatus();
			if (lock != null && lock.isHeld()) {
				lock.release();
				lock = null;
			}
		}
	}
	
	/**
	 * Disables probe.  Can only be called from intent handler thread
	 */
	private void _disable() {
		assert isIntentHandlerThread();
		if (enabled) {
			Log.i(TAG, "Disabling probe: " + getClass().getName());
			if (running) {
				stop();
			}
			enabled = false;
			sendProbeStatus();
			onDisable();
		}
	}
	
	/**
	 * @return true if actively running, otherwise false
	 */
	public boolean isRunning() {
		return enabled ? running : false;
	}
	
	@Override
	protected boolean shouldStop() {
		return enabled == false;
	}

	/**
	 * Start actively running the probe.  Send data broadcast when done (or when appropriate) and stop.
	 */
	protected abstract void onEnable();
	
	/**
	 * Start actively running the probe.  Send data broadcast when done (or when appropriate) and stop.
	 * @param params
	 */
	protected abstract void onRun(Bundle params);
	
	/**
	 * Stop actively running the probe.  Any passive listeners should continue running.
	 */
	protected abstract void onStop();
	
	/**
	 * Disable any passive listeners and tear down the service.  Will be called when probe service is destroyed.
	 */
	protected abstract void onDisable();
	

	
	/* TODO: may be a good way to make sure your configuration is set correctly
	public void sendProbeConfiguration() {
		
	}
	*/

	public void sendProbeDetails() {
		// TODO: create details object for probe and send to each pending intent
	}

	
	public void sendProbeStatus() {
		// TODO: create status object for probe and send to each pending intent
	}
	
	/**
	 * Sends a DATA broadcast of the current data from the probe, if it is available.
	 */
	public abstract void sendProbeData();
	// TODO: not sure we want this anymore
	// This requires probes keep caches of last data sent.
	// May not be useful unless we have a "send last data" feature.
	
	
	/**
	 * Sends a DATA broadcast for the probe, and records the time.
	 */
	protected void sendProbeData(long epochTimestamp, Bundle data) {
		Log.d(TAG, "Sent probe data at " + epochTimestamp);
		// TODO: send probe data to each pending request
		
		/*  OLD implementation
		mostRecentTimeDataSent = System.currentTimeMillis();
		Intent dataBroadcast = new Intent(OppProbe.getDataAction(getClass()));
		dataBroadcast.putExtra(BaseProbeKeys.TIMESTAMP, epochTimestamp);
		// TODO: should we send parameters with data broadcast?
		dataBroadcast.putExtras(data);
		Set<String> requestingPackages = allRequests.getByRequesterByRequestId().keySet();
		Log.i(TAG, "Sending probe data to: " + Utils.join(requestingPackages, ", "));
		for (String requestingPackage : requestingPackages) {
			Intent scopedDataBroadcast = new Intent(dataBroadcast);
			scopedDataBroadcast.setPackage(requestingPackage);
			sendBroadcast(dataBroadcast);
		}*/
	}
	
	

	protected String getDisplayName() {
		String className = getClass().getName().replace(getClass().getPackage().getName() + ".", "");
		return className.replaceAll("(\\p{Ll})(\\p{Lu})","$1 $2"); // Insert spaces
	}
	
	private static List<PackageInfo> apps;
	private static long appsLastLoadTime = 0L;
	private static final long APPS_CACHE_TIME = Utils.secondsToMillis(300); // 5 minutes
	private static PackageInfo getPackageInfo(Context context, String packageName) {
		long now = System.currentTimeMillis();
		if (apps == null || now > (appsLastLoadTime + APPS_CACHE_TIME)) {
			apps = context.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
			appsLastLoadTime = now;
		}
		for (PackageInfo info : apps) {
			if (info.packageName.equals(packageName)) {
				return info;
			}
		}
		return null;
	}
	
	private static boolean packageHasRequiredPermissions(Context context, String packageName, String[] requiredPermissions) {
		Log.v(Utils.TAG, "Getting package info for '" + packageName + "'");
		PackageInfo info = getPackageInfo(context, packageName);
		if (info == null) {
			Log.w(Utils.TAG, "Package '" + packageName + "' is not installed.");
			return false;
		}
		
		Set<String> packagePermissions = new HashSet<String>(Arrays.asList(info.requestedPermissions));
		Log.v(Utils.TAG, "Package permissions for '" + packageName + "': " + Utils.join(packagePermissions, ", "));
		for (String permission :  nonNullStrings(requiredPermissions)) {
			if (!packagePermissions.contains(permission)) {
				Log.w(Utils.TAG, "Package '" + packageName + "' does not have the required permission '" + permission + "' to run this probe.");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the scheduler object used to schedule this probe.
	 * Probes should override this to change how their probe gets scheduled.
	 * @return
	 */
	protected ProbeScheduler getScheduler() {
		return new DefaultProbeScheduler();
	}
	
	

	/**
	 * Represents a parameter that can be passed to a probe
	 * @author alangardner
	 *
	 */
	public final static class Parameter extends OppProbe.Parameter {
		
		private boolean supportedByProbe = true;
		
		public Parameter(final String name, final Object defaultValue, final String displayName, final String description) {
			super(name, defaultValue, displayName, description);
		}
		
		/**
		 * System parameter constructor, to be handled by system
		 * @param paramType
		 * @param defaultValue
		 */
		public Parameter(final SystemParameter paramType, final Object defaultValue) {
			this(paramType, defaultValue, false);
		}
		
		/**
		 * System parameter constructor, with option to specify that probe will handle system parameter
		 * instead of system.
		 * @param paramType
		 * @param defaultValue
		 * @param supportedByProbe
		 */
		public Parameter(final SystemParameter paramType, final Object defaultValue, final boolean supportedByProbe) {
			super(paramType.name, defaultValue, paramType.displayName, paramType.description);
			this.supportedByProbe = supportedByProbe;
		}

		public boolean isSupportedByProbe() {
			return supportedByProbe;
		}
		
	}
	
	/**
	 * The built-in parameters that the Funf system knows how to handle
	 * @author alangardner
	 *
	 */
	public enum SystemParameter {
		PASSIVE("PASSIVE", "Passive", "Whether the requester wants data they did not specifically request."),
		DURATION("DURATION", "Duration", "Length of time probe will run for (seconds)"),
		START("START_DATE", "Start Timestamp", "Date after which probe is allowed to run (seconds since epoch)"),
		END("END_DATE", "End Timestamp", "Date before which probe is allowed to run (seconds since epoch)"),
		PERIOD("PERIOD", "Period", "Length of time between probe runs (seconds)");
		
		public final String name, displayName, description;
		
		private  SystemParameter(String name, String displayName, String description) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
		}
	}

}
