/**
 * 
 * Funf: Open Sensing Framework Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Funf. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.storage;



import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.util.EqualsUtil;
import edu.mit.media.funf.util.HashCodeUtil;
import edu.mit.media.funf.util.LockUtil;
import edu.mit.media.funf.util.LogUtil;

public abstract class UploadService {

  @Configurable
  private boolean wifiOnly = false;
  
  @Configurable
  public int maxRemoteRetries = 6; 
  
  @Configurable
  public int maxFileRetries = 3;
  

  private Context ctx;
  private ConnectivityManager connectivityManager;
  private Map<String, Integer> fileFailures;
  private Map<String, Integer> remoteArchiveFailures;
  private Queue<ArchiveFile> filesToUpload;
  private Thread uploadThread;
  private WakeLock lock;
  private Handler mainHandler;
  private Runnable stopSelfTask = new Runnable() {
    
    @Override
    public void run() {
      onDestroy();
    }
  };

  public void onCreate(Context ctx) {
    this.ctx = ctx;
    mainHandler = new Handler();
    Log.i(LogUtil.TAG, "Creating...");
    connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    lock = LockUtil.getWakeLock(ctx);
    fileFailures = new HashMap<String, Integer>();
    remoteArchiveFailures = new HashMap<String, Integer>();
    filesToUpload = new ConcurrentLinkedQueue<ArchiveFile>();
    // TODO: consider and add multiple upload threads
    uploadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (Thread.currentThread().equals(uploadThread) && !filesToUpload.isEmpty()) {
          ArchiveFile archiveFile = filesToUpload.poll();
          runArchive(archiveFile.archive, archiveFile.remoteArchive, archiveFile.file,
              archiveFile.wifiOnly);
        }
        uploadThread = null;
        mainHandler.post(stopSelfTask);
      }
    });
  }

  public void onDestroy() {
    if (uploadThread != null && uploadThread.isAlive()) {
      uploadThread = null;
    }
    if (lock != null && lock.isHeld()) {
      lock.release();
    }
  }

  public int onRun(String archiveName, String remoteArchiveName) {
    Log.i(LogUtil.TAG, "Starting...");
    if (isOnline()) {
      if (archiveName != null && remoteArchiveName != null) {
        FileArchive archive = getArchive(archiveName);
        RemoteFileArchive remoteArchive = getRemoteArchive(remoteArchiveName);
        if (archive != null && remoteArchive != null) {
          for (File file : archive.getAll()) {
            archive(archive, remoteArchive, file, wifiOnly);
          }
        }
      }
    }

    // Start upload thread if necessary, even if no files to ensure stop
    if (uploadThread != null && !uploadThread.isAlive()) {
      uploadThread.start();
    }

    return Service.START_STICKY;
  }

  /**
   * Returns the file archive that the upload service will use.
   * 
   * @param databaseName
   * @return
   */
  public FileArchive getArchive(String name) {
    return DefaultArchive.getArchive(ctx, name);
  }

  /**
   * Get the remote archive with the specified name
   * 
   * @param name
   * @return
   */
  protected abstract RemoteFileArchive getRemoteArchive(final String name);


  public void archive(FileArchive archive, RemoteFileArchive remoteArchive, File file,
      boolean wifiOnly) {
    ArchiveFile archiveFile = new ArchiveFile(archive, remoteArchive, file, wifiOnly);
    if (!filesToUpload.contains(archiveFile)) {
      Log.i(LogUtil.TAG, "Queuing " + file.getName());
      filesToUpload.offer(archiveFile);
    }
  }

  protected void runArchive(FileArchive archive, RemoteFileArchive remoteArchive, File file,
      boolean wifiOnly) {
    Integer numRemoteFailures = remoteArchiveFailures.get(remoteArchive.getId());
    numRemoteFailures = (numRemoteFailures == null) ? 0 : numRemoteFailures;
    if (numRemoteFailures < maxRemoteRetries && isOnline()) {
      Log.i(LogUtil.TAG, "Archiving..." + file.getName());
      if (remoteArchive.add(file)) {
        archive.remove(file);
      } else {
        Integer numFileFailures = fileFailures.get(file.getName());
        numFileFailures = (numFileFailures == null) ? 1 : numFileFailures + 1;
        numRemoteFailures += 1;
        fileFailures.put(file.getName(), numFileFailures);
        remoteArchiveFailures.put(remoteArchive.getId(), numRemoteFailures);
        // 3 Attempts
        if (numFileFailures < maxFileRetries) {
          filesToUpload.offer(new ArchiveFile(archive, remoteArchive, file, wifiOnly));
        } else {
          Log.i(LogUtil.TAG, "Failed to upload '" + file.getAbsolutePath() + "' after 3 attempts.");
        }
      }
    } else {
      Log.i(LogUtil.TAG, "Canceling upload.  Remote archive '" + remoteArchive.getId()
          + "' is not currently available.");
    }
  }

  /**
   * Convenience class for pairing the database name with the db file
   */
  protected class ArchiveFile {
    public final FileArchive archive;
    public final RemoteFileArchive remoteArchive;
    public final File file;
    public final boolean wifiOnly;

    public ArchiveFile(FileArchive archive, RemoteFileArchive remoteArchive, File file,
        boolean wifiOnly) {
      this.archive = archive;
      this.remoteArchive = remoteArchive;
      this.file = file;
      this.wifiOnly = wifiOnly;
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o instanceof ArchiveFile
          && EqualsUtil.areEqual(remoteArchive.getId(), ((ArchiveFile) o).remoteArchive.getId())
          && EqualsUtil.areEqual(file, ((ArchiveFile) o).file);
    }

    @Override
    public int hashCode() {
      return HashCodeUtil.hash(HashCodeUtil.hash(HashCodeUtil.SEED, file), remoteArchive.getId());
    }
  }

  public boolean isOnline() {
    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
    if (!wifiOnly && netInfo != null && netInfo.isConnectedOrConnecting()) {
      return true;
    } else if (wifiOnly) {
      State wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
      if (State.CONNECTED.equals(wifiInfo) || State.CONNECTING.equals(wifiInfo)) {
        return true;
      }
    }
    return false;
  }

}
