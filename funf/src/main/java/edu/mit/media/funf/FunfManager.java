/**
 * 
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
 * 
 */
package edu.mit.media.funf;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;

import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.ContextInjectorTypeAdapaterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultScheduleSerializer;
import edu.mit.media.funf.config.HttpConfigUpdater;
import edu.mit.media.funf.config.ListenerInjectorTypeAdapterFactory;
import edu.mit.media.funf.config.SingletonTypeAdapterFactory;
import edu.mit.media.funf.datasource.Startable;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.StringUtil;

public class FunfManager extends Service {

    public static final String 
    ACTION_KEEP_ALIVE = "funf.keepalive",
    ACTION_INTERNAL = "funf.internal";

    private static final String
    PIPELINE_TYPE = "funf/pipeline",
    ALARM_TYPE = "funf/alarm";

    private static final String 
    DISABLED_PIPELINE_LIST = "__DISABLED__";

    private Handler handler;
    private SharedPreferences prefs;
    private Map<String,Pipeline> pipelines;
    private Map<String,Pipeline> disabledPipelines;
    private Set<String> disabledPipelineNames;

    @Override
    public void onCreate() {
        super.onCreate();
        this.handler = new Handler();
        getGson(); // Sets gson
        this.prefs = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        this.pipelines = new HashMap<String, Pipeline>();
        this.disabledPipelines = new HashMap<String, Pipeline>();
        this.disabledPipelineNames = new HashSet<String>(Arrays.asList(prefs.getString(DISABLED_PIPELINE_LIST, "").split(",")));
        this.disabledPipelineNames.remove(""); // Remove the empty name, if no disabled pipelines exist
        reload();
    }

    public void reload() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    reload();
                }
            });
            return;
        } 
        Set<String> pipelineNames = new HashSet<String>();
        pipelineNames.addAll(prefs.getAll().keySet());
        pipelineNames.remove(DISABLED_PIPELINE_LIST);
        Bundle metadata = getMetadata();
        pipelineNames.addAll(metadata.keySet());
        for (String pipelineName : pipelineNames) {
            reload(pipelineName);
        }
    }

    public void reload(final String name) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    reload(name);
                }
            });
            return;
        }
        String pipelineConfig = null;
        Bundle metadata = getMetadata();
        if (prefs.contains(name)) {
            pipelineConfig = prefs.getString(name, null);
        } else if (metadata.containsKey(name)) {
            pipelineConfig = metadata.getString(name);
        } 
        if (disabledPipelineNames.contains(name)) {
            // Disabled, so don't load any config
            Pipeline disabledPipeline = gson.fromJson(pipelineConfig, Pipeline.class);
            disabledPipelines.put(name, disabledPipeline);
            pipelineConfig = null;
        }
        if (pipelineConfig == null) {
            unregisterPipeline(name);
        } else {
            Pipeline newPipeline = gson.fromJson(pipelineConfig, Pipeline.class);
            registerPipeline(name, newPipeline); // Will unregister previous before running
        }
    }

    public JsonObject getPipelineConfig(String name) {
        String configString = prefs.getString(name, null);
        Bundle metadata = getMetadata();
        if (configString == null && metadata.containsKey(name)) {
            configString = metadata.getString(name);
        }
        return configString == null ? null : new JsonParser().parse(configString).getAsJsonObject();
    }

    public boolean save(String name, JsonObject config) {
        try {
            // Check if this is a valid pipeline before saving
            Pipeline pipeline = getGson().fromJson(config, Pipeline.class);
            return prefs.edit().putString(name, config.toString()).commit();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "Unable to save config: " + config.toString());
            return false;
        }
    }

    public boolean saveAndReload(String name, JsonObject config) {
        boolean success = save(name, config);
        if (success) {
            reload(name);
        }
        return success;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO: call onDestroy on all pipelines
        for (Pipeline pipeline : pipelines.values()) {
            pipeline.onDestroy();
        }

        // TODO: save outstanding requests
        // TODO: remove all remaining Alarms

        // TODO: make sure to destroy all probes
        for (Object probeObject : getProbeFactory().getCached()) {
            //String componentString = JsonUtils.immutable(gson.toJsonTree(probeObject)).toString();
            //cancelProbe(componentString);
            ((Probe)probeObject).destroy();
        }
        getProbeFactory().clearCache();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
            // Does nothing, but wakes up FunfManager
        } else if (ACTION_INTERNAL.equals(action)) {
            String type = intent.getType();
            Uri componentUri = intent.getData();
            if (PIPELINE_TYPE.equals(type)) {
                // Handle pipeline action
                String pipelineName = getComponentName(componentUri);
                String pipelineAction = getAction(componentUri);
                Pipeline pipeline = pipelines.get(pipelineName);
                if (pipeline != null) {
                    pipeline.onRun(pipelineAction, null);
                }
            } else if (ALARM_TYPE.equals(type)) {
                // Handle registered alarms
                String probeConfig = getComponentName(componentUri);
                final Probe probe = getGson().fromJson(probeConfig, Probe.class); 
                if (probe instanceof Runnable) {
                    handler.post((Runnable)probe);
                }
            }

        }
        return Service.START_FLAG_RETRY; // TODO: may want the last intent always redelivered to make sure system starts up
    }

    private Bundle getMetadata() {
        try {
            Bundle metadata = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA).metaData;
            return metadata == null ? new Bundle() : metadata;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Unable to get metadata for the FunfManager service.");
        }
    }

    /**
     * Get a gson builder with the probe factory built in
     * @return
     */
    public GsonBuilder getGsonBuilder() {
        return getGsonBuilder(this);
    }

    public static class ConfigurableRuntimeTypeAdapterFactory<E> extends DefaultRuntimeTypeAdapterFactory<E> {

        public ConfigurableRuntimeTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
            super(context, 
                    baseClass, 
                    defaultClass, 
                    new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory()));
        }

    }

    /**
     * Get a gson builder with the probe factory built in
     * @return
     */
    public static GsonBuilder getGsonBuilder(Context context) {
        return new GsonBuilder()
        .registerTypeAdapterFactory(getProbeFactory(context))
        .registerTypeAdapterFactory(getActionFactory(context))
        .registerTypeAdapterFactory(getPipelineFactory(context))
        .registerTypeAdapterFactory(getDataSourceFactory(context))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<Schedule>(context, Schedule.class, BasicSchedule.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<ConfigUpdater>(context, ConfigUpdater.class, HttpConfigUpdater.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<FileArchive>(context, FileArchive.class, DefaultArchive.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<RemoteFileArchive>(context, RemoteFileArchive.class, HttpArchive.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<DataListener>(context, DataListener.class, null))
        .registerTypeAdapter(DefaultSchedule.class, new DefaultScheduleSerializer())
        .registerTypeAdapter(Class.class, new JsonSerializer<Class<?>>() {

            @Override
            public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
                return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.getName());
            }
        });
    }

    private Gson gson;
    /**
     * Get a Gson instance which includes the SingletonProbeFactory
     * @return
     */
    public Gson getGson() {
        if (gson == null) {
            gson = getGsonBuilder().create();
        }
        return gson;
    }

    public TypeAdapterFactory getPipelineFactory() {
        return getPipelineFactory(this);
    }

    private static PipelineFactory PIPELINE_FACTORY;
    public static PipelineFactory getPipelineFactory(Context context) {
        if (PIPELINE_FACTORY == null) {
            PIPELINE_FACTORY = new PipelineFactory(context);
        }
        return PIPELINE_FACTORY;
    }

    public SingletonTypeAdapterFactory getProbeFactory() {
        return getProbeFactory(this);
    }

    private static SingletonTypeAdapterFactory PROBE_FACTORY;
    public static SingletonTypeAdapterFactory getProbeFactory(Context context) {
        if (PROBE_FACTORY == null) {
            PROBE_FACTORY = new SingletonTypeAdapterFactory(
                    new DefaultRuntimeTypeAdapterFactory<Probe>(
                            context, 
                            Probe.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
        }
        return PROBE_FACTORY;
    }

    public DefaultRuntimeTypeAdapterFactory<Action> getActionFactory() {
        return getActionFactory(this);
    }

    private static DefaultRuntimeTypeAdapterFactory<Action> ACTION_FACTORY;
    public static DefaultRuntimeTypeAdapterFactory<Action> getActionFactory(Context context) {
        if (ACTION_FACTORY == null) {
            ACTION_FACTORY = new DefaultRuntimeTypeAdapterFactory<Action>(
                            context, 
                            Action.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory()));
        }
        return ACTION_FACTORY;
    }
    
    public ListenerInjectorTypeAdapterFactory getDataSourceFactory() {
        return getDataSourceFactory(this);
    }

    private static ListenerInjectorTypeAdapterFactory DATASOURCE_FACTORY;
    public static ListenerInjectorTypeAdapterFactory getDataSourceFactory(Context context) {
        if (DATASOURCE_FACTORY == null) {
            DATASOURCE_FACTORY = new ListenerInjectorTypeAdapterFactory(
                    new DefaultRuntimeTypeAdapterFactory<Startable>(
                            context, 
                            Startable.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
        }
        return DATASOURCE_FACTORY;
    }

    public void registerPipeline(String name, Pipeline pipeline) {
        synchronized (pipelines) {
            Log.d(LogUtil.TAG, "Registering pipeline: " + name);
            unregisterPipeline(name);
            pipelines.put(name, pipeline);
            pipeline.onCreate(this);
        }
    }

    public Pipeline getRegisteredPipeline(String name) {
        Pipeline p = pipelines.get(name);
        if (p == null) {
            p = disabledPipelines.get(name);
        }
        return p;
    }

    public void unregisterPipeline(String name) {
        synchronized (pipelines) {
            Pipeline existingPipeline = pipelines.remove(name);
            if (existingPipeline != null) {
                existingPipeline.onDestroy();
            }
        }
    }

    public void enablePipeline(String name) {
        boolean previouslyDisabled = disabledPipelineNames.remove(name);
        if (previouslyDisabled) {
            prefs.edit().putString(DISABLED_PIPELINE_LIST, StringUtil.join(disabledPipelineNames, ",")).commit();
            reload(name);
        }
    }

    public boolean isEnabled(String name) {
        return this.pipelines.containsKey(name) && !disabledPipelineNames.contains(name);
    }

    public void disablePipeline(String name) {
        boolean previouslyEnabled = disabledPipelineNames.add(name);
        if (previouslyEnabled) {
            prefs.edit().putString(DISABLED_PIPELINE_LIST, StringUtil.join(disabledPipelineNames, ",")).commit();
            reload(name);
        }
    }

    private String getPipelineName(Pipeline pipeline) {
        for (Map.Entry<String, Pipeline> entry : pipelines.entrySet()) {
            if (entry.getValue() == pipeline) {
                return entry.getKey();
            }
        }
        return null;
    }

    public class LocalBinder extends Binder {
        public FunfManager getManager() {
            return FunfManager.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public static void registerAlarm(Context context, String probeConfig, Long start, Long interval, boolean exact) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = getFunfIntent(context, ALARM_TYPE, probeConfig, "");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        if (start == null)
            start = 0L;

        if (interval == null || interval <= 0) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, start, pendingIntent);
        } else {
            if (exact) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, start, interval, pendingIntent);
            } else {
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, start, interval, pendingIntent);
            }
        }
    }

    public static void unregisterAlarm(Context context, String probeConfig) {
        Intent intent = getFunfIntent(context, ALARM_TYPE, probeConfig, "");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            pendingIntent.cancel();
        }
    }    

    /////////////////////////////////////////////
    // Reserve action for later inter-funf communication
    // Use type to differentiate between probe/pipeline
    // funf:<componenent_name>#<action>

    private static final String 
    FUNF_SCHEME = "funf";


    // TODO: should these public?  May be confusing for people just using the library
    private static Uri getComponentUri(String component, String action) {
        return new Uri.Builder()
        .scheme(FUNF_SCHEME)
        .path(component) // Automatically prepends slash
        .fragment(action)
        .build();
    }

    private static String getComponentName(Uri componentUri) {
        return componentUri.getPath().substring(1); // Remove automatically prepended slash from beginning
    }

    private static String getAction(Uri componentUri) {
        return componentUri.getFragment();
    }

    private static Intent getFunfIntent(Context context, String type, String component, String action) {
        return getFunfIntent(context, type, getComponentUri(component, action));
    }

    private static Intent getFunfIntent(Context context, String type, Uri componentUri) {
        Intent intent = new Intent();
        intent.setClass(context, FunfManager.class);
        intent.setPackage(context.getPackageName());
        intent.setAction(ACTION_INTERNAL);
        intent.setDataAndType(componentUri, type);
        return intent;
    }
}
