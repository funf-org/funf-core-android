package edu.mit.media.hd.funf.configured;

import android.content.Intent;
import android.os.IBinder;
import edu.mit.media.hd.funf.AndroidUtils;
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

	
	protected void scheduleNextRun() {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		long archivePeriod = (config == null) ? 1 * 60 * 60 * 1000 : config.getArchivePeriod();
		AndroidUtils.configureAlarm(this, getClass(), archivePeriod);
	}


	@Override
	protected Class<? extends DatabaseService> getDatabaseServiceClass() {
		return ConfiguredDatabaseService.class;
	}
	
	
}
