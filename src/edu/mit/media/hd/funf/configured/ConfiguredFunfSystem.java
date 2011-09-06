package edu.mit.media.hd.funf.configured;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.FileUtils;
import edu.mit.media.hd.funf.IOUtils;
import edu.mit.media.hd.funf.storage.BundleSerializer;
import edu.mit.media.hd.funf.storage.DatabaseService;
import edu.mit.media.hd.funf.storage.HttpUploadService;
import edu.mit.media.hd.funf.storage.NameValueDatabaseService;
import edu.mit.media.hd.funf.storage.NameValueProbeDataListener;
import edu.mit.media.hd.funf.storage.UploadService;

public abstract class ConfiguredFunfSystem extends IntentService {

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
	
	private FunfConfig config;

	public ConfiguredFunfSystem() {
		super("ConfiguredFunfSystem");
		setIntentRedelivery(true);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null || action.equals(ACTION_RELOAD)) {
			ensureServicesAreRunning();
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
			enable();
		} else if(action.equals(ACTION_DISABLE)) {
			disable();
		}
	}
	
	public void ensureServicesAreRunning() {
		// TODO: implement
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
			getConfig().edit().setAll(jsonString).commit();
		} catch (JSONException e) {
			Log.e(TAG, "Unable to update configuration.", e);
		}
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
	
	public void enable() {
		// TODO: implement
	}

	public void disable() {
		// TODO: implement
	}
	
	public Class<? extends DatabaseService> getDatabaseServiceClass() {
		return NameValueDatabaseService.class;
	}
	
	public Class<? extends UploadService> getUploadServiceClass() {
		return HttpUploadService.class;
	}
	
	public BroadcastReceiver getProbeDataListener() {
		return new NameValueProbeDataListener(config.getName(), getDatabaseServiceClass(), getBundleSerializer());
	}
	
	// TODO: enable json serialization of bundles to eliminate the need for this class to be abstract
	public abstract BundleSerializer getBundleSerializer();
	
	public FunfConfig getConfig() {
		SharedPreferences prefs = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
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
