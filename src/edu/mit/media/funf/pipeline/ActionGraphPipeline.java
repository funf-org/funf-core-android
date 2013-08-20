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

import java.util.ArrayList;
import java.util.HashMap;
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
import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.action.ActionAdapter;
import edu.mit.media.funf.action.ActionGraph;
import edu.mit.media.funf.action.ProbeAction;
import edu.mit.media.funf.action.RunArchiveAction;
import edu.mit.media.funf.action.WriteDataAction;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuableProbe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.State;
import edu.mit.media.funf.probe.Probe.StateListener;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.StringUtil;

public class ActionGraphPipeline implements Pipeline, ActionGraph {

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

    private UploadService uploader;

    private boolean enabled;
    private FunfManager manager;
    private SQLiteOpenHelper databaseHelper = null;
    private Looper looper;
    private Handler handler;
    
    private class DataRequestInfo {
        private DataListener listener;
        private JsonElement checkpoint;
    }

    private StateListener probeStateListener = new StateListener() {
        @Override
        public void onStateChanged(Probe probe, State previousState) {
            if (probe instanceof ContinuableProbe && previousState == State.RUNNING) {
                JsonElement checkpoint = ((ContinuableProbe)probe).getCheckpoint();
                IJsonObject config = (IJsonObject)JsonUtils.immutable(getFunfManager().getGson().toJsonTree(probe));
                for (DataRequestInfo requestInfo : activeRequests.get(config)) {
                    requestInfo.checkpoint = checkpoint;
                }
            }
        }
    };

    private Map<String, DataListener> listenersByLabel; // list of listeners indexed by label (so that Action objects can refer to them)
    private Map<IJsonObject, List<DataRequestInfo>> activeRequests; // list of listeners that are currently registered to a probe

    public void addListenerByLabel(String label, DataListener listener) {
        listenersByLabel.put(label, listener);
    }

    public void removeListenerByLabel(String label) {
        listenersByLabel.remove(label);
    }

    public void registerProbeListener(String listenerLabel, String probeConfig) {
        Probe probe = getFunfManager().getGson().fromJson(probeConfig, Probe.class);
        IJsonObject probeJson = (IJsonObject)JsonUtils.immutable(
                getFunfManager().getGson().toJsonTree(probe));
        registerProbeListener(listenerLabel, probeJson);
    }
    
    public void registerProbeListener(String listenerLabel, IJsonObject probeConfig) {
        if (listenersByLabel.containsKey(listenerLabel)) {
            registerProbeListener(listenersByLabel.get(listenerLabel), probeConfig);
            Log.d(LogUtil.TAG, listenerLabel);
        }
    }

    public void registerProbeListener(DataListener listener, IJsonObject probeConfig) {
        synchronized(activeRequests) {
            List<DataRequestInfo> dataRequests = activeRequests.get(probeConfig);
            if (dataRequests != null) {
                for (DataRequestInfo request: dataRequests) {
                    if (request.listener == listener) { // listener is already active for this probe
                        return;
                    }
                }
            } else {
                activeRequests.put(probeConfig, new ArrayList<DataRequestInfo>());
                dataRequests = activeRequests.get(probeConfig);
            }
            Probe probe = getFunfManager().getGson().fromJson(probeConfig, Probe.class);
            probe.registerListener(listener);
            probe.addStateListener(probeStateListener); 
            DataRequestInfo newDataRequest = new DataRequestInfo();
            newDataRequest.listener = listener;
            dataRequests.add(newDataRequest);
            Log.d(LogUtil.TAG, "pipeline: registered listener");
        }
    }

    public void unregisterProbeListener(String listenerLabel, String probeConfig) {
        Probe probe = getFunfManager().getGson().fromJson(probeConfig, Probe.class);
        IJsonObject probeJson = (IJsonObject)JsonUtils.immutable(
                getFunfManager().getGson().toJsonTree(probe));
        unregisterProbeListener(listenerLabel, probeJson);
    }
    
    public void unregisterProbeListener(String listenerLabel, IJsonObject probeConfig) {
        if (listenersByLabel.containsKey(listenerLabel)) {
            unregisterProbeListener(listenersByLabel.get(listenerLabel), probeConfig);
        }
    }

    public void unregisterProbeListener(DataListener listener, IJsonObject probeConfig) {
        synchronized(activeRequests) {
            List<DataRequestInfo> dataRequests = activeRequests.get(probeConfig);
            Probe probe = getFunfManager().getGson().fromJson(probeConfig, Probe.class);
            if (probe != null && dataRequests != null) {
                for (int i = 0; i < dataRequests.size(); i++) {
                    if (dataRequests.get(i).listener == listener) {
                        dataRequests.remove(i);
                        if (probe instanceof ContinuousProbe) {
                            ((ContinuousProbe)probe).unregisterListener(listener);
                        }
                        if (probe instanceof PassiveProbe) {
                            ((PassiveProbe)probe).unregisterPassiveListener(listener);
                        }
                        break; // Should only have one request for this listener and probe
                    }
                }
            }   
        }
    }

    private void setupActionGraph() {
        if (enabled == false) {
            activeRequests = new HashMap<IJsonObject, List<DataRequestInfo>>();
            listenersByLabel = new HashMap<String, DataListener>();
            
            // Hardcoded setup for now
            // TODO: setup graph via config file and TypeAdapters
            
            String accelAlarmConfig = 
                    "{ \"@type\": \"edu.mit.media.funf.probe.builtin.AlarmProbe\"," +
            		"  \"interval\": 60," +
                    "  \"exact\": false" +
            		"}";
            String accelProbeConfig = "edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe";
            String archiveAlarmConfig = 
                    "{ \"@type\": \"edu.mit.media.funf.probe.builtin.AlarmProbe\"," +
                    "  \"interval\": 200," +
                    "  \"exact\": false" +
                    "}";
            
            Action writeData = new WriteDataAction(this, databaseHelper);
            addListenerByLabel("write", (DataListener)writeData);
            
            Action startAccel = new ProbeAction(this, accelProbeConfig, "write", ProbeAction.REGISTER_LISTENER);
            Action stopAccel = new ProbeAction(this, accelProbeConfig, "write", ProbeAction.UNREGISTER_LISTENER);
            stopAccel.setDelay(10);
            
            addListenerByLabel("start", (DataListener)new ActionAdapter(startAccel));
            addListenerByLabel("stop", (DataListener)new ActionAdapter(stopAccel));
            
            Action archiveData = new RunArchiveAction(this, getArchive(), databaseHelper);
            addListenerByLabel("archive", (DataListener)new ActionAdapter(archiveData));
            
            registerProbeListener("start", accelAlarmConfig);
            registerProbeListener("stop", accelAlarmConfig);
            registerProbeListener("archive", archiveAlarmConfig);
            
            enabled = true;
            Log.d(LogUtil.TAG, "action graph created");
        }
    }

    private void destroyActionGraph() {
        if (enabled == true) {
            synchronized(activeRequests) {
                for (Map.Entry<IJsonObject, List<DataRequestInfo>> entry : activeRequests.entrySet()) {
                    List<DataRequestInfo> dataRequests = entry.getValue();
                    Probe probe = getFunfManager().getGson().fromJson(entry.getKey(), Probe.class);
                    for (int i = 0; i < dataRequests.size(); i++) {
                        DataRequestInfo request = dataRequests.remove(i);
                        if (probe instanceof ContinuousProbe) {
                            ((ContinuousProbe)probe).unregisterListener(request.listener);
                        }
                        if (probe instanceof PassiveProbe) {
                            ((PassiveProbe)probe).unregisterPassiveListener(request.listener);
                        }
                    }
                }
                activeRequests.clear();
            }
            
            synchronized(listenersByLabel) {
                listenersByLabel.clear();
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
                setupActionGraph();
            }
        });
    }

    @Override
    public void onRun(String action, JsonElement config) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDestroy() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                destroyActionGraph();
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
