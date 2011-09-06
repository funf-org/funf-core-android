package edu.mit.media.hd.funf.configured;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.storage.BundleSerializer;
import edu.mit.media.hd.funf.storage.NameValueProbeDataListener;

/**
 * Reads the database configuration to determine which data streams to save to which databases
 */
public abstract class ConfiguredDataListenerService extends Service {

	private static final String TAG = ConfiguredDataListenerService.class.getName();
	
	private List<NameValueProbeDataListener> dataListeners;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (dataListeners != null) {
			for (NameValueProbeDataListener dataListener : dataListeners) {
				unregisterReceiver(dataListener);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Start called");
		if (dataListeners == null) {
			Log.i(TAG, "Creating data listeners");
			dataListeners = new ArrayList<NameValueProbeDataListener>();
			FunfConfig config = FunfConfig.getFunfConfig(this);
			if (config == null) {
				Log.i(TAG, "No Config");
				stopSelf();
			} else {
				Map<String,ProbeDatabaseConfig> databases = config.getDatabases();
				for (Map.Entry<String, ProbeDatabaseConfig> entry : databases.entrySet()) {
					String databaseName = entry.getKey();
					Log.i(TAG, "Configuring database: " + databaseName);
					String[] probesToRecord = entry.getValue().getProbesToRecord();
					NameValueProbeDataListener dataListener = getDataListener(databaseName);
					IntentFilter filter = new IntentFilter();
					Log.i(TAG, "Listening for probes: " + Utils.join(Arrays.asList(probesToRecord), ", ") );
					for (String probe : probesToRecord) {
						filter.addAction(OppProbe.getDataAction(probe));
					}
					registerReceiver(dataListener, filter);
				}
			}
		}
		return START_STICKY;
	}
	
	protected NameValueProbeDataListener getDataListener(String databaseName) {
		return new NameValueProbeDataListener(databaseName, ConfiguredDatabaseService.class, getBundleSerializer());
	}
	
	protected abstract BundleSerializer getBundleSerializer();



	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	

}
