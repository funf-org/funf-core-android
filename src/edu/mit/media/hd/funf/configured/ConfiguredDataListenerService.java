package edu.mit.media.hd.funf.configured;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import edu.mit.media.hd.funf.probe.Utils;
import edu.mit.media.hd.funf.storage.BundleSerializer;
import edu.mit.media.hd.funf.storage.ProbeDataListener;

/**
 * Reads the database configuration to determine which data streams to save to which databases
 */
public abstract class ConfiguredDataListenerService extends Service {

	private List<ProbeDataListener> dataListeners;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (dataListeners != null) {
			for (ProbeDataListener dataListener : dataListeners) {
				unregisterReceiver(dataListener);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (dataListeners == null) {
			dataListeners = new ArrayList<ProbeDataListener>();
			FunfConfig config = FunfConfig.getFunfConfig(this);
			if (config == null) {
				stopSelf();
			} else {
				Map<String,ProbeDatabaseConfig> databases = config.getDatabases();
				for (Map.Entry<String, ProbeDatabaseConfig> entry : databases.entrySet()) {
					String databaseName = entry.getKey();
					String[] probesToRecord = entry.getValue().getProbesToRecord();
					ProbeDataListener dataListener = getDataListener(databaseName);
					IntentFilter filter = new IntentFilter();
					for (String probe : probesToRecord) {
						filter.addAction(Utils.getDataAction(probe));
					}
					registerReceiver(dataListener, filter);
				}
			}
		}
		return START_STICKY;
	}
	
	protected ProbeDataListener getDataListener(String databaseName) {
		return new ProbeDataListener(databaseName, ConfiguredDatabaseService.class, getBundleSerializer());
	}
	
	protected abstract BundleSerializer getBundleSerializer();



	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	

}
