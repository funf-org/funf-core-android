package edu.mit.media.hd.funf.configured;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.Utils;
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
	private byte[] encryptionKey;
	
	@Override
	public void onCreate() {
		super.onCreate(); // Must call to set up parent class
		FunfConfig config = FunfConfig.getFunfConfig(this);
		if (config == null) {
			stopSelf();
		} else {
			databases = FunfConfig.getFunfConfig(this).getDatabases();
			encryptionKey = config.getEncryptionKey();
		}
	}

	protected Archive<File> getFileArchive(final String databaseName) {
		return ConfiguredDatabaseService.getDefaultArchive(this, databaseName, encryptionKey);
	}

	protected RemoteArchive getRemoteArchiver(final String databaseName) {
		ProbeDatabaseConfig dbConfig = databases.get(databaseName);
		return new HttpArchive(dbConfig.getUploadUrl());// TODO: how do i get the upload url here
	}

	@Override
	protected Set<String> getDatabaseNames() {
		return (databases == null) ? new HashSet<String>() : databases.keySet();
	}

	private static final long DEFAULT_PERIOD = 6 * 60 * 60;
	
	@Override
	protected void scheduleNextRun() {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		long remoteArchivePeriod = Utils.secondsToMillis((config == null) ? DEFAULT_PERIOD : config.getRemoteArchivePeriod());
		AndroidUtils.configureAlarm(this, getClass(), remoteArchivePeriod);
	}
	
	
}
