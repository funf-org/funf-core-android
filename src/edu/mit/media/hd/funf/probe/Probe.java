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

import static edu.mit.media.hd.funf.probe.Utils.nonNullStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

public abstract class Probe extends Service {

	
	private static final String TAG = Probe.class.getName();
	private static final String MOST_RECENT_RUN_KEY = "mostRecentTimeRun";
	private static final String MOST_RECENT_KEY = "mostRecentTimeDataSent";
	private static final String MOST_RECENT_PARAMS_KEY = "mostRecentTimeDataSent";
	
	private PowerManager.WakeLock lock;
	private long mostRecentTimeRun;
	private long mostRecentTimeDataSent;
	private Bundle mostRecentParameters;
	private long nextRunTime;
	private SharedPreferences prefs;
	private boolean enabled;
	private boolean running;
	private StopTimer stopTimer;
	
	
	// TODO: keep list of all active requests to this probe
	private ProbeRequests requests;
	
	@Override
	public final void onCreate() {
		prefs = getSharedPreferences("PROBE_" + getClass().getName(), MODE_PRIVATE);
		requests = ProbeRequests.getRequestsForProbe(this, getClass().getName());
		mostRecentTimeDataSent = prefs.getLong(MOST_RECENT_KEY, 0);
		mostRecentTimeRun = prefs.getLong(MOST_RECENT_RUN_KEY, 0);
		mostRecentParameters = Utils.getBundleFromPrefs(prefs, MOST_RECENT_PARAMS_KEY);
		enabled = false;
		running = false;
		stopTimer = new StopTimer();
	}

	@Override
	public final void onDestroy() {
		if (prefs != null) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(MOST_RECENT_KEY, mostRecentTimeDataSent);
			editor.putLong(MOST_RECENT_RUN_KEY, mostRecentTimeRun);
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
		run(intent.getExtras());
		return START_STICKY;
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
	public void sendProbeStatus() {
		Intent statusBroadcast = new Intent(Utils.getStatusAction());
		String name = getClass().getName();
		String displayName = getClass().getName().replace(getClass().getPackage().getName() + ".", "");
		
		List<String> requiredPermissionsList = new ArrayList<String>(Arrays.asList(nonNullStrings(getRequiredPermissions())));
		if (!requiredPermissionsList.contains(android.Manifest.permission.WAKE_LOCK)) {
			requiredPermissionsList.add(android.Manifest.permission.WAKE_LOCK);
		}
		String[] requiredPermissions = new String[requiredPermissionsList.size()];
		requiredPermissionsList.toArray(requiredPermissions);
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter(SystemParameter.ENABLED, true));
		parameters.add(new Parameter(SystemParameter.REQUESTER, ""));
		for (Parameter param : getAvailableParametersNotNull()) {
			// Add all parameters except duplicate enabled and requester parameters
			if (!(SystemParameter.ENABLED.name.equals(param.getName()) || 
					SystemParameter.REQUESTER.name.equals(param.getName()))) {
				parameters.add(param);
			}
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
		sendBroadcast(statusBroadcast);
	}
	
	/**
	 * Sends a DATA broadcast of the current data from the probe, if it is available.
	 */
	public abstract void sendProbeData();
	
	/**
	 * Sends a DATA broadcast for the probe, and records the time.
	 */
	protected void sendProbeData(long timestamp, Bundle params, Bundle data) {
		mostRecentTimeDataSent = timestamp;
		Intent dataBroadcast = new Intent(Utils.getDataAction(getClass()));
		dataBroadcast.putExtra("TIMESTAMP", timestamp);
		// TODO: should we send parameters with data broadcast?
		dataBroadcast.putExtras(data);
		sendBroadcast(dataBroadcast); // TODO: send with permission required
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
	public final void run(Bundle params) {
		if (!requests.put(params)) {
			Log.w(TAG, "Unable to start probe because REQUESTER parameter was not specified");
			return; // Require successful storing of request
		}
		Log.i(TAG, "Running probe: " + getClass().getName());
		boolean requestEnabled = params.getBoolean(SystemParameter.ENABLED.name, true);
		if (!requestEnabled) {
			String requester = params.getString(SystemParameter.REQUESTER.name);
			requests.remove(requester);
		}

		if (!enabled) {
			enable();
		}
		Bundle completeParams = getCompleteParams(params);
		if (shouldRunNow(completeParams)) {
			running = true;
			if (lock == null) {
				lock = Utils.getWakeLock(this);
			}
			Log.i(TAG, "Started Running " + getClass().getCanonicalName());
			mostRecentTimeRun = System.currentTimeMillis();
			Parameter durationParam = getAvailableSystemParameter(SystemParameter.DURATION);
			if (durationParam != null && !durationParam.isSupportedByProbe()) {
				long duration = completeParams.getLong(SystemParameter.DURATION.name);
				if (duration > 0) {
					stopTimer.scheduleStop(duration);
				}
			}
			Log.i(TAG, "Calling onRun for probe: " + getClass().getName());
			onRun(completeParams); // call onRun to update parameters
		} 
		if (requests.getAll().size() > 0) {
			scheduleNextRun(params);
		} else {
			disable();
		}
		// TODO: clear out all unneeded requests (expired, no period, etc.)
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
		Bundle completeParams = getDefaultParameters();
		// TODO: only use parameters that are specified
		// for (Parameter param : probe.getAvailableParameters()) {
    	// Utils.putInBundle(params, param.getName(), param.getValue());
        //}
		completeParams.putAll(params);
		return completeParams;
	}
	
	private boolean shouldRunNow(Bundle params) {
		boolean enabled = params.getBoolean(SystemParameter.ENABLED.name, true);
		long period = params.getLong(SystemParameter.PERIOD.name, 0L) * 1000;
		long startTime = params.getLong(SystemParameter.START.name, 0L) * 1000;
		long endTime = params.getLong(SystemParameter.END.name, 0L) * 1000;
		long currentTime = System.currentTimeMillis();
		return enabled 
		    && (startTime == 0 || startTime <= currentTime) // After start time (if exists)
			&& (endTime == 0 || currentTime <= endTime)   // Before end time (if exists)
			&& (period == 0 || (mostRecentTimeRun + period) <= currentTime); // At least one period since last run
	}	
	
	private void scheduleNextRun(Bundle params) {
		// TODO: need to be smarter about this.  Probe may handle period, but not start or end times.
		Parameter periodParam = getAvailableSystemParameter(SystemParameter.PERIOD);
		if (periodParam == null || periodParam.isSupportedByProbe()) {
			return;
		}
		ProbeScheduleResolver scheduleResolver = new ProbeScheduleResolver(requests.getAll(), getDefaultParameters(), getPreviousRunTime(), getPreviousRunParams());
		Intent nextRunIntent = new Intent(this, getClass());
		nextRunIntent.putExtras(scheduleResolver.getNextRunParams());
		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextRunIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		long nextRunTime = scheduleResolver.getNextRunTime();
		this.nextRunTime = nextRunTime;
		if (nextRunTime != 0L) {
			Log.i(TAG, "LAST_TIME: " + mostRecentTimeRun);
			Log.i(TAG, "NEXT_TIME: " + nextRunTime);
			Log.i(TAG, "CURRENT_TIME: " + System.currentTimeMillis());
			Log.i(TAG, "DIFFERENCE: " + (nextRunTime - System.currentTimeMillis()));
			am.set(AlarmManager.RTC_WAKEUP, nextRunTime, pendingIntent);
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
			cancelNextRun();
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
	 * Convenience class for interacting with an OPP probe status bundle
	 * @author alangardner
	 *
	 */
	public final static class Status {
		private Bundle bundle;
		Status(String name, String displayName, 
				boolean enabled, boolean running, 
				long nextRun, long previousRun, 
				String[] requiredPermissions, 
				String[] requiredFeatures, 
				List<Parameter> parameters) {
			this.bundle = new Bundle();
			bundle.putBoolean("ENABLED", enabled);
			bundle.putBoolean("RUNNING", running);
			bundle.putLong("NEXT_RUN", nextRun);
			bundle.putLong("PREVIOUS_RUN", previousRun);
			bundle.putString("NAME", name);
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
		public Status(Bundle bundle) {
			this.bundle = bundle;
		}
		public String getName() {
			return bundle.getString("NAME");
		}
		public String getDisplayName() {
			return bundle.getString("DISPLAY_NAME");
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
		public String[] getRequiredPermissions() {
			return bundle.getStringArray("REQUIRED_PERMISSIONS");
		}
		public String[] getRequiredFeatures() {
			return bundle.getStringArray("REQUIRED_FEATURES");
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
			&& o instanceof Status 
			&& getName().equals(((Status)o).getName());
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

		public static final String NAME_KEY = "NAME";
		public static final String DEFAULT_VALUE_KEY = "DEFAULT_VALUE";
		public static final String DISPLAY_NAME_KEY = "DISPLAY_NAME";
		public static final String DESCRIPTION_KEY = "DESCRIPTION";
		
		private boolean supportedByProbe = true;
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
			this(paramType.name, defaultValue, paramType.displayName, paramType.description);
			this.supportedByProbe = supportedByProbe;
		}
		
		/**
		 * Convenience constructor to access parameter information from a bundle.
		 * WARNING: this will not correctly set the 'isSupportedByProbe' flag.  Use only for convenient access to other parameters.
		 * @param paramBundle
		 */
		public Parameter(final Bundle paramBundle) {
			// TODO: we might want to ensure that the bundle has the appropriate keys
			this.paramBundle = paramBundle;
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
		
		public boolean isSupportedByProbe() {
			return supportedByProbe;
		}
		
		public Bundle getBundle() {
			return paramBundle;
		}
	}
	
	/**
	 * The built-in parameters that the Funf system knows how to handle
	 * @author alangardner
	 *
	 */
	public enum SystemParameter {
		ENABLED("ENABLED", "Enabled", "Whether or not probe should run on requester's behalf."),
		REQUESTER("REQUESTER", "Requester", "The identifier for who is requesting data from this probe."),
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
