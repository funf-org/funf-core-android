package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
public abstract class DatabaseService extends Service {

	public static final String TAG = DatabaseService.class.getName();
	public static final String TIMESTAMP_KEY = "TIMESTAMP";
	public static final String NAME_KEY = "NAME";
	public static final String VALUE_KEY = "VALUE";
	
	private Thread writeThread;
	private LinkedBlockingQueue<Message> dataQueue;
	private DatabaseHelper dbHelper;
	private String databaseName;
	private Archive<File> archive;
	
	
	protected abstract String getDatabaseName();
	protected abstract Archive<File> getArchive();
	
	@Override
	public void onCreate() {
		Log.i(TAG, "Creating");
		this.databaseName = getDatabaseName();
		this.archive = getArchive();
		dataQueue = new LinkedBlockingQueue<Message>();
		dbHelper = new DatabaseHelper(this, databaseName, 1);
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
		    					runArchive();
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
		dataQueue.offer(new Message(Message.END, null)); // Signal the end of the queue
		try {
			// Try waiting until the thread is finished
			writeThread.join(500);
		} catch (InterruptedException e) {
			// Otherwise clear the queue and stop the thread
			dataQueue.clear();
			writeThread.stop();
		}
		dbHelper.close();
		Log.i(TAG, "Destroyed");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Started");
		final long timestamp = intent.getLongExtra(TIMESTAMP_KEY, 0L);
		final String name = intent.getStringExtra(NAME_KEY);
		final String value = intent.getStringExtra(VALUE_KEY);
		save(timestamp, name, value);
		stopSelf(startId); // TODO: may decide it is not worth stopping between writes, or set timeout
		return Service.START_STICKY;
	}

	/**
	 * Queue data up to be written to the database.  Returns immediately.
	 * @param timestamp
	 * @param name
	 * @param value
	 */
	public void save(long timestamp, final String name, final String value) {
		if (timestamp == 0L || name == null || value == null) {
			Log.e(TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + name + " - " + value);
		} else {
			Log.i(TAG, "Queing up data");
			dataQueue.offer(new Datum(timestamp, name, value));
		}
	}
	
	public void archive() {
		Log.i(TAG, "Queing archive");
		dataQueue.offer(new Message(Message.ARCHIVE, null));
	}
	
	private void runArchive() {
		Log.i(TAG, "Running archive: " + getDatabasePath(databaseName));
		dbHelper.close();
		if (archive.add(getDatabasePath(databaseName))) {
			getDatabasePath(databaseName).delete();
		}
		dbHelper = new DatabaseHelper(this, databaseName, 1);
	}
	
	
	/**
	 * Write data to the database in a transaction
	 * NOTE: Should only be called by one thread at a time (the writeThread)
	 * @param datum
	 */
	private void writeToDatabase(Datum datum) {
		Log.i(TAG, "Writing to database");
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
		public Datum(long timestamp, final String name, final String value) {
			super(name, value);
			this.timestamp = timestamp;
		}
	}
	
	/**
	 * Special datum used to represent actions the db service should take
	 * TODO: could create a separate queue for messages, and not reuse this class
	 */
	private static class Message {
		public static final String ARCHIVE = "__ARCHIVE__";
		public static final String END = "__END__";
		public final String name, value;
		public Message(String name, String value) {
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
