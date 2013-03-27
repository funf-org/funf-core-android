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

import static edu.mit.media.funf.Utils.nonNullStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.funf.CustomizedIntentService;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe.Parameter.Builtin;
import edu.mit.media.funf.probe.ProbeExceptions.UnstorableTypeException;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;

public abstract class Probe extends CustomizedIntentService implements BaseProbeKeys {


	protected final String TAG = getClass().getName();
	
	private static final String PREFIX = Probe.class.getName();
	
	public static final String
	CALLBACK_KEY = "CALLBACK",
	REQUESTS_KEY = "REQUESTS",
	ACTION_SEND_DETAILS = PREFIX + ".SEND_DETAILS",
	ACTION_SEND_CONFIGURATION = PREFIX + ".SEND_CONFIGURATION",
	ACTION_SEND_STATUS = PREFIX + ".SEND_STATUS",
	ACTION_REQUEST = PREFIX + ".REQUEST",
	ACTION_DATA = PREFIX + ".DATA",
	ACTION_DETAILS = PREFIX + ".DETAILS",
	ACTION_STATUS = PREFIX + ".STATUS";
	
	private static final String 
	MOST_RECENT_RUN_KEY = "mostRecentTimeRun",
	MOST_RECENT_DATA_KEY = "mostRecentTimeDataSent",
	MOST_RECENT_DATA_BY_REQUEST_KEY = "mostRecentTimeDataSentByRequest",
	MOST_RECENT_PARAMS_KEY = "mostRecentParamsSent",
	NEXT_RUN_TIME_KEY = "nextRunTime";
	
	static final String
	ACTION_INTERNAL_REQUESTS = "PROBE_ACTION_INTERNAL_REQUESTS_INTENT",
	ACTION_INTERNAL_CALLBACK_REGISTERED = "PROBE_INTERNAL_CALLBACK_REGISTERED",
	
	ACTION_RUN = "PROBE_INTERNAL_RUN",
	ACTION_STOP = "PROBE_INTERNAL_STOP",
	ACTION_DISABLE = "PROBE_INTERNAL_DISABLE",
	
	INTERNAL_CALLBACK_INTENT = "PROBE_INTERNAL_CALLBACK_INTENT",
	INTERNAL_REQUESTS_KEY = "PROBE_INTERNAL_REQUESTS",
	INTERNAL_PROBE_STATE = "PROBE_INTERNAL_STATE",
	PROBE_STATE_RUNNING = "RUNNING",
	PROBE_STATE_ENABLED = "ENABLED",
	PROBE_STATE_DISABLED = "DISABLED";
	
	private static final long FAR_IN_FUTURE_MILLIS = 365L * 24 * 60 * 60 * 1000; // One year
	
	private PowerManager.WakeLock lock;
	private Intent requestsIntent;
	private boolean enabled;
	private boolean running;
	private SharedPreferences historyPrefs;
	private Queue<Intent> pendingRequests;
	private Queue<Intent> deadRequests;
	
	@Override
	public final void onCreate() {
		Log.v(TAG, "CREATED");
		super.onCreate();
		enabled = false;
		running = false;
		pendingRequests = new ConcurrentLinkedQueue<Intent>();
		deadRequests = new ConcurrentLinkedQueue<Intent>();
	}
	
	@Override
	public final void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "DESTROYED");
	}
	
	@Override 
	public void onBeforeDestroy() {
		// Ensure disable happens on message thread
		disable();
	}
	
	private Intent getRequestsIntent() {
		Intent i = new Intent(ACTION_INTERNAL_REQUESTS, null, this, getClass());
		// TODO: implement security number so that we can verify this was our intent we created, and not an external attack
		return i;
	}
	
	private boolean isRequestsIntent(Intent intent) {
		// TODO: verify security number
		// TODO: maybe count brute force attacks, shut down if too many
		return ACTION_INTERNAL_REQUESTS.equals(intent.getAction());
	}
	
	private void loadRequestsIntent(Intent currentIntent) {
		if (isRequestsIntent(currentIntent)) {
			Log.d(TAG, "Is requests intent.");
			requestsIntent = currentIntent;
		} else {
		   	// Attempt to grab pending intent and send Otherwise initialize requestsIntent
			Intent internalRunIntent = getRequestsIntent();
			PendingIntent selfLaunchingIntent = PendingIntent.getService(this, 0, internalRunIntent, PendingIntent.FLAG_NO_CREATE);
			if (selfLaunchingIntent == null) {
				requestsIntent = internalRunIntent;
			} else {
				try {
					Log.i(TAG, "Sending requests pending intent and waiting");
					selfLaunchingIntent.send();
					queueIntent(currentIntent, true);
					pauseQueueUntilIntentReceived(internalRunIntent, null);
				} catch (CanceledException e) {
					Log.e(TAG, "CANCELLED INTERNAL RUN INTENT");
					requestsIntent = internalRunIntent;
				}
			}
		}
	}
	
	private SharedPreferences getHistoryPrefs() {
		if (historyPrefs == null) { // Load prefs off main thread
    		historyPrefs = getSharedPreferences("PROBE_" + getClass().getName(), MODE_PRIVATE);
    	}
		return historyPrefs;
	}
	
    protected void onHandleIntent(Intent intent) {
    	String action = intent.getAction();
    	Log.d(TAG, getDisplayName() + ": " + action);
    	Log.d(TAG, "Component: " + intent.getComponent() == null ? "<none>" : intent.getComponent().getClassName());
		
    	
    	getHistoryPrefs();
    	if (requestsIntent == null) {
    		loadRequestsIntent(intent);
    		if (requestsIntent == null) {
    			Log.d(TAG, "Did not successfully load requests Intent");
    			return;
    		}
    	}

    	
    	updateRequests();


		Log.d(TAG, "RunIntent " + (requestsIntent == null ? "<null>" : "exists"));
    	
    	if (intent.getComponent().getClassName().equals(Probe.class.getName())) { // Internally queued, not available outside of probe class
    		if (ACTION_RUN.equals(action) || ACTION_STOP.equals(action) || ACTION_DISABLE.equals(action)) {
    			ProbeScheduler scheduler = getScheduler();
    			ArrayList<Intent> requests = requestsIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
    			Log.d(TAG, "Requests:" + requests);
    			if (isAvailableOnDevice() && !ACTION_DISABLE.equals(action) && scheduler.shouldBeEnabled(this, requests)) {
    				if(ACTION_RUN.equals(action)) {
    					_run();
    				} else {
    					_stop();
    				}
    			} else {
    				_disable();
    				updateInternalRequestsPendingIntent() ;
    			}

    			Long nextScheduledTime = scheduler.scheduleNextRun(this, requests);
    			Log.d(TAG, "Next scheduled time: " + nextScheduledTime);
    			if (nextScheduledTime == null) {
    				getHistoryPrefs().edit().remove(NEXT_RUN_TIME_KEY).commit();
    			} else {
    				getHistoryPrefs().edit().putLong(NEXT_RUN_TIME_KEY, nextScheduledTime).commit();
    			}
    		} else if (ACTION_INTERNAL_CALLBACK_REGISTERED.equals(action)){
				Intent callbackRegisteredIntent = intent.getParcelableExtra(INTERNAL_CALLBACK_INTENT);
				PendingIntent callback = intent.getParcelableExtra(CALLBACK_KEY);
	    		_callback_registered(callbackRegisteredIntent, callback);
			}
    	} else if (ACTION_REQUEST.equals(action) || action == null) {
			ArrayList<Bundle> test = Utils.getArrayList(intent.getExtras(), REQUESTS_KEY);
			Log.d(TAG, "REQUEST: " + test);
			boolean succesfullyQueued = queueRequest(intent);
			if (succesfullyQueued) {
				run();
			}
		} else if (ACTION_INTERNAL_REQUESTS.equals(action)) {
			
		} else if (ACTION_RUN.equals(action)) { // External stop, queue up stop
			run();
		} else if (ACTION_STOP.equals(action)) { // External stop, queue up stop
			stop();
		} else if (ACTION_DISABLE.equals(action)) {   // External disable, queue up disable
			disable();
		} else if(ACTION_SEND_DETAILS.equals(action)) {  // DOESN'T NEED requests
			PendingIntent callback = intent.getParcelableExtra(CALLBACK_KEY);
			sendProbeDetails(callback);
		} else if(ACTION_SEND_CONFIGURATION.equals(action)) {
			//PendingIntent callback = intent.getParcelableExtra(CALLBACK_KEY);
			//sendProbeConfiguration(callback);
		} else if(ACTION_SEND_STATUS.equals(action)) { // DOESN'T NEED requests
			PendingIntent callback = intent.getParcelableExtra(CALLBACK_KEY);
			sendProbeStatus(callback);
		} else {
			onHandleCustomIntent(intent);
		}
    }
    
    /**
     * Probe subclasses can override this to receive custom intents that the base probe does not handle.
     * For instance, data intents from other probes.
     * @param intent
     */
    protected void onHandleCustomIntent(Intent intent) {
    	
    }
    
    private void updateRequests() {
    	updateRequests(false);
    }
	
    private static Map<PendingIntent,Intent> getCallbacksToRequests(ArrayList<Intent> requests) {
    	Map<PendingIntent,Intent> existingCallbacksToRequests = new HashMap<PendingIntent,Intent>();
		for (Intent existingRequest : requests) {
			PendingIntent callback = existingRequest.getParcelableExtra(CALLBACK_KEY);
			existingCallbacksToRequests.put(callback, existingRequest);
		}
		return existingCallbacksToRequests;
    }
    
	/**
	 * Updates request list with items in queue, replacing duplicate pending intents for this probe.
	 * @param requests
	 */
	private void updateRequests(boolean removeRunOnce) {
		assert requestsIntent != null;
		boolean hasChanges = false;
		ArrayList<Intent> requests = requestsIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
		if (requests == null) {
			hasChanges = true;
			requests = new ArrayList<Intent>();
		}
		
		// Remove run once requests
		Parameter periodParam = Parameter.getAvailableParameter(getAvailableParameters(), Parameter.Builtin.PERIOD);
		if (periodParam != null && removeRunOnce) {
			for (Intent request : requests) {
				ArrayList<Bundle> dataRequests = Utils.getArrayList(request.getExtras(), REQUESTS_KEY);
				List<Bundle> runOnceDataRequests = new ArrayList<Bundle>();
				for (Bundle dataRequest : dataRequests) {
					long periodValue = Utils.getLong(dataRequest, Parameter.Builtin.PERIOD.name, (Long)periodParam.getValue());
					if (periodValue == 0L) {
						Log.d(TAG, "Removing run once dataRequest: " + dataRequest);
						runOnceDataRequests.add(dataRequest);
					}
				}
				dataRequests.removeAll(runOnceDataRequests);
				if (dataRequests.isEmpty()) {
					deadRequests.add(request);
				} else {
					request.putExtra(REQUESTS_KEY, dataRequests);
				}
			}
		}
		
		// Remove all requests that we aren't able to (or supposed to) send to anymore
		if (!deadRequests.isEmpty()) {
			hasChanges = true;
			for (Intent deadRequest = deadRequests.poll(); deadRequest != null; deadRequest = deadRequests.poll()) {
				Log.d(TAG, "Removing dead request: " + deadRequest);
				requests.remove(deadRequest);
			}
		}
		// Add any pending requests
		if (!pendingRequests.isEmpty()) {
			hasChanges = true;
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
					ArrayList<Bundle> dataRequests = Utils.getArrayList(request.getExtras(), REQUESTS_KEY);
					Log.d(TAG, "Adding pending intent with data requests: " + dataRequests);
					if (existingRequestIndex >= 0) {
						if (dataRequests == null || dataRequests.isEmpty()) {
							Log.d(TAG, "Adding pending intent, removing because empty or null");
							requests.remove(existingRequestIndex);
						} else {
							requests.set(existingRequestIndex, request);
						}
					} else {
						if (dataRequests != null && !dataRequests.isEmpty()) { // Only add requests with nonempty data requests
							Log.d(TAG, "Adding new pending intent: " + request);
							requests.add(request);
						}
					}
				} else {
					Log.w(TAG, "Package '" + callback.getTargetPackage() + "' does not have the required permissions to get data from this probe.");
				}
			}
		}
		
		if (hasChanges) {
			requestsIntent.putExtra(INTERNAL_REQUESTS_KEY, requests);
			updateInternalRequestsPendingIntent();
		}
	}
	
	private void updateInternalRequestsPendingIntent() {
		PendingIntent internalPendingIntent = PendingIntent.getService(this, 0, requestsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Keep the pending intent valid, by having the alarm clock keep a reference to it
		AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		manager.set(AlarmManager.RTC, System.currentTimeMillis() + FAR_IN_FUTURE_MILLIS, internalPendingIntent);
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
		SharedPreferences.Editor editor = getHistoryPrefs().edit();
		editor.clear();
		if (previousDataSentTime != null) editor.putLong(MOST_RECENT_DATA_KEY, previousDataSentTime);
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
		getHistoryPrefs().edit().clear().commit();
		if (requestsIntent != null) {
			requestsIntent.removeExtra(INTERNAL_REQUESTS_KEY);
		}
		PendingIntent selfLaunchingIntent = PendingIntent.getService(this, 0, getRequestsIntent(), PendingIntent.FLAG_NO_CREATE);
		if (selfLaunchingIntent != null) {
			selfLaunchingIntent.cancel();
		}
	}
	
	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe sent data
	 */
	public long getPreviousDataSentTime() {
		return getHistoryPrefs().getLong(MOST_RECENT_DATA_KEY, 0L);
	}

	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe was run 
	 */
	public long getPreviousRunTime() {
		return getHistoryPrefs().getLong(MOST_RECENT_RUN_KEY, 0L);
	}
	
	/**
	 * @return the bundle of params used to run the probe the most recent time it was run
	 */
	public Bundle getPreviousRunParams() {
		return Utils.getBundleFromPrefs(getHistoryPrefs(), MOST_RECENT_PARAMS_KEY);
	}
	
	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe was run 
	 */
	public long getNextRunTime() {
		return getHistoryPrefs().getLong(NEXT_RUN_TIME_KEY, 0L);
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
	 * @return true if the necessary hardware or features are available on this device to run this probe.
	 */
	public boolean isAvailableOnDevice() {
		return true;
	}
	

    /**
     * Safe method to run probe, which ensures that requests are preserved.
     */
    protected void run() {
    	if (requestsIntent != null) { // Assume the app is already disabled if requestsIntent is null
    		Intent i = new Intent(ACTION_RUN, null, this, Probe.class);
			queueIntent(i);
		}
    }
    
	
	
	protected final void stop() {
		Log.d(TAG, "Stop queued");
		if (requestsIntent != null) { // Assume the app is already disabled if requestsIntent is null
			Intent i = new Intent(ACTION_STOP, null, this, Probe.class);
			queueIntent(i);
		}
	}
	
	protected final void disable() {
		Log.d(TAG, "Disable queued");
		if (requestsIntent != null) { // Assume the app is already disabled if requestsIntent is null
			Intent i = new Intent(ACTION_DISABLE, null, this, Probe.class);
			queueIntent(i);
		}
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
			sendProbeStatus(null);
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
		ArrayList<Intent> requests = requestsIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
		Bundle parameters = scheduler.startRunningNow(this, requests);
		if (parameters != null) {
			Log.i(TAG, "Running probe: " + getClass().getName());
			running = true;
			if (lock == null) {
				lock = Utils.getWakeLock(this);
			}
			sendProbeStatus(null);
			long nowTimestamp = Utils.millisToSeconds(System.currentTimeMillis());
			getHistoryPrefs().edit().putLong(MOST_RECENT_RUN_KEY, nowTimestamp).commit();
			onRun(parameters);
		}
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
			sendProbeStatus(null);
			// Remove one off requests
			updateRequests(true);
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
				_stop();
			}
			enabled = false;
			sendProbeStatus(null);
			onDisable();
		}
	}
	
	/**
	 * @return true if actively running, otherwise false
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * @return true if actively running, otherwise false
	 */
	public boolean isRunning() {
		return enabled ? running : false;
	}
	
	@Override
	protected void onEndOfQueue() {
		if (isEnabled() == false) {
			stopSelf();
		} 
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

	public void sendProbeDetails(PendingIntent callback) {
		Details details = new Details(getClass().getName(), getDisplayName(), getRequiredPermissions(), getRequiredFeatures(), getAvailableParameters());
		Intent detailsIntent = new Intent(ACTION_DETAILS);
		detailsIntent.putExtras(details.getBundle());
		callback(Utils.millisToSeconds(System.currentTimeMillis()), detailsIntent, callback);
	}

	
	public void sendProbeStatus(PendingIntent callback) {
		Status status = new Status(getClass().getName(), enabled, isRunning(), getNextRunTime(), getPreviousRunTime());
		Intent statusValuesIntent = new Intent(ACTION_STATUS);
		statusValuesIntent.putExtras(status.getBundle());
		callback(Utils.millisToSeconds(System.currentTimeMillis()),statusValuesIntent, callback);
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
		// Should always be loaded when enabled
		Intent dataIntent = new Intent(ACTION_DATA);
		dataIntent.putExtras(data);
		callback(epochTimestamp, dataIntent, null);
	}
	
	/**
	 * Send some values to each requesting pending intent
	 * @param valuesIntent
	 */
	protected void callback(long epochTimestamp, Intent valuesIntent, PendingIntent callback) {
		assert requestsIntent != null;
		valuesIntent.putExtra(PROBE, getClass().getName());
		valuesIntent.putExtra(TIMESTAMP, epochTimestamp);
		Log.d(TAG, "Queing probe " + valuesIntent.getAction() + " callback at " + epochTimestamp);
		if (isIntentHandlerThread()) {
			_callback_registered(valuesIntent, callback);
		} else {
			// Run on message queue to avoid concurrent modification of requests
			Intent callbackRegisteredIntent = new Intent(ACTION_INTERNAL_CALLBACK_REGISTERED, null, this, Probe.class);
			callbackRegisteredIntent.putExtra(INTERNAL_CALLBACK_INTENT, valuesIntent);
			callbackRegisteredIntent.putExtra(CALLBACK_KEY, callback);
			queueIntent(callbackRegisteredIntent); 
		}
	}
	
	private static boolean isDataRequestAcceptingPassiveData(Bundle dataRequest, Parameter[] availableParameters) {
		boolean acceptOpportunisticData = true;
		Parameter opportunisticParameter = Parameter.getAvailableParameter(availableParameters, Parameter.Builtin.OPPORTUNISTIC);
		if (opportunisticParameter != null) {
			acceptOpportunisticData = dataRequest.getBoolean(opportunisticParameter.getName(), (Boolean)opportunisticParameter.getValue());
		}
		return acceptOpportunisticData;
	}
	
	private boolean isDataRequestSatisfied(Bundle dataRequest, Parameter[] availableParameters, long dataTime, long lastDataSentTime) {
		Parameter periodParameter = Parameter.getAvailableParameter(availableParameters, Parameter.Builtin.PERIOD);
		if (periodParameter == null) {
			return true;
		} else {
			long period = Utils.getLong(dataRequest, periodParameter.getName(), (Long)periodParameter.getValue());
			return dataTime >= (lastDataSentTime + period);
		}
	}
	
	/**
	 * Send some values to each requesting pending intent
	 * @param valuesIntent
	 */
	protected void _callback_registered(Intent valuesIntent, PendingIntent callback) {
		long epochTimestamp = valuesIntent.getLongExtra(TIMESTAMP, 0L);
		Set<PendingIntent> callbacks = new HashSet<PendingIntent>();
		if (callback != null) {
			callbacks.add(callback);
		}
		ArrayList<Intent> requests =  null;
		if (requestsIntent != null) {
			requests = requestsIntent.getParcelableArrayListExtra(INTERNAL_REQUESTS_KEY);
		}

		if (ACTION_DATA.equals(valuesIntent.getAction())) {
			// Send to all requesters
			if (requests != null && !requests.isEmpty()) {
				JSONObject dataSentTimes = null;
				try {
					dataSentTimes = new JSONObject(getHistoryPrefs().getString(MOST_RECENT_DATA_BY_REQUEST_KEY, "{}"));
				} catch (JSONException e) {
					Log.e(TAG, "Unable to parse data sent history.");
					dataSentTimes = new JSONObject();
				}
				
				Parameter[] availableParameters = getAvailableParameters();
				for (Intent request : requests) {
					PendingIntent requesterCallback = request.getParcelableExtra(CALLBACK_KEY);
					ArrayList<Bundle> dataRequests = Utils.getArrayList(request.getExtras(), REQUESTS_KEY);
					
					for (Bundle dataRequest : dataRequests) {
						String requestKey = normalizedStringRepresentation(dataRequest, getAvailableParameters());
						long lastDataSentTime = dataSentTimes.optLong(requestKey);
						// TODO: If data request accepts passive data, but also needs data enforced on a schedule
						// we may need to make logic more complicated
						if (isDataRequestSatisfied(dataRequest, availableParameters, epochTimestamp, lastDataSentTime)
								|| isDataRequestAcceptingPassiveData(dataRequest, availableParameters)) {
							callbacks.add(requesterCallback);
							try {
								dataSentTimes.put(requestKey, epochTimestamp);
							} catch (JSONException e) {
								Log.e(TAG, "Unable to store data sent time in .");
							}
						}
					}
				}

				// Save the data sent times
				getHistoryPrefs().edit()
				.putString(MOST_RECENT_DATA_BY_REQUEST_KEY, dataSentTimes.toString())
				.putLong(MOST_RECENT_DATA_KEY, epochTimestamp)
				.commit();
			}

		} else {
			// Add all
			for (Intent request : requests) {
				PendingIntent requesterCallback = request.getParcelableExtra(CALLBACK_KEY);
				callbacks.add(requesterCallback);
			}
		}
		

		Log.d(TAG, "Sent probe data at " + epochTimestamp);
		for (PendingIntent requesterCallback : callbacks) {
			try {
				requesterCallback.send(this, 0, valuesIntent);
			} catch (CanceledException e) {
				Log.w(TAG, "Unable to send to canceled pending intent at " + epochTimestamp);
				Map<PendingIntent,Intent> callbacksToRequests = getCallbacksToRequests(requests);
				Intent request = callbacksToRequests.get(requesterCallback);
				if (request != null) {
					deadRequests.add(request);
				}
			}
		}
		
		updateRequests();
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
	
	
	public final static class Status {
		private Bundle bundle;
		public Status(String name,
				boolean enabled, boolean running, 
				long nextRun, long previousRun) {
			this.bundle = new Bundle();
			bundle.putBoolean("ENABLED", enabled);
			bundle.putBoolean("RUNNING", running);
			bundle.putLong("NEXT_RUN", nextRun);
			bundle.putLong("PREVIOUS_RUN", previousRun);
			bundle.putString(Probe.PROBE, name);
		}
		
		public Status(Bundle bundle) {
			this.bundle = new Bundle(bundle);
		}
		
		public String getProbe() {
			return bundle.getString(Probe.PROBE);
		}
		public boolean isEnabled() {
			return bundle.getBoolean("ENABLED");
		}
		public boolean isRunning() {
			return bundle.getBoolean("RUNNING");
		}
		public long getNextRun() {
			return bundle.getLong("NEXT_RUN");
		}
		public long getPreviousRun() {
			return bundle.getLong("PREVIOUS_RUN");
		}
		public Bundle getBundle() {
			return bundle;
		}
		@Override
		public boolean equals(Object o) {
			return o != null 
			&& o instanceof Status 
			&& getProbe().equals(((Status)o).getProbe());
		}
		@Override
		public int hashCode() {
			return getProbe().hashCode();
		}
	}
	
	public final static class Details {
		private Bundle bundle;
		public Details(String name, String displayName, 
				String[] requiredPermissions, 
				String[] requiredFeatures, 
				Parameter[] parameters) {
			this.bundle = new Bundle();
			bundle.putString(PROBE, name);
			bundle.putString("DISPLAY_NAME", displayName);
			bundle.putStringArray("REQUIRED_PERMISSIONS", requiredPermissions == null ? new String[]{} : requiredPermissions);
			bundle.putStringArray("REQUIRED_FEATURES", requiredFeatures == null ? new String[]{} : requiredFeatures);
			ArrayList<Bundle> paramBundles = new ArrayList<Bundle>();
			if (parameters != null) {
				for (Parameter param : parameters) {
					paramBundles.add(param.getBundle());
				}
			}
			bundle.putParcelableArrayList("PARAMETERS", paramBundles);
		}
		public Details(Bundle bundle) {
			this.bundle = bundle;
		}
		
		public String getName() {
			return bundle.getString(PROBE);
		}
		public String getDisplayName() {
			return bundle.getString("DISPLAY_NAME");
		}
		
		public String[] getRequiredPermissions() {
			return bundle.getStringArray("REQUIRED_PERMISSIONS");
		}
		public String[] getRequiredFeatures() {
			return bundle.getStringArray("REQUIRED_FEATURES");
		}
		public Parameter getParameter(final String name) {
			for(Parameter parameter : getParameters()) {
				if (parameter.getName().equalsIgnoreCase(name)) {
					return parameter;
				}
			}
			return null;
		}
		public Parameter[] getParameters() {
			ArrayList<Bundle> paramBundles = bundle.getParcelableArrayList("PARAMETERS");
			List<Parameter> paramList = new ArrayList<Parameter>();
			for (Bundle paramBundle : paramBundles) {
				paramList.add(new Parameter(paramBundle));
			}
			Parameter[] parameters = new Parameter[paramBundles.size()];
			paramList.toArray(parameters);
			return parameters;
		}
		public Bundle getBundle() {
			return bundle;
		}
		@Override
		public boolean equals(Object o) {
			return o != null 
			&& o instanceof Details 
			&& getName().equals(((Details)o).getName());
		}
		@Override
		public int hashCode() {
			return getName().hashCode();
		}
	}
	

	/**
	 * Represents a parameter that can be passed to a probe
	 * @author alangardner
	 *
	 */
	public final static class Parameter {
		/**
		 * The built-in parameters that the Funf system knows how to handle
		 * @author alangardner
		 *
		 */
		public enum Builtin {
			OPPORTUNISTIC("OPPORTUNISTIC", "Opportunistic", "Whether the requester wants data they did not specifically request."),
			DURATION("DURATION", "Duration", "Length of time probe will run for (seconds)"),
			START("START_DATE", "Start Timestamp", "Date after which probe is allowed to run (seconds since epoch)"),
			END("END_DATE", "End Timestamp", "Date before which probe is allowed to run (seconds since epoch)"),
			PERIOD("PERIOD", "Period", "Length of time between probe runs (seconds)");
			
			public final String name, displayName, description;
			
			private  Builtin(String name, String displayName, String description) {
				this.name = name;
				this.displayName = displayName;
				this.description = description;
			}
		}
		
		
		public static final String NAME_KEY = "NAME";
		public static final String DEFAULT_VALUE_KEY = "DEFAULT_VALUE";
		public static final String DISPLAY_NAME_KEY = "DISPLAY_NAME";
		public static final String DESCRIPTION_KEY = "DESCRIPTION";
		
		private final Bundle paramBundle;
		
		/**
		 * Custom parameter constructor
		 * @param name
		 * @param value
		 * @param displayName
		 * @param description
		 */
		public Parameter(final String name, final Object value, final String displayName, final String description) {
			paramBundle = new Bundle();
			paramBundle.putString(NAME_KEY, name);
			paramBundle.putString(DISPLAY_NAME_KEY, displayName);
			paramBundle.putString(DESCRIPTION_KEY, description);
			Utils.putInBundle(paramBundle, DEFAULT_VALUE_KEY, value);
		}
		
		public Parameter(final Bundle paramBundle) {
			// TODO: we might want to ensure that the bundle has the appropriate keys
			this.paramBundle = paramBundle;
		}
		
		/**
		 * System parameter constructor, to be handled by system
		 * @param paramType
		 * @param defaultValue
		 * @param supportedByProbe
		 */
		public Parameter(final Parameter.Builtin paramType, final Object defaultValue) {
			this(paramType.name, defaultValue, paramType.displayName, paramType.description);
		}

		public String getName() {
			return paramBundle.getString(NAME_KEY);
		}

		public Object getValue() {
			return paramBundle.get(DEFAULT_VALUE_KEY);
		}

		public String getDisplayName() {
			return paramBundle.getString(DISPLAY_NAME_KEY);
		}

		public String getDescription() {
			return paramBundle.getString(DESCRIPTION_KEY);
		}
		
		public Bundle getBundle() {
			return paramBundle;
		}

		public static Parameter getAvailableParameter(Parameter[] availableParameters, Builtin systemParam) {
			for (Parameter p : availableParameters) {
				if(systemParam.name.equals(p.getName())) {
					return p;
				}
			}
			return null;
		}
		
	}
	


	public static String normalizedStringRepresentation(Bundle dataRequest, Parameter[] availableParameters) {
		// We use JSONObject to ensure special characters are properly and consistently escaped during serialization
		// But key order is not guaranteed, so we make many of them
		List<String> representations = new ArrayList<String>();
		for (Parameter parameter : availableParameters) {
			String name = parameter.getName();
			Object value = dataRequest.get(name);
			if (value != null) {
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put(name, value);
					representations.add(jsonObject.toString());
				} catch (JSONException e) {
					Log.e(Utils.TAG, "Unable to serialize parameter name '" + name + "' with value '" + value + "'");
				}
			}
		}
		Collections.sort(representations);
		return "[" + Utils.join(representations, ",") + "]";
	}

	
	/**
	 * Binder interface to the probe
	*/
	public class LocalBinder extends Binder {
		Probe getService() {
			return Probe.this;
		}
	}
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
