package edu.mit.media.hd.funf.configured;

import org.json.JSONException;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.IOUtils;
import edu.mit.media.hd.funf.storage.DatabaseService;

public abstract class ConfigurationUpdaterService extends Service {
	public static final String TAG = ConfigurationUpdaterService.class.getName();
	
	@Override
	public IBinder onBind(Intent bindIntent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO: detect whether wifi is available, etc.
		try {
			String configJson = IOUtils.httpGet(getRemoteConfigUrl(), null);
			if (configJson == null) {
				Log.e(TAG, "Unable to download config");
			} else {
				FunfConfig config = new FunfConfig(IOUtils.httpGet(getRemoteConfigUrl(), null));
				FunfConfig.setFunfConfig(this, config);
				// TODO: only reload on different configuration
				Intent dbIntent = new Intent(this, getDatabaseServiceClass());
				bindService(dbIntent, new ServiceConnection() {
					@Override
					public void onServiceConnected(ComponentName name, IBinder service) {
						DatabaseService dbService = ((DatabaseService.LocalBinder)service).getService();
						dbService.reload();
						unbindService(this);
					}
					@Override
					public void onServiceDisconnected(ComponentName name) {
					}
				}, BIND_AUTO_CREATE);
			}
		} catch (JSONException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		scheduleNextRun();
		stopSelf();
		return START_STICKY;
	}
	
	protected void scheduleNextRun() {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		long updatePeriod = (config == null) ? 1 * 60 * 60 * 1000 : config.getUpdatePeriod();
		AndroidUtils.configureAlarm(this, getClass(), updatePeriod);
	}

	protected Class<? extends DatabaseService> getDatabaseServiceClass() {
		return ConfiguredDatabaseService.class;
	}
	
	protected abstract String getRemoteConfigUrl();

}
