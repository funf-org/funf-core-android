package edu.mit.media.hd.funf.configured;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONException;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.FileUtils;
import edu.mit.media.hd.funf.IOUtils;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.client.ProbeCommunicator;
import edu.mit.media.hd.funf.probe.Probe;
import edu.mit.media.hd.funf.probe.ProbeUtils;
import edu.mit.media.hd.funf.storage.BundleSerializer;
import edu.mit.media.hd.funf.storage.DatabaseService;
import edu.mit.media.hd.funf.storage.HttpUploadService;
import edu.mit.media.hd.funf.storage.NameValueDatabaseService;
import edu.mit.media.hd.funf.storage.NameValueProbeDataListener;
import edu.mit.media.hd.funf.storage.UploadService;

public abstract class ConfiguredFunfSystem extends IntentService implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Funf";
	
	private static final String PREFIX = "edu.mit.media.funf.";
	public static final String
	ACTION_RELOAD = PREFIX + "reload",
	ACTION_UPDATE_CONFIG = PREFIX + "update",
	ACTION_UPLOAD_DATA = PREFIX + "upload",
	ACTION_ARCHIVE_DATA = PREFIX + "archive",
	ACTION_ENABLE = PREFIX + "enable",
	ACTION_DISABLE = PREFIX + "disable";
	
	public static final String
	CONFIG = "config",
	CONFIG_URL = "config_url",
	CONFIG_FILE = "config_file";

	private NameValueProbeDataListener dataListener;
	private Handler handler;

	public ConfiguredFunfSystem() {
		super("ConfiguredFunfSystem");
		setIntentRedelivery(true);
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
		unregisterListeners();
		getConfig().getPrefs().unregisterOnSharedPreferenceChangeListener(this);
		getSystemPrefs().unregisterOnSharedPreferenceChangeListener(this);
	}



	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null) {
			ensureServicesAreRunning();
		} else if (action.equals(ACTION_RELOAD)) {
			reload();
		} else if(action.equals(ACTION_UPDATE_CONFIG)) {
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
		} else if(action.equals(ACTION_UPLOAD_DATA)) {
			uploadData();
		} else if(action.equals(ACTION_ARCHIVE_DATA)) {
			archiveData();
		} else if(action.equals(ACTION_ENABLE)) {
			setEnabled(true);
		} else if(action.equals(ACTION_DISABLE)) {
			setEnabled(false);
		}
	}

	private static final String ENABLED_KEY = "enabled";
	public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
		if (sharedPreferences.equals(getConfig().getPrefs())) {
			onConfigChange(getConfig().toString(true));
		}
		reload();
	}
	
	public void reload() {
		boolean enabled = getSystemPrefs().getBoolean(ENABLED_KEY, true);
		cancelAlarms();
		unregisterListeners();
		removeProbeRequests();
		if (enabled) {
			// Schedule this for the future to prevent race conditions
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					ensureServicesAreRunning();
				}
			}, 5000);
		}
	}
	
	private void scheduleAlarms() {
		FunfConfig config = getConfig();
		scheduleAlarm(ACTION_UPDATE_CONFIG, config.getConfigUpdatePeriod());
		scheduleAlarm(ACTION_ARCHIVE_DATA, config.getDataArchivePeriod());
		scheduleAlarm(ACTION_UPLOAD_DATA, config.getDataUploadPeriod());
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
		}
	}
	
	public void ensureServicesAreRunning() {
		scheduleAlarms();
		registerListeners();
		sendProbeRequests();
	}
	
	private void sendProbeRequests() {
		String requestId = getConfig().getName();
		for (Entry<String, Bundle[]> entry : getConfig().getDataRequests().entrySet()) {
			String probeName = entry.getKey();
			Bundle[] probeRequests = entry.getValue();
			ProbeCommunicator probe = new ProbeCommunicator(this, probeName);
			probe.registerDataRequest(requestId, probeRequests);
		}
	}
	
	private void removeProbeRequests() {
		// TODO: Use a more general approach to remove data registration in all probes in all packages
		Log.w(TAG, "Only removing requests for probes that are registered in this package.");
		String requestId = getConfig().getName();
		for (Class<? extends Probe> probeClass : ProbeUtils.getAvailableProbeClasses(this)) {
			ProbeCommunicator probe = new ProbeCommunicator(this, probeClass);
			probe.unregisterDataRequest(requestId);
		}
	}
	
	private synchronized void registerListeners() {
		if (dataListener == null) {
			Log.i(TAG, "Creating data listeners");
			FunfConfig config = getConfig();
			if (config == null) {
				Log.i(TAG, "No Config");
				return;
			}
			dataListener = new NameValueProbeDataListener(config.getName(), getDatabaseServiceClass(), getBundleSerializer());
			Set<String> probes = config.getDataRequests().keySet();
			IntentFilter filter = new IntentFilter();
			for (String probe : probes) {
				filter.addAction(OppProbe.getDataAction(probe));
			}
			registerReceiver(dataListener, filter);
		}
	}
	
	private synchronized void unregisterListeners() {
		if (dataListener != null) {
			unregisterReceiver(dataListener);
			dataListener = null;
		}
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
			// Write to temporary to compare
			FunfConfig tempConfig = getTemporaryConfig();
			boolean successfullyWroteConfig = tempConfig.edit().setAll(jsonString).commit();
			if (successfullyWroteConfig	&& !tempConfig.equals(getConfig())) {
				getConfig().edit().setAll(jsonString).commit();
			}
		} catch (JSONException e) {
			Log.e(TAG, "Unable to update configuration.", e);
		}
	}
	
	protected void onConfigChange(String json) {
		// Record configuration change to database
		Intent i = new Intent(this, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_RECORD);
		i.putExtra(NameValueDatabaseService.NAME_KEY, getClass().getName());
		i.putExtra(NameValueDatabaseService.VALUE_KEY, json);
		startService(i);
	}
	
	public void uploadData() {
		String archiveName = getConfig().getName();
		String uploadUrl = getConfig().getDataUploadUrl();
		Intent i = new Intent(this, getUploadServiceClass());
		i.putExtra(UploadService.ARCHIVE_ID, archiveName);
		i.putExtra(UploadService.REMOTE_ARCHIVE_ID, uploadUrl);
	}
	
	public void archiveData() {
		Intent i = new Intent(this, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_ARCHIVE);
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
		return new NameValueProbeDataListener(getConfig().getName(), getDatabaseServiceClass(), getBundleSerializer());
	}
	
	// TODO: enable json serialization of bundles to eliminate the need for this class to be abstract
	public abstract BundleSerializer getBundleSerializer();
	
	public SharedPreferences getSystemPrefs() {
		return getSharedPreferences(getClass().getName() + "_system", MODE_PRIVATE);
	}
	
	public FunfConfig getConfig() {
		return getConfig(this, getClass().getName() + "_config");
	}
	
	protected static FunfConfig getConfig(Context context, String name) {
		SharedPreferences prefs = context.getSharedPreferences(name, MODE_PRIVATE);
		return new FunfConfig(prefs);
	}
	
	/**
	 * Used for testing the configuration before loading it into the actual config
	 * @return
	 */
	protected FunfConfig getTemporaryConfig() {
		SharedPreferences prefs = getSharedPreferences(getClass().getName() + "_tempconfig", MODE_PRIVATE);
		return new FunfConfig(prefs);
	}
	
	/**
	 * Binder interface to the probe
	 */
	public class LocalBinder extends Binder {
		public ConfiguredFunfSystem getService() {
            return ConfiguredFunfSystem.this;
        }
    }
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
