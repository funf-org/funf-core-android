package edu.mit.media.hd.funf.storage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.FunfConfig;
import edu.mit.media.hd.funf.storage.DatabaseService.LocalBinder;

public class ArchiverService extends Service {
	public static final String TAG = ArchiverService.class.getName();
	private Class<? extends DatabaseService> dbServiceClass;
	
	public ArchiverService(final Class<? extends DatabaseService> dbServiceClass) {
		this.dbServiceClass = dbServiceClass;
	}
	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
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
		Log.i(TAG, "Succesfully started = " + result);
		FunfConfig config = FunfConfig.getFunfConfig(this);
		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		Intent archiveIntent = new Intent(this, getClass());
		PendingIntent pi = PendingIntent.getService(this, 0, archiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR * 3, pi);
		return Service.START_STICKY;
	}
	
	
}
