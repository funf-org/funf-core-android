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

import static edu.mit.media.hd.funf.Utils.nonNullStrings;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.hd.funf.HashCodeUtil;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.OppProbe.Status;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

public abstract class Probe extends Service {

	
	protected final String TAG = getClass().getName();
	
	public static final String TIMESTAMP = "TIMESTAMP";
	
	private static final String MOST_RECENT_RUN_KEY = "mostRecentTimeRun";
	private static final String MOST_RECENT_KEY = "mostRecentTimeDataSent";
	private static final String MOST_RECENT_PARAMS_KEY = "mostRecentParamsSent";
	private static final String NONCES_KEY = "nonces";
	
	private PowerManager.WakeLock lock;
	private long mostRecentTimeRun;
	private long mostRecentTimeDataSent;
	private Bundle mostRecentParameters;
	private long nextRunTime;
	private SharedPreferences prefs;
	private boolean enabled;
	private boolean running;
	private StopTimer stopTimer;
	private Set<Nonce> nonces;
	
	// TODO: keep list of all active requests to this probe
	private ProbeRequests allRequests;
	
	@Override
	public final void onCreate() {
		Log.i(TAG, "CREATED");
		prefs = getSharedPreferences("PROBE_" + getClass().getName(), MODE_PRIVATE);
		allRequests = ProbeRequests.getRequestsForProbe(this, getClass().getName());
		mostRecentTimeDataSent = prefs.getLong(MOST_RECENT_KEY, 0);
		mostRecentTimeRun = prefs.getLong(MOST_RECENT_RUN_KEY, 0);
		mostRecentParameters = Utils.getBundleFromPrefs(prefs, MOST_RECENT_PARAMS_KEY);
		nonces = Nonce.unserializeNonces(prefs.getString(NONCES_KEY, null));
		enabled = false;
		running = false;
		stopTimer = new StopTimer();
	}

	@Override
	public final void onDestroy() {
		Log.i(TAG, "DESTROYED");
		if (prefs != null) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(MOST_RECENT_KEY, mostRecentTimeDataSent);
			editor.putLong(MOST_RECENT_RUN_KEY, mostRecentTimeRun);
			Log.i(TAG, "Most recent run onDestroy: " + mostRecentTimeRun);
			editor.putString(NONCES_KEY, Nonce.serializeNonces(nonces));
			try {
				Utils.putInPrefs(editor, MOST_RECENT_PARAMS_KEY, mostRecentParameters);
			} catch (UnstorableTypeException e) {
				Log.e(TAG, e.getLocalizedMessage());
			}
			editor.commit();
		}
		if (enabled) {
			disable();
		}
	}
	
	@Override
	public final int onStartCommand(Intent intent, int flags, int startId) {
		Bundle extras = intent.getExtras();
		String requester = extras.getString(OppProbe.ReservedParamaters.REQUESTER.name);
		Log.i(TAG, "Requester: " + String.valueOf(requester));
		if (requester != null && packageHasRequiredPermissions(requester)) {
			Log.i(TAG, "Updating requests");
			updateRequests(intent);
		}
		run();
		return START_REDELIVER_INTENT;
	}
	
	private void updateRequests(Intent requestIntent) {
		Bundle extras = requestIntent.getExtras();
		String requester = extras.getString(OppProbe.ReservedParamaters.REQUESTER.name);
		String requestId = extras.getString(OppProbe.ReservedParamaters.REQUEST_ID.name);
		requestId = (requestId == null) ? "" : requestId;
		long nonce = extras.getLong(OppProbe.ReservedParamaters.NONCE.name, -1L);
		if (redeemNonce(nonce)) {
			// null REQUESTER is internal (ProbeController does not allow null REQUESTER)
			// TODO: may need to handle default top level bundle parameters
			Bundle[] requests = Utils.copyBundleArray(extras.getParcelableArray(OppProbe.ReservedParamaters.REQUESTS.name));
			
			if (requests.length == 0) {
				allRequests.remove(requester, requestId);
			} else {
				if (!allRequests.put(requester, requestId, requests)) {
					Log.w(TAG, "Unable to store requests for probe.");
				}
			}
		}
	}
	
	/**
	 * Resets all of the run data information
	 */
	public void reset() {
		this.mostRecentParameters = new Bundle();
		this.mostRecentTimeDataSent = 0;
		this.mostRecentTimeRun = 0;
		Log.i(TAG, "Most recent run reset: " + mostRecentTimeRun);
		this.nextRunTime = 0;
		this.nonces = new HashSet<Nonce>();
		allRequests = ProbeRequests.getRequestsForProbe(this, getClass().getName());
		cancelNextRun();
	}
	

	private void cleanRequests() {
		final long currentTime = System.currentTimeMillis();
		for (Map.Entry<String, Map<String,List<Bundle>>> requesterTorRequestIdToBundles : allRequests.getByRequesterByRequestId().entrySet()) {
			final String requester = requesterTorRequestIdToBundles.getKey();
			final Map<String,List<Bundle>> requestIdsToBundles = requesterTorRequestIdToBundles.getValue();
			for (Map.Entry<String, List<Bundle>> requestIdToBundles : requestIdsToBundles.entrySet()) {
				final String requestId = requestIdToBundles.getKey();
				final List<Bundle> bundles = requestIdToBundles.getValue();
				Set<Bundle> bundlesToRemove = new HashSet<Bundle>();
				for (Bundle bundle : bundles) {
					Bundle params = getCompleteParams(bundle);
					long period = Utils.getLong(params, SystemParameter.PERIOD.name, 0L) * 1000;
					long endTime = Utils.getLong(params, SystemParameter.END.name, 0L) * 1000;
					if (period == 0L || endTime > currentTime) {
						bundlesToRemove.add(bundle);
					}
				}
				if (!bundlesToRemove.isEmpty()) {
					bundles.removeAll(bundlesToRemove);
					Bundle[] cleanedBundles = new Bundle[bundles.size()];
					bundles.toArray(cleanedBundles);
					allRequests.put(requester, requestId, cleanedBundles);
				}
			}
		}
	}
	
	public long getPreviousDataSentTime() {
		return mostRecentTimeDataSent;
	}

	/**
	 * @return a timestamp (in millis since epoch) of the most recent time this probe was run 
	 */
	public long getPreviousRunTime() {
		return mostRecentTimeRun;
	}
	
	/**
	 * @return the bundle of params used to run the probe the most recent time it was run
	 */
	public Bundle getPreviousRunParams() {
		return mostRecentParameters;
	}

	/**
	 * Sends a STATUS broadcast for the probe.
	 */
	public void sendProbeStatus(final String packageName, final boolean includeNonce) {
		Intent statusBroadcast = new Intent(OppProbe.getStatusAction());
		String name = getClass().getName();
		String displayName = getClass().getName().replace(getClass().getPackage().getName() + ".", "");
		
		List<String> requiredPermissionsList = new ArrayList<String>(Arrays.asList(nonNullStrings(getRequiredPermissions())));
		if (!requiredPermissionsList.contains(android.Manifest.permission.WAKE_LOCK)) {
			requiredPermissionsList.add(android.Manifest.permission.WAKE_LOCK);
		}
		String[] requiredPermissions = new String[requiredPermissionsList.size()];
		requiredPermissionsList.toArray(requiredPermissions);
		List<OppProbe.Parameter> parameters = new ArrayList<OppProbe.Parameter>();
		for (Parameter param : getAvailableParametersNotNull()) {
			parameters.add(param);
		}
		Status status = new Status(
				name,
				displayName,
				enabled,
				isRunning(),
				nextRunTime,
				mostRecentTimeRun,
				requiredPermissions,
				nonNullStrings(getRequiredFeatures()),
				parameters
				);
		statusBroadcast.putExtras(status.getBundle());
		if (packageName != null) {
			statusBroadcast.setPackage(packageName);
			if (includeNonce && packageHasRequiredPermissions(packageName)) {
				statusBroadcast.putExtra(OppProbe.ReservedParamaters.NONCE.name, createNonce());
			}
		}
		Log.i(TAG, "Sending probe status to '" + statusBroadcast.getPackage() + '"');
		sendBroadcast(statusBroadcast);
	}
	
	private static PackageInfo getPackageInfo(Context context, String packageName) {
		List<PackageInfo> apps = context.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
		for (PackageInfo info : apps) {
			if (info.packageName.equals(packageName)) {
				return info;
			}
		}
		return null;
	}
	
	private boolean packageHasRequiredPermissions(String packageName) {
		//Log.i(TAG, "Getting package info for '" + packageName + "'");
		PackageInfo info = getPackageInfo(this, packageName);
		if (info == null) {
			Log.i(TAG, "Package '" + packageName + "' is not installed.");
			return false;
		}
		
		Set<String> packagePermissions = new HashSet<String>(Arrays.asList(info.requestedPermissions));
		//Log.i(TAG, "Package permissions for '" + packageName + "': " + Utils.join(packagePermissions, ", "));
		for (String permission :  nonNullStrings(getRequiredPermissions())) {
			if (!packagePermissions.contains(permission)) {
				Log.i(TAG, "Package '" + packageName + "' does not have the required permission '" + permission + "' to run this probe.");
				return false;
			}
		}
		return true;
	}
	
	private void removeInvalidNonces() {
		List<Nonce> noncesToRemove = new ArrayList<Nonce>();
		for (Nonce oldNonce : nonces) {
			if(!oldNonce.isValid()) {
				noncesToRemove.add(oldNonce);
			}
		}
		nonces.removeAll(noncesToRemove);
	}
	
	
	
	/**
	 * Sends a DATA broadcast of the current data from the probe, if it is available.
	 */
	public abstract void sendProbeData();
	
	/**
	 * Sends a DATA broadcast for the probe, and records the time.
	 */
	protected void sendProbeData(long timestamp, Bundle params, Bundle data) {
		Log.i(TAG, getClass().getName() + " sent probe data at " + timestamp);
		mostRecentTimeDataSent = timestamp;
		Intent dataBroadcast = new Intent(OppProbe.getDataAction(getClass()));
		dataBroadcast.putExtra(TIMESTAMP, timestamp);
		// TODO: should we send parameters with data broadcast?
		dataBroadcast.putExtras(data);
		Set<String> requestingPackages = allRequests.getByRequesterByRequestId().keySet();
		for (String requestingPackage : requestingPackages) {
			Intent scopedDataBroadcast = new Intent(dataBroadcast);
			scopedDataBroadcast.setPackage(requestingPackage);
			sendBroadcast(dataBroadcast);
		}
	}
	
	/**
	 * Returns the set of probe interfaces this probe implements.  
	 * By default every probe implements a unique interface based on its name.
	 * However, probes can agree on a name and set of parameters that they respond to to implement the same probe interface.
	 * @return
	 */
	protected String[] getProbeInterfaces() {
		return new String[]{getClass().getName()};
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
	private Parameter[] getAvailableParametersNotNull() {
		Parameter[] availableParameters = getAvailableParameters();
		return (availableParameters == null) ? new Parameter[] {} : availableParameters;
	}
	
	/**
	 * Enables the probe start gathering data and sending broadcasts as appropriate.
	 * Multiple calls to this method will alter the parameters with which the probe is running.
	 * Depending on the probe implementation, the probe may stop automatically after it runs.
	 * @param params
	 */
	public final void run() {
		Log.i(TAG, "Running probe: " + getClass().getName());

		if (!enabled) {
			enable();
		}
		Log.i(TAG, "Is Running: " + running);
		if (!running) {
			// Merge all schedules and run if necessary
			ProbeScheduleResolver scheduleResolver = new ProbeScheduleResolver(allRequests.getAll(), getDefaultParameters(), getPreviousRunTime(), getPreviousRunParams());
			Bundle completeParams = getCompleteParams(scheduleResolver.getNextRunParams());
			Log.i(TAG, "Should run: " + shouldRunNow(completeParams));
			if (shouldRunNow(completeParams)) {
				running = true;
				if (lock == null) {
					lock = Utils.getWakeLock(this);
				}
				Log.i(TAG, "Started Running " + getClass().getCanonicalName());
				mostRecentTimeRun = System.currentTimeMillis();
				Parameter durationParam = getAvailableSystemParameter(SystemParameter.DURATION);
				if (durationParam != null && !durationParam.isSupportedByProbe()) {
					long duration = Utils.getLong(completeParams, SystemParameter.DURATION.name, 0L) * 1000;
					if (duration > 0) {
						stopTimer.scheduleStop(duration);
					}
				}
				Log.i(TAG, "Calling onRun for probe: " + getClass().getName());
				onRun(completeParams); // call onRun to update parameters
			} else if (allRequests.getAll().size() > 0) {
				Log.i(TAG, "Scheduling");
				scheduleNextRun();
			} else {
				Log.i(TAG, "Disabling");
				disable();
			}
		}
	}
	
	/**
	 * Schedules a stop for the latest delay time it receives.  Delay time is always
	 * measured from the time 'schedule' is called.
	 *
	 */
	private class StopTimer  {
		// TODO: may need to synchronize for multithreaded case
		private Timer timer;
		private long latestRunToTime = 0;
		
		public void cancel() {
			if (timer != null) {
				timer.cancel();
				timer.purge();
				timer = null;
			}
		}

		public void scheduleStop(long delay) {
			long newRunToTime = System.currentTimeMillis() + delay;
			if (newRunToTime > latestRunToTime) {
				latestRunToTime = newRunToTime;
				cancel();
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						stop();
					}
				}, delay);
			}
		}
	}
	
	private Parameter getAvailableSystemParameter(SystemParameter systemParam) {
		for (Parameter p : getAvailableParametersNotNull()) {
			if(systemParam.name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
	
	private Bundle getDefaultParameters() {
		Bundle params = new Bundle();
		for(Parameter param :getAvailableParametersNotNull()) {
			Utils.putInBundle(params, param.getName(), param.getValue());
		}
		return params;
	}
	
	private Bundle getCompleteParams(Bundle params) {
		if (params == null) {
			return null;
		}
		Bundle completeParams = getDefaultParameters();
		// TODO: only use parameters that are specified
		// for (Parameter param : probe.getAvailableParameters()) {
    	// Utils.putInBundle(params, param.getName(), param.getValue());
        //}
		completeParams.putAll(params);
		return completeParams;
	}
	
	private boolean shouldRunNow(Bundle params) {
		if (params == null) {
			return false;
		}
		long period = Utils.getLong(params, SystemParameter.PERIOD.name, 0L) * 1000;
		long startTime = Utils.getLong(params, SystemParameter.START.name, 0L) * 1000;
		long endTime = Utils.getLong(params, SystemParameter.END.name, 0L) * 1000;
		long currentTime = System.currentTimeMillis();
		Log.i(TAG, Utils.join(Arrays.asList(period, startTime, endTime, currentTime, mostRecentTimeRun), ", "));
		return (startTime == 0 || startTime <= currentTime) // After start time (if exists)
			&& (endTime == 0 || currentTime <= endTime)   // Before end time (if exists)
			&& (period == 0 || (mostRecentTimeRun + period) <= currentTime); // At least one period since last run
	}	
	
	private void scheduleNextRun() {
		cleanRequests(); // Cleaning currently removes any existing 0 period requests
		// TODO: need to be smarter about this.  Probe may handle period, but not start or end times.
		Parameter periodParam = getAvailableSystemParameter(SystemParameter.PERIOD);
		if (periodParam == null || periodParam.isSupportedByProbe()) {
			Log.i(TAG, "PERIOD parameter not supported by  " + getClass().getName());
			return;
		}
		ProbeScheduleResolver scheduleResolver = new ProbeScheduleResolver(allRequests.getAll(), getDefaultParameters(), getPreviousRunTime(), getPreviousRunParams());
		Bundle nextRunParams = scheduleResolver.getNextRunParams();
		if (nextRunParams != null) {
			Intent nextRunIntent = new Intent(this, getClass());
			nextRunIntent.putExtras(nextRunParams);
			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextRunIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			long nextRunTime = scheduleResolver.getNextRunTime();
			this.nextRunTime = nextRunTime;
			Log.i(TAG, "Next run time: " + nextRunTime);
			if (nextRunTime != 0L) {
				Log.i(TAG, "LAST_TIME: " + mostRecentTimeRun);
				Log.i(TAG, "NEXT_TIME: " + nextRunTime);
				Log.i(TAG, "CURRENT_TIME: " + System.currentTimeMillis());
				Log.i(TAG, "DIFFERENCE: " + (nextRunTime - System.currentTimeMillis()));
				am.set(AlarmManager.RTC_WAKEUP, nextRunTime, pendingIntent);
			}
		}
	}
	
	private void cancelNextRun() {
		Intent nextRunIntent = new Intent(this, getClass());
		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextRunIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(pendingIntent);
		this.nextRunTime = 0L;
	}
	
	/**
	 * Start actively running the probe.  Send data broadcast when done (or when appropriate) and stop.
	 * @param params
	 */
	protected abstract void onRun(Bundle params);
	
	/**
	 * Disables the probe from collecting data.  The probe may send out one last broadcast, but no more afterwards.
	 * Only one call to this method is required to disable the probe, regardless of how many time start as been called.
	 * If this method is called when the probe is already disabled, the method returns gracefully.
	 * @param params
	 */
	public final void stop() {
		if (enabled && running) {
			Log.i(TAG, "Stopping probe: " + getClass().getName());
			onStop();
			running = false;
			if (allRequests.getAll().size() > 0) {
				scheduleNextRun();
			} else {
				disable();
			}
			if (lock != null && lock.isHeld()) {
				lock.release();
				lock = null;
			}
		}
	}
	
	/**
	 * Stop actively running the probe.  Any passive listeners should continue running.
	 */
	protected abstract void onStop();
	
	
	/**
	 * Turn on probe, setting it up to be run and registering passive listeners (if any).
	 */
	public final void enable() {
		if (!enabled) {
			Log.i(TAG, "Enabling probe: " + getClass().getName());
			enabled = true;
			running = false;
			onEnable();
		}
	}
	/**
	 * Start actively running the probe.  Send data broadcast when done (or when appropriate) and stop.
	 */
	protected abstract void onEnable();
	
	/**
	 * Turn off probe, stopping if currently running and disabling all passive listeners.
	 */
	public final void disable() {
		if (enabled) {
			Log.i(TAG, "Disabling probe: " + getClass().getName());
			if (running) {
				stop();
			}
			enabled = false;
			onDisable();
			stopSelf();
		}
	}
	
	/**
	 * Disable any passive listeners and tear down the service.  Will be called when probe service is destroyed.
	 */
	protected abstract void onDisable();
	
	/**
	 * @return true if actively running, otherwise false
	 */
	public boolean isRunning() {
		return enabled ? running : false;
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
	

	/**
	 * Create a new valid nonce
	 * @return long value of the nonce
	 */
	protected long createNonce() {
		removeInvalidNonces();
		Nonce nonce = new Nonce();
		nonces.add(nonce);
		return nonce.value;
	}
	
	/**
	 * Checks if the nonce and valid and invalidates it if it exists.
	 * @param nonce
	 * @return true if valid nonce, false otherwise
	 */
	protected boolean redeemNonce(long nonce) {
		removeInvalidNonces();
		Nonce redeemedNonce = null;
		for (Nonce existingNonce : nonces) {
			if (existingNonce.value == nonce) {
				redeemedNonce = existingNonce;
				break;
			}
		}
		nonces.remove(redeemedNonce);
		return nonces != null;
	}
	

	private static class Nonce {
		public final long value;
		public final long timestamp;
		public Nonce() {
			this(Math.abs(new Random().nextLong()), System.currentTimeMillis());
		}
		public Nonce(long value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
		public boolean isValid() {
			return System.currentTimeMillis() < (timestamp + 1000); // 1 second expiration
		}
		@Override
		public boolean equals(Object o) {
			return o != null && o instanceof Nonce && this.value == ((Nonce)o).value;
		}
		@Override
		public int hashCode() {
			return HashCodeUtil.hash(HashCodeUtil.SEED, value);
		}
		

		public static String serializeNonces(Set<Nonce> nonces) {
			Set<String> nonceStrings = new HashSet<String>();
			for (Nonce nonce : nonces) {
				nonceStrings.add(nonce.value + "@" + nonce.timestamp);
			}
			return Utils.join(nonceStrings, ",");
		}
		
		public static Set<Nonce> unserializeNonces(String noncesString) {
			Set<Nonce> nonces = new HashSet<Nonce>();
			if (noncesString != null && !noncesString.trim().equals("")) {
				String[] nonceStrings = noncesString.split(",");
				for (String nonceString : nonceStrings) {
					String[] nonceParts = nonceString.split("@");
					nonces.add(new Nonce(Long.valueOf(nonceParts[0]), Long.valueOf(nonceParts[1])));
				}
			}
			return nonces;
		}
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
		START("START_DATE", "Start Timestamp", "Date after which probe is allowed to run (millis since epoch)"),
		END("END_DATE", "End Timestamp", "Date before which probe is allowed to run (millis since epoch)"),
		PERIOD("PERIOD", "Period", "Length of time between probe runs (seconds)");
		
		public final String name, displayName, description;
		
		private  SystemParameter(String name, String displayName, String description) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
		}
	}
	

}
