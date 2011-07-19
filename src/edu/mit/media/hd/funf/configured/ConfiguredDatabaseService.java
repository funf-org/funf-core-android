package edu.mit.media.hd.funf.configured;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.mit.media.hd.funf.storage.Archive;
import edu.mit.media.hd.funf.storage.DatabaseHelper;
import edu.mit.media.hd.funf.storage.DatabaseService;

/**
 * Database service which uses the FunfConfig available
 *
 */
public class ConfiguredDatabaseService extends DatabaseService {

	public static final String TAG = ConfiguredDatabaseService.class.getName();
	
	
	protected Map<String, DatabaseHelper> getDatabaseHelpers() {
		Map<String,DatabaseHelper> databaseHelpers = new HashMap<String, DatabaseHelper>();
		FunfConfig config = FunfConfig.getFunfConfig(this);
		if (config == null) {
			stopSelf();
		} else {
			for (Map.Entry<String,ProbeDatabaseConfig> dbEntry : config.getDatabases().entrySet()) {
				String dbName = dbEntry.getKey();
				databaseHelpers.put(dbName, new DatabaseHelper(this, dbName, 1));
			}
		}
		return databaseHelpers;
	}
	
	protected Archive<File> getArchive(String databaseName) {
		FunfConfig config = FunfConfig.getFunfConfig(this);
		return getDefaultArchive(this, databaseName, config.getEncryptionKey());
	}
	
}
