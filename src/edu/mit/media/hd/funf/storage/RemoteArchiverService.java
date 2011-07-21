package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import edu.mit.media.hd.funf.AndroidUtils;
import edu.mit.media.hd.funf.EqualsUtil;
import edu.mit.media.hd.funf.HashCodeUtil;
import edu.mit.media.hd.funf.Utils;


/**
 * Archives the file when started
 *
 */
public abstract class RemoteArchiverService extends Service {
	public static final String TAG = RemoteArchiverService.class.getName();
	
	private Map<String, Integer> fileFailures;
	private Queue<DatabaseFile> filesToUpload;
	private Thread uploadThread;
	private WakeLock lock;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;  // No binding for now
	}
	
	/**
	 * Convenience class for pairing the database name with the db file
	 */
	protected class DatabaseFile {
		public final String databaseName;
		public final File file;
		public DatabaseFile(String databaseName, File databaseFile) {
			this.databaseName = databaseName;
			this.file = databaseFile;
		}
		@Override
		public boolean equals(Object o) {
			return o != null && o instanceof DatabaseFile 
				&& EqualsUtil.areEqual(databaseName, ((DatabaseFile)o).databaseName)
				&& EqualsUtil.areEqual(file, ((DatabaseFile)o).file);
		}
		@Override
		public int hashCode() {
			return HashCodeUtil.hash(HashCodeUtil.hash(HashCodeUtil.SEED, file), databaseName);
		}
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG, "Creating...");
		lock = Utils.getWakeLock(this);
		fileFailures = new HashMap<String, Integer>();
		filesToUpload = new ConcurrentLinkedQueue<DatabaseFile>();
		// TODO: consider multiple upload threads
		uploadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(Thread.currentThread().equals(uploadThread) && !filesToUpload.isEmpty()) {
					DatabaseFile dbFile = filesToUpload.poll();
					runArchive(dbFile.databaseName, dbFile.file);
				}
				uploadThread = null;
				stopSelf();
			}
		});
	}
	@Override
	public void onDestroy() {
		if (uploadThread != null && uploadThread.isAlive()) {
			uploadThread = null;
		}
		if (lock.isHeld()) {
			lock.release();
		}
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Starting...");
		// Get all db files in DB dir
		Set<String> databaseNames = getDatabaseNames();
		for (String databaseName : databaseNames) {
			for (File file : getFileArchive(databaseName).getAll()) {
				archive(databaseName, file);
			}
		}
		
		// Start upload thread if necessary
		if (uploadThread != null && !uploadThread.isAlive()) {
			uploadThread.start();
		}
		scheduleNextRun();
		return Service.START_STICKY;
	}
	
	
	public void archive(String databaseName, File file) {
		DatabaseFile dbFile = new DatabaseFile(databaseName, file);
		if (!filesToUpload.contains(dbFile)) {
			Log.i(TAG, "Queuing " + databaseName + " archive..." + file.getName());
			filesToUpload.offer(dbFile);
		}
	}

	protected void runArchive(String databaseName, File file) {
		Log.i(TAG, "Archiving..." + file.getName());
		Archive<File> archive = getFileArchive(databaseName);
		RemoteArchive remoteArchive = getRemoteArchiver(databaseName);
		if(remoteArchive.add(file)) {
			archive.remove(file);
		} else {
			Integer numFailures = fileFailures.get(file.getName());
			numFailures = (numFailures == null) ? 1 : numFailures + 1;
			fileFailures.put(file.getName(), numFailures);
			// 3 Attempts
			if (numFailures < 3) {
				filesToUpload.offer(new DatabaseFile(databaseName, file));
			} else {
				Log.e(TAG, "Failed to upload '" + file.getAbsolutePath() + "' after 3 attempts.");
			}
		}
		// TODO: Multiple web failure: queue up for one hour from now, and stop self
	}
	

	protected abstract Set<String> getDatabaseNames();
	
	protected abstract Archive<File> getFileArchive(final String databaseName);

	protected abstract RemoteArchive getRemoteArchiver(final String databaseName);
	
	protected void scheduleNextRun() {
		AndroidUtils.configureAlarm(this, getClass(), 6 * AlarmManager.INTERVAL_HOUR);
	}
	
}
