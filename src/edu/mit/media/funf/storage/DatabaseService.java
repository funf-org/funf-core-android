/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.storage;

import static edu.mit.media.funf.Utils.TAG;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.app.IntentService;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Simple database service that is able to write timestamp, name, value tuples.
 * 
 * There is a single separate thread responsible for all writes and maintenance, 
 * so there is no need to deal with synchronization.  
 * 
 * This class can be subclassed to provide custom implementations of Archive and 
 * SQliteHelper.Subclasses have no access to data queue.
 * @author alangardner
 *
 */
public abstract class DatabaseService extends IntentService {

	
	public static final String
		ACTION_RECORD = "edu.mit.media.funf.RECORD",
		ACTION_ARCHIVE = "edu.mit.media.funf.ARCHIVE";
	
	public static final String DATABASE_NAME_KEY = "DATABASE_NAME";
	
	private Map<String, SQLiteOpenHelper> databaseHelpers; // Cache open database helpers

	public DatabaseService() {
		super("FunfDatabaseService"); // Name only used for debugging
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (databaseHelpers != null) {
			for (SQLiteOpenHelper dbHelper : databaseHelpers.values()) {
				dbHelper.close();
			}
		}
		Log.i(TAG, "Destroyed");
	}

	@Override
	public void onHandleIntent(Intent intent) {
		Log.i(TAG, "Started");
		final String databaseName = intent.getStringExtra(DATABASE_NAME_KEY);
		if (databaseName == null) {
			Log.e(TAG, "Database name not specified.");
			return;
		}
		String action = intent.getAction();
		if (action == null || action.equals(ACTION_RECORD)) {
			writeToDatabase(databaseName, intent);
		} else if (action.equals(ACTION_ARCHIVE)) {
			runArchive(databaseName);
		}
	}
	
	/**
	 * Returns the file archive for the database.
	 * @param databaseName
	 * @return
	 */
	public Archive<File> getArchive(String databaseName) {
		return DefaultArchive.getArchive(this, databaseName);
	}
	
	/**
	 * The NameValueDatabaseHelper
	 * @param name
	 * @return
	 */
	protected abstract SQLiteOpenHelper getDatabaseHelper(String name);
	
	/**
	 * Pulls data from the intent to update the database.  This method is called from within a transaction.
	 * Throw a SQLException to signify a failure and rollback the transaction.
	 * @param db
	 * @param intent
	 */
	protected abstract void updateDatabase(SQLiteDatabase db, Intent intent) throws SQLException;
	
	
	private void runArchive(String databaseName) {
		SQLiteOpenHelper dbHelper = getOrCreateDatabaseHelper(databaseName);
		File dbFile = new File(dbHelper.getReadableDatabase().getPath());
		Log.i(TAG, "Running archive: " + dbFile.getAbsolutePath());
		dbHelper.close();
		Archive<File> archive = getArchive(databaseName);
		if (archive.add(dbFile)) {
			dbFile.delete();
		}
	}
	
	private SQLiteOpenHelper getOrCreateDatabaseHelper(String databaseName) {
		if (databaseHelpers == null) {
			databaseHelpers = new HashMap<String, SQLiteOpenHelper>();
		}
		SQLiteOpenHelper dbHelper = databaseHelpers.get(databaseName);
		if (dbHelper == null) {
			Log.i(TAG, "DataBaseService: Creating database '" + databaseName + "'");
			dbHelper = getDatabaseHelper(databaseName);
			databaseHelpers.put(databaseName, dbHelper);
		}
		return dbHelper;
	}
	
	
	/**
	 * Write data to the database in a transaction
	 * NOTE: Should only be called by one thread at a time (the writeThread)
	 * @param datum
	 */
	private void writeToDatabase(String databaseName, Intent intent) {
		Log.i(TAG, "Writing to database");
		SQLiteOpenHelper dbHelper = getOrCreateDatabaseHelper(databaseName);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.beginTransaction();
			updateDatabase(db, intent);
			db.setTransactionSuccessful();
			Log.i(TAG, "Writing successful");
		} catch (Exception e) {
			Log.e(TAG, "DataBaseService: save error",e);
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	/**
	 * Binder interface to the probe
	 */
	public class LocalBinder extends Binder {
		public DatabaseService getService() {
            return DatabaseService.this;
        }
    }
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
