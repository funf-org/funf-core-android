package edu.mit.media.hd.funf.storage;

import android.app.AlarmManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.storage.DatabaseService.LocalBinder;

public abstract class ArchiverService extends Service {
	public static final String TAG = ArchiverService.class.getName();
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Class<? extends DatabaseService> dbServiceClass = getDatabaseServiceClass();
		Log.i(TAG, "Starting w/ " + dbServiceClass.getName() + "...");
		boolean result = bindService(new Intent(this, dbServiceClass), 
				new ServiceConnection() {
					@Override
					public void onServiceConnected(ComponentName name, IBinder service) {
						Log.i(TAG, "Service connected...");
						((LocalBinder)service).getService().archive();
						unbindService(this);
						stopSelf();
					}
					@Override
					public void onServiceDisconnected(ComponentName name) {
						Log.i(TAG, "Service disconnected...");
					}
					
				}, BIND_AUTO_CREATE);
		scheduleNextRun();
		Log.i(TAG, "Succesfully started = " + result);
		return Service.START_STICKY;
	}
	
	protected abstract Class<? extends DatabaseService> getDatabaseServiceClass();
	
	protected void scheduleNextRun() {
		AndroidUtils.configureAlarm(this, getClass(), 3 * AlarmManager.INTERVAL_HOUR);
	}
	
	
}
