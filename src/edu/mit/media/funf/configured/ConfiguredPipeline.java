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
package edu.mit.media.funf.configured;

import static edu.mit.media.funf.AsyncSharedPrefs.async;
import static edu.mit.media.funf.Utils.TAG;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.funf.CustomizedIntentService;
import edu.mit.media.funf.FileUtils;
import edu.mit.media.funf.IOUtils;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.storage.BundleSerializer;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.HttpUploadService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.NameValueProbeDataListener;
import edu.mit.media.funf.storage.UploadService;

public abstract class ConfiguredPipeline extends CustomizedIntentService implements OnSharedPreferenceChangeListener {


	private static final String PREFIX = "edu.mit.media.funf.";
	public static final String
	ACTION_RELOAD = PREFIX + "reload",
	ACTION_UPDATE_CONFIG = PREFIX + "update",
	ACTION_UPLOAD_DATA = PREFIX + "upload",
	ACTION_ARCHIVE_DATA = PREFIX + "archive",
	ACTION_ENABLE = PREFIX + "enable",
	ACTION_DISABLE = PREFIX + "disable";
	
	public static final String
	EXTRA_FORCE_UPLOAD = "FORCE";

	public static final String LAST_CONFIG_UPDATE = "LAST_CONFIG_UPDATE";
	public static final String LAST_DATA_UPLOAD = "LAST_DATA_UPLOAD";
	
	public static final String
	CONFIG = "config",
	CONFIG_URL = "config_url",
	CONFIG_FILE = "config_file";

	private Map<String, Bundle[]> sentProbeRequests = null;
	private BroadcastReceiver dataListener;
	private Handler handler;

	public ConfiguredPipeline() {
		super("ConfiguredPipeline");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		handler = new Handler();
		ensureServicesAreRunning();
		getConfig().getPrefs().registerOnSharedPreferenceChangeListener(this);
		getSystemPrefs().registerOnSharedPreferenceChangeListener(this);
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
		getConfig().getPrefs().unregisterOnSharedPreferenceChangeListener(this);
		getSystemPrefs().unregisterOnSharedPreferenceChangeListener(this);
	}

	// HACK: Send a fake start id to prevent this service from being stopped
	// This is so we could use all of the other features of Intent service without rewriting them
	private static final int FAKE_START_ID = 98723546;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, FAKE_START_ID);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (ACTION_RELOAD.equals(action)) {
			reload();
		} else if(ACTION_UPDATE_CONFIG.equals(action)) {
			String config = intent.getStringExtra(CONFIG);
			String configUrl = intent.getStringExtra(CONFIG_URL);
			String configFilePath = intent.getStringExtra(CONFIG_FILE);
			if (config != null) {
				updateConfig(config);
			} else if (configFilePath != null) {
				File file = new File(configFilePath);
				updateConfig(file);
			} else if (configUrl != null) {
				try {
					updateConfig(new URL(configUrl));
				} catch (MalformedURLException e) {
					Log.e(TAG, "Unable to parse config url.");
				}
			} else {
				updateConfig();
			}
		} else if(ACTION_UPLOAD_DATA.equals(action)) {
			boolean force = intent.getBooleanExtra(EXTRA_FORCE_UPLOAD, false);
			uploadData(force);
		} else if(ACTION_ARCHIVE_DATA.equals(action)) {
			archiveData();
		} else if(ACTION_ENABLE.equals(action)) {
			setEnabled(true);
		} else if(ACTION_DISABLE.equals(action)) {
			setEnabled(false);
		} else if (Probe.ACTION_DATA.equals(action)) {
			onDataReceived(intent.getExtras());
		} else if (Probe.ACTION_STATUS.equals(action)) {
			onStatusReceived(new Probe.Status(intent.getExtras()));
		} else if (Probe.ACTION_DETAILS.equals(action)) {
			onDetailsReceived(new Probe.Details(intent.getExtras()));
		}
	}

	public static final String ENABLED_KEY = "enabled";
	public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
		Log.i(TAG, "Shared Prefs changed");
		if (sharedPreferences.equals(getConfig().getPrefs())) {
			Log.i(TAG, "Configuration changed");
			onConfigChange(getConfig().toString(true));
			if (FunfConfig.isDataRequestKey(key)) {
				if (isEnabled()) {
					String probeName = FunfConfig.keyToProbename(key);
					sendProbeRequest(probeName);
				}
			} else if (FunfConfig.CONFIG_UPDATE_PERIOD_KEY.equals(key)) {
				cancelAlarm(ACTION_UPDATE_CONFIG);
			} else if (FunfConfig.DATA_ARCHIVE_PERIOD_KEY.equals(key)) {
				cancelAlarm(ACTION_ARCHIVE_DATA);
			} else if (FunfConfig.DATA_UPLOAD_PERIOD_KEY.equals(key)) {
				cancelAlarm(ACTION_UPLOAD_DATA);
			}
			if (isEnabled()) {
				scheduleAlarms();
			}
			
		} else if (sharedPreferences.equals(getSystemPrefs()) && ENABLED_KEY.equals(key)) {
			Log.i(TAG, "System prefs changed");
			reload();
		}
	}
	
	public void reload() {
		cancelAlarms();
		removeProbeRequests();
		sentProbeRequests = null;
		if (isEnabled()) {
			ensureServicesAreRunning();
		}
	}
	
	private void scheduleAlarms() {
		FunfConfig config = getConfig();
		scheduleAlarm(ACTION_UPDATE_CONFIG, config.getConfigUpdatePeriod());
		scheduleAlarm(ACTION_ARCHIVE_DATA, config.getDataArchivePeriod());
		long uploadPeriod = config.getDataUploadPeriod();
		if (uploadPeriod > 0) {
			scheduleAlarm(ACTION_UPLOAD_DATA, config.getDataUploadPeriod());
		}
	}
	
	private void scheduleAlarm(String action, long delayInSeconds) {
		Intent i = new Intent(this, getClass());
		i.setAction(action);
		boolean noAlarmExists = (PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE) == null);
		if (noAlarmExists) {
			PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			long delayInMilliseconds = Utils.secondsToMillis(delayInSeconds);
			long startTimeInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
			Log.i(TAG, "Scheduling alarm for '" + action + "' at " + Utils.millisToSeconds(startTimeInMilliseconds) + " and every " + delayInSeconds  + " seconds");
			// Inexact repeating doesn't work unlesss interval is 15, 30 min, or 1, 6, or 24 hours
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTimeInMilliseconds, delayInMilliseconds, pi);
		}
	}
	
	private void cancelAlarms() {
		cancelAlarm(ACTION_UPDATE_CONFIG);
		cancelAlarm(ACTION_ARCHIVE_DATA);
		cancelAlarm(ACTION_UPLOAD_DATA);
	}
	
	private void cancelAlarm(String action) {
		Intent i = new Intent(this, getClass());
		i.setAction(action);
		PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE);
		if (pi != null) {
			AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			alarmManager.cancel(pi);
			pi.cancel();
		}
	}
	
	public void ensureServicesAreRunning() {
		if (isEnabled()) {
			scheduleAlarms();
			sendProbeRequests();
		}
	}
	
	public void setEncryptionPassword(char[] password) {
		DefaultArchive.getArchive(this, getPipelineName()).setEncryptionPassword(password);
	}
	
	protected PendingIntent getCallback() {
		// TODO: Maybe do a callback per probe, so they can be cancelled individually
		return PendingIntent.getService(this, 0, new Intent(this, getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	public void sendProbeRequests() {
		Map<String,Bundle[]> dataRequests = getConfig().getDataRequests();
		for (String probeName : dataRequests.keySet()) {
			sendProbeRequest(probeName);
		}
	}
	
	public void sendProbeRequest(String probeName) {
		Bundle[] requests = getConfig().getDataRequests().get(probeName);
		if (requests == null) {
			requests = new Bundle[] {}; // null is same as blank config
		}
		ArrayList<Bundle> dataRequest = new ArrayList<Bundle>(Arrays.asList(requests));
		Intent request = new Intent(Probe.ACTION_REQUEST);
		request.setClassName(this, probeName);
		request.putExtra(Probe.CALLBACK_KEY, getCallback());
		request.putExtra(Probe.REQUESTS_KEY, dataRequest);
		startService(request);
	}
	
	private void removeProbeRequests() {
		getCallback().cancel();
	}
	
	public static final String DEFAULT_PIPELINE_NAME = "mainPipeline";
	public String getPipelineName() {
		return DEFAULT_PIPELINE_NAME;
	}
	
	public void updateConfig() {
		String configUpdateUrl = getConfig().getConfigUpdateUrl();
		if (configUpdateUrl == null) {
			Log.i(TAG, "No update url configured.");
		} else {
			try {
				updateConfig(new URL(configUpdateUrl));
			} catch (MalformedURLException e) {
				Log.e(TAG, "Invalid update URL specified.", e);
			}
		}
	}
	
	public void updateConfig(URL url) {
		String jsonString = IOUtils.httpGet(url.toExternalForm(), null);
		updateConfig(jsonString);
	}
	
	private static final long MAX_REASONABLE_CONFIG_FILE_SIZE = (long)(Math.pow(2, 20));
	public void updateConfig(File file) {
		try {
			String jsonString = FileUtils.getStringFromFileWithLimit(file, MAX_REASONABLE_CONFIG_FILE_SIZE);
			updateConfig(jsonString);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Too large to a valid configuration file.", e);
		}
	}
	
	public void updateConfig(String jsonString) {
		if (jsonString == null) {
			Log.e(TAG, "A null configuration cannot be specified.");
			return;
		}
		try {
			Log.i(TAG, "Updating pipeline config.");
			// Write to temporary to compare
			FunfConfig tempConfig = getTemporaryConfig();
			boolean successfullyWroteConfig = tempConfig.edit().setAll(jsonString).commit();
			if (successfullyWroteConfig) {
				getSystemPrefs().edit().putLong(LAST_CONFIG_UPDATE, System.currentTimeMillis()).commit();
			}
			if (successfullyWroteConfig	&& !tempConfig.equals(getConfig())) {
				getConfig().edit().setAll(tempConfig).commit();
			}
		} catch (JSONException e) {
			Log.e(TAG, "Unable to update configuration.", e);
		}
	}
	
	protected void onConfigChange(String json) {
		// Record configuration change to database
		Intent i = new Intent(this, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_RECORD);
		i.putExtra(DatabaseService.DATABASE_NAME_KEY, getPipelineName());
		i.putExtra(NameValueDatabaseService.TIMESTAMP_KEY, System.currentTimeMillis());
		i.putExtra(NameValueDatabaseService.NAME_KEY, getClass().getName());
		i.putExtra(NameValueDatabaseService.VALUE_KEY, json);
		startService(i);
	}
	
	public void uploadData(boolean force) {
		archiveData();
		String archiveName = getPipelineName();
		String uploadUrl = getConfig().getDataUploadUrl();
		Intent i = new Intent(this, getUploadServiceClass());
		i.putExtra(UploadService.ARCHIVE_ID, archiveName);
		i.putExtra(UploadService.REMOTE_ARCHIVE_ID, uploadUrl);
		i.putExtra(UploadService.NETWORK, (!force && getConfig().getDataUploadOnWifiOnly()) ? UploadService.NETWORK_WIFI_ONLY : UploadService.NETWORK_ANY);
		startService(i);
		getSystemPrefs().edit().putLong(LAST_DATA_UPLOAD, System.currentTimeMillis()).commit();
	}
	
	public void uploadData() {
		uploadData(false);
	}
	
	public void archiveData() {
		Intent i = new Intent(this, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_ARCHIVE);
		i.putExtra(DatabaseService.DATABASE_NAME_KEY, getPipelineName());
		startService(i);
	}
	
	public boolean isEnabled() {
		return getSystemPrefs().getBoolean(ENABLED_KEY, true);
	}
	
	public boolean setEnabled(boolean enabled) {
		return getSystemPrefs().edit().putBoolean(ENABLED_KEY, enabled).commit();
	}
	
	public Class<? extends DatabaseService> getDatabaseServiceClass() {
		return NameValueDatabaseService.class;
	}
	
	public Class<? extends UploadService> getUploadServiceClass() {
		return HttpUploadService.class;
	}
	
	public BroadcastReceiver getProbeDataListener() {
		return new NameValueProbeDataListener(getPipelineName(), getDatabaseServiceClass(), getBundleSerializer());
	}
	
	public void onDataReceived(Bundle data) {
		String dataJson = getBundleSerializer().serialize(data);
		String probeName = data.getString(Probe.PROBE);
		long timestamp = data.getLong(Probe.TIMESTAMP, 0L);
		Bundle b = new Bundle();
		b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, getPipelineName());
		b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
		b.putString(NameValueDatabaseService.NAME_KEY, probeName);
		b.putString(NameValueDatabaseService.VALUE_KEY, dataJson);
		Intent i = new Intent(this, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_RECORD);
		i.putExtras(b);
		startService(i);
	}
	
	public void onStatusReceived(Probe.Status status) {
		// TODO:
	}
	
	public void onDetailsReceived(Probe.Details details) {
		// TODO:
	}
	
	// TODO: enable json serialization of bundles to eliminate the need for this class to be abstract
	public abstract BundleSerializer getBundleSerializer();
	
	public SharedPreferences getSystemPrefs() {
		return async(getSharedPreferences(getClass().getName() + "_system", MODE_PRIVATE));
	}
	
	public FunfConfig getConfig() {
		return getConfig(this, getClass().getName() + "_config");
	}
	
	protected static FunfConfig getConfig(Context context, String name) {
		SharedPreferences prefs = context.getSharedPreferences(name, MODE_PRIVATE);
		return FunfConfig.getInstance(async(prefs));
	}
	
	/**
	 * Used for testing the configuration before loading it into the actual config
	 * @return
	 */
	protected FunfConfig getTemporaryConfig() {
		SharedPreferences prefs = getSharedPreferences(getClass().getName() + "_tempconfig", MODE_PRIVATE);
		return FunfConfig.getInstance(async(prefs));
	}
	
	@Override
	protected void onEndOfQueue() {
		// nothing
	}
	
	/**
	 * Binder interface to the probe
	 */
	public class LocalBinder extends Binder {
		public ConfiguredPipeline getService() {
            return ConfiguredPipeline.this;
        }
    }
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
