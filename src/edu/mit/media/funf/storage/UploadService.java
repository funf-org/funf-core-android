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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.util.LockUtil;
import edu.mit.media.funf.util.LogUtil;

public class UploadService {

  @Configurable
  private int maxRemoteRetries = 6; 
  
  @Configurable
  private int maxFileRetries = 3;
  
  private Context context;
  
  private Map<String, Integer> fileFailures;
  private Map<String, Integer> remoteArchiveFailures;
  private Set<File> filesToUpload;
  private WakeLock lock;
  
  private Handler uploadHandler;
  private Looper looper;
  private Runnable endUploads = new Runnable() {
    
    @Override
    public void run() {
      if (lock != null && lock.isHeld()) {
        lock.release();
      }
      lock = null;
    }
  };
  
  public UploadService() {
    
  }
  
  public UploadService(Context context) {
    setContext(context);
  }
  
  public void setContext(Context context) {
    this.context = context;
  }

  public void start() {
    HandlerThread thread = new HandlerThread(getClass().getName());
    thread.start();
    looper = thread.getLooper();
    uploadHandler = new Handler(looper);
    fileFailures = new HashMap<String, Integer>();
    remoteArchiveFailures = new HashMap<String, Integer>();
    filesToUpload = Collections.synchronizedSet(new HashSet<File>());
  }

  public void stop() {
    looper.quit();
    endUploads.run();
  }

  public void run(final FileArchive archive, final RemoteFileArchive remoteArchive) {
    Log.i(LogUtil.TAG, "Running upload...");
    if (archive != null && remoteArchive != null) {
      if (lock == null) {
        lock = LockUtil.getWakeLock(context);
      }
      for (final File file : archive.getAll()) {
        archive(archive, remoteArchive, file);
      }
    }
  }


  public void archive(final FileArchive archive, final RemoteFileArchive remoteArchive, final File file) {
    if (!filesToUpload.contains(file)) {
      filesToUpload.add(file);
      uploadHandler.post(new Runnable() {
        @Override
        public void run() {
          runArchive(archive, remoteArchive, file);
        }
      });
      uploadHandler.removeCallbacks(endUploads);
      uploadHandler.post(endUploads); // Add stop self to end
    }
  }

  protected void runArchive(FileArchive archive, RemoteFileArchive remoteArchive, File file) {
    Integer numRemoteFailures = remoteArchiveFailures.get(remoteArchive.getId());
    numRemoteFailures = (numRemoteFailures == null) ? 0 : numRemoteFailures;
    if (numRemoteFailures < maxRemoteRetries && remoteArchive.isAvailable()) {
      Log.i(LogUtil.TAG, "Archiving..." + file.getName());
      if (remoteArchive.add(file)) {
        archive.remove(file);
        filesToUpload.remove(file);
      } else {
        Integer numFileFailures = fileFailures.get(file.getName());
        numFileFailures = (numFileFailures == null) ? 1 : numFileFailures + 1;
        numRemoteFailures += 1;
        fileFailures.put(file.getName(), numFileFailures);
        remoteArchiveFailures.put(remoteArchive.getId(), numRemoteFailures);
        // 3 Attempts
        if (numFileFailures < maxFileRetries) {
          filesToUpload.remove(file); // Remove so we can queue up again
          archive(archive, remoteArchive, file);
        } else {
          Log.i(LogUtil.TAG, "Failed to upload '" + file.getAbsolutePath() + "' after 3 attempts.");
          filesToUpload.remove(file);
        }
      }
    } else {
      Log.i(LogUtil.TAG, "Canceling upload.  Remote archive '" + remoteArchive.getId()
          + "' is not currently available.");
      filesToUpload.remove(file);
    }
  }


}
