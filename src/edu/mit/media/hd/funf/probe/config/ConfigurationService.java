package edu.mit.media.hd.funf.probe.config;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Convenience service to mass configure probes using a FNFConiguration object
 *
 */
public class ConfigurationService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO: parse json configuration
		// Build Funf configuration
		// for each database item configure database writer data listener
		// for each probe request
		return super.onStartCommand(intent, flags, startId);
	}

	
}
