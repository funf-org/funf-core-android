package edu.mit.media.hd.funf.configured;

import java.io.File;
import java.util.Map;
import java.util.Set;

import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.storage.Archive;
import edu.mit.media.hd.funf.storage.HttpArchive;
import edu.mit.media.hd.funf.storage.RemoteArchive;
import edu.mit.media.hd.funf.storage.RemoteArchiverService;


/**
 * Uses the database config in the Funf Config to run remote archiving
 */
public class ConfiguredRemoteArchiverService extends RemoteArchiverService {
	public static final String TAG = ConfiguredRemoteArchiverService.class.getName();
	
	private Map<String, ProbeDatabaseConfig> databases;
	
	@Override
	public void onCreate() {
		super.onCreate(); // Must call to set up parent class
		FunfConfig config = FunfConfig.getFunfConfig(this);
		if (config == null) {
			stopSelf();
		} else {
			databases = FunfConfig.getFunfConfig(this).getDatabases();
		}
	}

	protected Archive<File> getFileArchive(final String databaseName) {
		return ConfiguredDatabaseService.getDefaultArchive(this, databaseName);
	}

	protected RemoteArchive getRemoteArchiver(final String databaseName) {
		ProbeDatabaseConfig dbConfig = databases.get(databaseName);
		return new HttpArchive(dbConfig.getUploadUrl());// TODO: how do i get the upload url here
	}

	@Override
	protected Set<String> getDatabaseNames() {
		return databases.keySet();
	}

	@Override
	protected void scheduleNextRun() {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		AndroidUtils.configureAlarm(this, getClass(), config.getRemoteArchivePeriod());
	}
	
	
}
