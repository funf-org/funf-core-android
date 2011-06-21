package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.hd.funf.FunfConfig;
import edu.mit.media.hd.funf.ProbeDatabaseConfig;

/**
 * Simple database service that is able to write timestamp, name, value tuples.  
 * This class is abstract to enforce the user to specify a name for the database in the constructor.
 * 
 * There is a single separate thread responsible for all writes and maintenance, 
 * so there is no need to deal with synchronization.  
 * Subclasses have no access to data queue, except through defined methods.
 * @author alangardner
 *
 */
public class DatabaseService extends Service {

	public static final String TAG = DatabaseService.class.getName();
	public static final String DATABASE_NAME_KEY = "DATABASE_NAME";
	public static final String NAME_KEY = "NAME";
	public static final String VALUE_KEY = "VALUE";
	public static final String TIMESTAMP_KEY = "TIMESTAMP";
	
	private Thread writeThread;
	private LinkedBlockingQueue<Message> dataQueue;
	private Map<String, DatabaseHelper> databaseHelpers;
	
	@Override
	public void onCreate() {
		Log.i(TAG, "Creating");
		dataQueue = new LinkedBlockingQueue<Message>();
		runReloadConfig();
		writeThread = new Thread(new Runnable(){
			public void run() {
				while(true) {
					try{
		    			Log.i(TAG,"DatabaseService: to save one datum");
		    			Message message = dataQueue.take();
		    			if (message instanceof Datum) { 
		    				Datum d = (Datum)message;
		    				writeToDatabase(d);
		    			} else {
		    				// Handle messages
		    				if (Message.ARCHIVE.equals(message.name)) {
		    					runArchive(message.databaseName);
		    				} else if (Message.RELOAD.equals(message.name)) {
		    					runReloadConfig();
		    				} else if (Message.END.equals(message.name)) {
		    					break; // end loop, Signal to exit
		    				}
		    			}
		    		}catch(Exception e){
		    			Log.e(TAG, "DataBaseService: error in write", e);
		    		}
				}
			}	
	    });
		writeThread.setDaemon(true);
		writeThread.start();
		Log.i(TAG, "Created");
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "Rebinding");
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "Unbinding");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Destroying");
		dataQueue.offer(new Message(null, Message.END, null)); // Signal the end of the queue
		try {
			// Try waiting until the thread is finished
			writeThread.join(500);
		} catch (InterruptedException e) {
			// Otherwise clear the queue and stop the thread
			dataQueue.clear();
			writeThread.stop();
		}
		for (DatabaseHelper dbHelper : databaseHelpers.values()) {
			dbHelper.close();
		}
		Log.i(TAG, "Destroyed");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Started");
		final long timestamp = intent.getLongExtra(TIMESTAMP_KEY, 0L);
		final String databaseName = intent.getStringExtra(DATABASE_NAME_KEY);
		final String name = intent.getStringExtra(NAME_KEY);
		final String value = intent.getStringExtra(VALUE_KEY);
		save(databaseName, name, value, timestamp);
		stopSelf(startId); // TODO: may decide it is not worth stopping between writes, or set timeout
		return Service.START_STICKY;
	}

	/**
	 * Queue data up to be written to the database.  Returns immediately.
	 * @param timestamp
	 * @param name
	 * @param value
	 */
	public void save(String databaseName, final String name, final String value, long timestamp) {
		if (timestamp == 0L || name == null || value == null) {
			Log.e(TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + name + " - " + value);
		} else {
			Log.i(TAG, "Queing up data");
			dataQueue.offer(new Datum(databaseName, name, value, timestamp));
		}
	}
	
	public void archive() {
		for (String databaseName : databaseHelpers.keySet()) {
			archive(databaseName);
		}
	}
	
	public void archive(String databaseName) {
		Log.i(TAG, "Queing archive");
		dataQueue.offer(new Message(databaseName, Message.ARCHIVE, null));
	}
	
	protected Archive<File> getArchive(String databaseName) {
		return getDefaultArchive(this, databaseName);
	}
	
	public static Archive<File> getDefaultArchive(Context contex, String databaseName) {
		String rootSdCardPath = "/sdcard/" + contex.getPackageName() + "/" + databaseName + "/";
		Archive<File> backupArchive = FileDirectoryArchive.getRollingFileArchive(new File(rootSdCardPath + "backup"));
		Archive<File> mainArchive = new CompositeFileArchive(
				FileDirectoryArchive.getTimestampedFileArchive(new File(rootSdCardPath + "archive")),
				FileDirectoryArchive.getTimestampedFileArchive(contex.getDir("funf_" + databaseName + "_archive", Context.MODE_PRIVATE))
				);
		return new BackedUpArchive(mainArchive, backupArchive);
	}
	
	
	private void runArchive(String databaseName) {
		Log.i(TAG, "Running archive: " + getDatabasePath(databaseName));
		DatabaseHelper dbHelper = databaseHelpers.get(databaseName);
		dbHelper.close();
		Archive<File> archive = getArchive(databaseName);
		if (archive.add(getDatabasePath(databaseName))) {
			getDatabasePath(databaseName).delete();
		}
		dbHelper = new DatabaseHelper(this, databaseName, 1);
	}
	
	public void reloadConfig() {
		Log.i(TAG, "Queing reload");
		dataQueue.offer(new Message(null, Message.RELOAD, null));
	}
	
	private void runReloadConfig() {
		Log.i(TAG, "Reloading config");
		for (DatabaseHelper dbHelper : databaseHelpers.values()) {
			dbHelper.close();
		}
		databaseHelpers = new HashMap<String, DatabaseHelper>();
		FunfConfig config = FunfConfig.getFunfConfig(this);
		if (config == null) {
			stopSelf();
		} else {
			for (Map.Entry<String,ProbeDatabaseConfig> dbEntry : config.getDatabases().entrySet()) {
				String dbName = dbEntry.getKey();
				databaseHelpers.put(dbName, new DatabaseHelper(this, dbName, 1));
			}
		}
	}
	
	
	/**
	 * Write data to the database in a transaction
	 * NOTE: Should only be called by one thread at a time (the writeThread)
	 * @param datum
	 */
	private void writeToDatabase(Datum datum) {
		Log.i(TAG, "Writing to database");
		DatabaseHelper dbHelper = databaseHelpers.get(datum.databaseName);
		if (dbHelper == null) {
			Log.e(TAG, "DataBaseService: no database with name '" + datum.databaseName + "'");
			return;
		}
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.beginTransaction();
			ContentValues cv = new ContentValues();
			cv.put(DatabaseHelper.COLUMN_NAME, datum.name);
			cv.put(DatabaseHelper.COLUMN_TIMESTAMP, datum.timestamp);
			cv.put(DatabaseHelper.COLUMN_VALUE, datum.value);
			db.insertOrThrow(DatabaseHelper.DATA_TABLE.name, "", cv);
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
	 * Represents a bit a timestamp, name, value tuple to be written to the database
	 *
	 */
	private static class Datum extends Message {
		public final long timestamp;
		public Datum(final String databaseName, final String name, final String value, long timestamp) {
			super(databaseName, name, value);
			this.timestamp = timestamp;
		}
	}
	
	/**
	 * Special datum used to represent actions the db service should take
	 * TODO: could create a separate queue for messages, and not reuse this class
	 */
	private static class Message {
		public static final String ARCHIVE = "__ARCHIVE__";
		public static final String RELOAD = "__RELOAD__";
		public static final String END = "__END__";
		public final String databaseName, name, value;
		public Message(String databaseName, String name, String value) {
			this.databaseName = databaseName;
			this.name = name;
			this.value = value;
		}
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
