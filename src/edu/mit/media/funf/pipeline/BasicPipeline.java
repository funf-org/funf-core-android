/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
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
 * 
 */
package edu.mit.media.funf.pipeline;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

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
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.StringUtil;

public class BasicPipeline implements Pipeline {

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
            
            DataListener databaseListener = (DataListener)new WriteDataAction(databaseHelper);
            
            for (StartableDataSource dataSource: data) {
                dataSource.setListener(databaseListener);
            }
            
            if (schedules != null) {
                if (schedules.containsKey("archive")) {
                    DataListener archiveListener = (DataListener)new ActionAdapter(
                            new RunArchiveAction(archive, databaseHelper));
                    schedules.get("archive").setListener(archiveListener);
                    schedules.get("archive").start();
                }
                
                if (schedules.containsKey("upload")) {
                    DataListener uploadListener = (DataListener)new ActionAdapter(
                            new RunUploadAction(archive, upload, uploader));
                    schedules.get("upload").setListener(uploadListener);
                    schedules.get("upload").start();
                }
                
                if (schedules.containsKey("update")) {
                    DataListener updateListener = (DataListener)new ActionAdapter(
                            new RunUpdateAction(name, getFunfManager(), update));
                    schedules.get("update").setListener(updateListener);
                    schedules.get("update").start();
                }
            }
            
            for (StartableDataSource dataSource: data) {
                dataSource.start();
            }
            
            enabled = true;
            Log.d(LogUtil.TAG, "BasicPipeline enabled");
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                setupDataSources();
            }
        });
    }

    @Override
    public void onRun(String action, JsonElement config) {
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
