package edu.mit.media.hd.funf.configured;

import android.content.Intent;
import android.os.IBinder;
import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.storage.ArchiverService;
import edu.mit.media.hd.funf.storage.DatabaseService;

/**
 * Archiver service that uses the ConfiguredDatabasService and the Funf config to schedule
 */
public class ConfiguredArchiverService extends ArchiverService {
	public static final String TAG = ConfiguredArchiverService.class.getName();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private static final long DEFAULT_PERIOD = 1 * 60 * 60; // every hour
	protected void scheduleNextRun() {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		long archivePeriod = Utils.secondsToMillis((config == null) ? DEFAULT_PERIOD : config.getArchivePeriod());
		AndroidUtils.configureAlarm(this, getClass(), archivePeriod);
	}


	@Override
	protected Class<? extends DatabaseService> getDatabaseServiceClass() {
		return ConfiguredDatabaseService.class;
	}
	
	
}
