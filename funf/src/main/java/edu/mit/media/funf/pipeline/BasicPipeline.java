/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.media.funf.pipeline;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.action.ActionAdapter;
import edu.mit.media.funf.action.RunArchiveAction;
import edu.mit.media.funf.action.RunUpdateAction;
import edu.mit.media.funf.action.RunUploadAction;
import edu.mit.media.funf.action.WriteDataAction;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.util.StringUtil;

public class BasicPipeline implements Pipeline, DataListener {

    public static final String ACTION_ARCHIVE = "archive",
            ACTION_UPLOAD = "upload",
            ACTION_UPDATE = "update";
    
    @Configurable
    protected String name = "actiongraph";

    @Configurable
    protected int version = 1;

    @Configurable
    protected FileArchive archive = null;

    @Configurable
    protected RemoteFileArchive upload = null;

    @Configurable
    protected ConfigUpdater update = null;
    
    @Configurable
    protected List<StartableDataSource> data = null;
    
    @Configurable
    protected Map<String, StartableDataSource> schedules = null;
    
    private UploadService uploader;    
    
    private boolean enabled;
    private FunfManager manager;
    private SQLiteOpenHelper databaseHelper = null;
    private Looper looper;
    private Handler handler;
    
    private WriteDataAction writeAction;
    private RunArchiveAction archiveAction;
    private RunUploadAction uploadAction;
    private RunUpdateAction updateAction;
    
//    private class DataRequestInfo {
//        private DataListener listener;
//        private JsonElement checkpoint;
//    }
//
//    private StateListener probeStateListener = new StateListener() {
//        @Override
//        public void onStateChanged(Probe probe, State previousState) {
//            if (probe instanceof ContinuableProbe && previousState == State.RUNNING) {
//                JsonElement checkpoint = ((ContinuableProbe)probe).getCheckpoint();
//                IJsonObject config = (IJsonObject)JsonUtils.immutable(getFunfManager().getGson().toJsonTree(probe));
//                for (DataRequestInfo requestInfo : activeDataRequests.get(config)) {
//                    requestInfo.checkpoint = checkpoint;
//                }
//            }
//        }
//    };
    
    public IJsonObject getImmutableProbeConfig(String probeConfig) {
        Probe probe = getFunfManager().getGson().fromJson(probeConfig, Probe.class);
        IJsonObject probeJson = (IJsonObject)JsonUtils.immutable(
                getFunfManager().getGson().toJsonTree(probe));
        return probeJson;
    }
    
    protected void setupDataSources() {
        if (enabled == false) {
            
            for (StartableDataSource dataSource: data) {
                dataSource.setListener((DataListener)writeAction);
            }
            
            if (schedules != null) {
                if (schedules.containsKey("archive")) {
                    DataListener archiveListener = (DataListener)new ActionAdapter(archiveAction);
                    schedules.get("archive").setListener(archiveListener);
                    schedules.get("archive").start();
                }
                
                if (schedules.containsKey("upload")) {
                    DataListener uploadListener = (DataListener)new ActionAdapter(uploadAction);
                    schedules.get("upload").setListener(uploadListener);
                    schedules.get("upload").start();
                }
                
                if (schedules.containsKey("update")) {
                    DataListener updateListener = (DataListener)new ActionAdapter(updateAction);
                    schedules.get("update").setListener(updateListener);
                    schedules.get("update").start();
                }
            }
            
            for (StartableDataSource dataSource: data) {
                dataSource.start();
            }
            
            enabled = true;
        }
    }

    private void destroyDataSources() {
        if (enabled == true) {
            
            for (StartableDataSource dataSource: data) {
                dataSource.stop();
            }
            enabled = false;
        }
    }

    @Override
    public void onCreate(FunfManager manager) {
        if (archive == null) {
            archive = new DefaultArchive(manager, name);
        }
        if (uploader == null) {
            uploader = new UploadService(manager);
            uploader.start();
        }
        this.manager = manager;
        reloadDbHelper(manager);
        
        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();
        this.looper = thread.getLooper();
        this.handler = new Handler(looper);
        
        writeAction = new WriteDataAction(databaseHelper);
        writeAction.setHandler(handler);
        archiveAction = new RunArchiveAction(archive, databaseHelper);
        archiveAction.setHandler(handler);
        uploadAction = new RunUploadAction(archive, upload, uploader);
        uploadAction.setHandler(handler);
        updateAction = new RunUpdateAction(name, getFunfManager(), update);
        updateAction.setHandler(handler);
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                setupDataSources();
            }
        });
    }

    @Override
    public void onRun(String action, JsonElement config) {
        // Run on handler thread
        if (ACTION_ARCHIVE.equals(action)) {
            archiveAction.run();
        } else if (ACTION_UPLOAD.equals(action)) {
            uploadAction.run();
        } else if (ACTION_UPDATE.equals(action)) {
            updateAction.run();
        }
    }

    @Override
    public void onDestroy() {
        if (uploader != null) {
            uploader.stop();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                destroyDataSources();
                looper.quit();
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
      writeAction.onDataReceived(probeConfig, data);
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
      writeAction.onDataCompleted(probeConfig, checkpoint);
    }

    public Handler getHandler() {
        return handler;
    }

    public FunfManager getFunfManager() {
        return manager;
    }

    protected void reloadDbHelper(Context ctx) {
        this.databaseHelper = new NameValueDatabaseHelper(ctx, StringUtil.simpleFilesafe(name), version);
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public int getVersion() {
        return version;
    }


    public void setVersion(int version) {
        this.version = version;
    }


    public FileArchive getArchive() {
        return archive;
    }


    public void setArchive(FileArchive archive) {
        this.archive = archive;
    }

    public RemoteFileArchive getUpload() {
        return upload;
    }


    public void setUpload(RemoteFileArchive upload) {
        this.upload = upload;
    }


    public ConfigUpdater getUpdate() {
        return update;
    }


    public void setUpdate(ConfigUpdater update) {
        this.update = update;
    }
}
