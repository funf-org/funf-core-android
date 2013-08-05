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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.trigger.Trigger;
import edu.mit.media.funf.util.LogUtil;

public class TriggerActionPipeline implements Pipeline {

    @Configurable
    protected String name = "triggeraction";

    @Configurable
    protected int version = 1;

    @Configurable
    protected FileArchive archive = null;

    @Configurable
    protected RemoteFileArchive upload = null;

    @Configurable
    protected ConfigUpdater update = null;

    @Configurable
    protected Map<String, JsonElement> probes = new HashMap<String, JsonElement>(); 

    @Configurable
    protected Map<String, JsonElement> triggers = new HashMap<String, JsonElement>();

    @Configurable
    protected Map<String, JsonElement> actions = new HashMap<String, JsonElement>();

    private UploadService uploader;

    private boolean enabled;
    private FunfManager manager;
    private SQLiteOpenHelper databaseHelper = null;
    private Looper looper;
    private Handler handler;

    private void setupTriggerActionGraph() {
        if (enabled == false) {
            Map<String, Trigger> allTriggers = getAllTriggers();
            Map<String, Action> allActions = getAllActions();

            // register actions for each trigger
            for (Trigger trigger: allTriggers.values()) {
                Set<String> actionLabels = trigger.getActionLabels();
                if (actionLabels != null && !actionLabels.isEmpty()) {
                    for (String actionLabel: actionLabels) {
                        if (allActions.containsKey(actionLabel)) {
                            trigger.registerAction(allActions.get(actionLabel));
                        }
                    }    
                }
            }

            // register triggers for each action
            for (Action action: allActions.values()) {
                Set<String> triggerLabels = action.getTriggerLabels();
                if (triggerLabels != null && !triggerLabels.isEmpty()) {
                    for (String triggerLabel: triggerLabels) {
                        if (allTriggers.containsKey(triggerLabel)) {
                            action.registerTrigger(allTriggers.get(triggerLabel));
                        }
                    }    
                }
            }

            // activate init trigger
            if (allTriggers.containsKey("init")) {
                Trigger trigger = allTriggers.get("init");
                trigger.start();    
            }
            enabled = true;
        }
    }

    private void destroyTriggerActionGraph() {
        if (enabled == true) {
            Map<String, Trigger> allTriggers = getAllTriggers();
            Map<String, Action> allActions = getAllActions();

            // deactivate triggers
            for (Trigger trigger: allTriggers.values()) {
                trigger.unregisterAllActions();
                trigger.destroy();
            }

            // deactivate actions
            for (Action action: allActions.values()) {
                action.unregisterAllTriggers();
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
        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();
        this.looper = thread.getLooper();
        this.handler = new Handler(looper);
        handler.post(new Runnable() {
            @Override
            public void run() {
                setupTriggerActionGraph();
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
                destroyTriggerActionGraph();
                looper.quit();
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    protected Handler getHandler() {
        return handler;
    }

    public FunfManager getFunfManager() {
        return manager;
    }

    public Probe getProbeByLabel(String label) {
        if (probes.containsKey(label)) {
            return getFunfManager().getGson().fromJson(probes.get(label), Probe.class);
        } else {
            return null;
        }
    }

    public Map<String, Probe> getAllProbes() {
        Map<String, Probe> allProbes = new HashMap<String, Probe>();
        for (String label: probes.keySet()) {
            allProbes.put(label, getProbeByLabel(label));
        }
        return allProbes;
    }

    public Trigger getTriggerByLabel(String label) {
        if (triggers.containsKey(label)) {
            Trigger trigger = getFunfManager().getGson().fromJson(triggers.get(label), Trigger.class);
            trigger.setLabel(label);
            trigger.setPipeline(this);
            trigger.setActionHandler(getHandler());
            return trigger;
        } else {
            return null;
        }
    }

    public Map<String, Trigger> getAllTriggers() {
        Map<String, Trigger> allTriggers = new HashMap<String, Trigger>();
        for (String label: triggers.keySet()) {
            allTriggers.put(label, getTriggerByLabel(label));
        }
        return allTriggers;
    }

    public Action getActionByLabel(String label) {
        if (actions.containsKey(label)) {
            Action action = getFunfManager().getGson().fromJson(actions.get(label), Action.class);
            action.setLabel(label);
            action.setPipeline(this);
            return action;
        } else {
            return null;
        }
    }

    public Map<String, Action> getAllActions() {
        Map<String, Action> allActions = new HashMap<String, Action>();
        for (String label: actions.keySet()) {
            allActions.put(label, getActionByLabel(label));
        }
        return allActions;
    }

    public SQLiteDatabase getDb() {
        return databaseHelper.getReadableDatabase();
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

    public void writeData(String name, IJsonObject data) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        final double timestamp = data.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsDouble();
        final String value = data.toString();
        if (timestamp == 0L || name == null || value == null) {
            Log.e(LogUtil.TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + name + " - " + value);
            throw new SQLException("Not all required fields specified.");
        }
        ContentValues cv = new ContentValues();
        cv.put(NameValueDatabaseHelper.COLUMN_NAME, name);
        cv.put(NameValueDatabaseHelper.COLUMN_VALUE, value);
        cv.put(NameValueDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
        db.insertOrThrow(NameValueDatabaseHelper.DATA_TABLE.name, "", cv);
    }

}
