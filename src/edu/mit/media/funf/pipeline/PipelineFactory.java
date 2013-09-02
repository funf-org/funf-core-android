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
package edu.mit.media.funf.pipeline;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.util.AnnotationUtil;
import edu.mit.media.funf.util.LogUtil;

public class PipelineFactory implements RuntimeTypeAdapterFactory {

    public static final String SCHEDULES_FIELD_NAME = "schedules";
    public static final String DATA_FIELD_NAME = "data";
    public static final String SOURCE_FIELD_NAME = "source";
    public static final String DURATION_FIELD_NAME = "duration";
    public static final String DELEGATE_FIELD_NAME = "delegate";
    public static final String FILTERS_FIELD_NAME = "filters";

    public static final String PROBE_DS = "edu.mit.media.funf.datasource.ProbeDataSource";
    public static final String COMPOSITE_DS = "edu.mit.media.funf.datasource.CompositeDataSource";
    public static final String PROBE_BUILTIN = "edu.mit.media.funf.probe.builtin";
    public static final String ALARM_PROBE = "edu.mit.media.funf.probe.builtin.AlarmProbe";
    public static final String ACTION_ADAPTER = "edu.mit.media.funf.action.ActionAdapter";
    public static final String REGISTER_DURATION_ACTION = "edu.mit.media.funf.action.RegisterDurationAction";

    public static final String SCHEDULE = "@schedule";
    public static final String PROBE = "@probe";
    public static final String TYPE = "@type";

    public static final TypeToken<Map<String,Schedule>> SCHEDULES_FIELD_TYPE_TOKEN = new TypeToken<Map<String,Schedule>>(){};

    private RuntimeTypeAdapterFactory delegate;

    /**
     * Use the base class as the default class.
     * @param context
     * @param baseClass
     */
    public PipelineFactory(Context context) {
        this(context, BasicPipeline.class);
    }

    /**
     * @param context
     * @param baseClass  
     * @param defaultClass  Setting this to null will cause a ParseException if the runtime type information is incorrect or unavailable.
     */
    public PipelineFactory(Context context, Class<? extends Pipeline> defaultClass) {
        assert context != null;
        this.delegate = new DefaultRuntimeTypeAdapterFactory<Pipeline>(context, Pipeline.class, defaultClass, new ConfigurableTypeAdapterFactory());
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        TypeAdapter<T> delegateAdapter = delegate.create(gson, type);
        if (delegateAdapter != null) {
            delegateAdapter = new ScheduleAnnotatedTypeAdapter<T>(gson, type, delegateAdapter);
        }
        return delegateAdapter;
    }

    private static class ScheduleAnnotatedTypeAdapter<T> extends TypeAdapter<T> {

        private Gson gson;
        private TypeToken<T> type;
        private TypeAdapter<T> delegateAdapter;

        private ScheduleAnnotatedTypeAdapter(Gson gson, TypeToken<T> type, TypeAdapter<T> delegateAdapter) {
            this.gson = gson;
            this.type = type;
            this.delegateAdapter = delegateAdapter;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            // Cannot determine what was annotated, and what was in schedules
            delegateAdapter.write(out, value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            JsonObject el = Streams.parse(in).getAsJsonObject();

            // Strip existing json schedules
            JsonObject directSchedules = el.has(SCHEDULES_FIELD_NAME) ? el.remove(SCHEDULES_FIELD_NAME).getAsJsonObject() : new JsonObject();

            // Strip off @schedule annotations to update schedules attribute
            // Load annotated schedules
            JsonObject annotatedSchedules = new JsonObject();
            // TODO: make this recursive to have nested schedules with dot notation
            //			for (Map.Entry<String,JsonElement> entry : el.entrySet()) {
            //				JsonElement entryEl = entry.getValue();
            //				if (entryEl.isJsonObject()) {
            //					JsonObject subConfig = entryEl.getAsJsonObject();
            //					if (subConfig.has(SCHEDULE)) {
            //						JsonElement scheduleConfig = subConfig.remove(SCHEDULE);
            //						annotatedSchedules.add(entry.getKey(), scheduleConfig);
            //					}
            //				}
            //			}

            // For compatibility with previous versions of config file, rewrite 
            // any @schedule annotated data as CompositeDataSource containing 
            // an AlarmProbe and RegisterDurationAction.
            rewriteScheduleAsDataSource(el);

            // rewrite @probe annotations as ProbeDataSource
            rewriteProbeAsDataSource(el);

            Log.d(LogUtil.TAG, el.toString());

            T result = delegateAdapter.read(new JsonTreeReader(el));

            // If there is a 'schedules' field, inject schedules
            Field schedulesField = AnnotationUtil.getField(SCHEDULES_FIELD_NAME, result.getClass());
            if (schedulesField != null) {
                ///// Default schedules for every schedulable top level object
                JsonObject defaultSchedules = new JsonObject();
                List<Field> fields = new ArrayList<Field>();
                for (Field field : AnnotationUtil.getAllFields(fields, result.getClass())) {
                    DefaultSchedule defaultSchedule = field.getAnnotation(DefaultSchedule.class);
                    if (defaultSchedule == null) {
                        boolean currentAccessibility = field.isAccessible();
                        try {
                            field.setAccessible(true);
                            Object fieldValue = field.get(result);
                            if (fieldValue != null) {
                                Class<?> fieldRuntimeClass = field.get(result).getClass();
                                defaultSchedule = fieldRuntimeClass.getAnnotation(DefaultSchedule.class);
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
                        } catch (IllegalAccessException e) {
                            Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
                        } finally {
                            field.setAccessible(currentAccessibility);
                        }
                    }
                    if (defaultSchedule != null) {
                        defaultSchedules.add(field.getName(), gson.toJsonTree(defaultSchedule, DefaultSchedule.class));
                    }
                }

                JsonObject schedulesJson = directSchedules;
                JsonUtils.deepCopyOnto(defaultSchedules, schedulesJson, false); // Copy in default schedules, but do not replace
                JsonUtils.deepCopyOnto(annotatedSchedules, schedulesJson, true); // Override with annotations


                // For each schedule find default schedule, fill in remainder
                Map<String,Schedule> schedules = gson.fromJson(schedulesJson, new TypeToken<Map<String,Schedule>>(){}.getType());	

                boolean currentAccessibility = schedulesField.isAccessible();
                try {
                    schedulesField.setAccessible(true);
                    schedulesField.set(result, schedules);
                } catch (IllegalArgumentException e) {
                    Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
                } catch (IllegalAccessException e) {
                    Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
                } finally {
                    schedulesField.setAccessible(currentAccessibility);
                }
            }
            return result;
        }

        /**
         * If the given JsonObject contains a member named "@schedule", rewrites
         * the object as a CompositeDataSource consisting of an AlarmProbe and 
         * RegisterDurationAction
         * 
         * Recursively checks for "@schedule" annotations in member JsonObjects.
         * 
         * Renames the "strict" property in the schedule object to "exact" in
         * the AlarmProbe.
         *  
         * eg.
         * { "@type": "edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe",
         *   "@schedule": { "interval": 300, "duration": 10, "strict": false, "offset": 12345 } }
         * 
         * will be rewritten as:
         * 
         * { "@type": "edu.mit.media.funf.datasource.CompositeDataSource",
         *   "source": { "@probe": "edu.mit.media.funf.probe.builtin.AlarmProbe",
         *               "interval": 300, "exact": false, "offset": 12345 },
         *   "filters": { "@type": "edu.mit.media.funf.action.ActionAdapter",
         *                "delegate": { "@type": "edu.mit.media.funf.action.RegisterDurationAction",
         *                              "duration": 10,
         *                              "delegate": { "@probe": "edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe" }
         *                            }
         *              }
         * } 
         * 
         * @param baseObj
         * @param scheduleObj
         */
        private void rewriteScheduleAsDataSource(JsonObject baseObj) {
            if (baseObj == null)
                return;

            if (baseObj.has(SCHEDULE)) {
                
                JsonObject scheduleObj = (JsonObject)baseObj.remove(SCHEDULE);
                int duration = scheduleObj.has(DURATION_FIELD_NAME) ? 
                        scheduleObj.remove(DURATION_FIELD_NAME).getAsInt() : 0;
                scheduleObj.addProperty(PROBE, ALARM_PROBE);
                renameJsonObjectKey(scheduleObj, "strict", "exact");

                JsonObject probeObj = JsonUtils.deepCopy(baseObj);
                renameJsonObjectKey(probeObj, TYPE, PROBE);
                
                JsonObject actionObj = new JsonObject();
                actionObj.addProperty(TYPE, REGISTER_DURATION_ACTION);
                actionObj.addProperty(DURATION_FIELD_NAME, duration);
                actionObj.add(DELEGATE_FIELD_NAME, probeObj);
                
                JsonObject filtersObj = new JsonObject();
                filtersObj.addProperty(TYPE, ACTION_ADAPTER);
                filtersObj.add(DELEGATE_FIELD_NAME, actionObj);

                // empty existing baseObj
                List<String> keys = new ArrayList<String>();
                for (Map.Entry<String, JsonElement> entry: baseObj.entrySet()) {
                    keys.add(entry.getKey());
                }
                
                for (String key: keys) {
                    baseObj.remove(key);
                }
                
                // populate baseObj with CompositeDataSource properties
                baseObj.addProperty(TYPE, COMPOSITE_DS);
                baseObj.add(SOURCE_FIELD_NAME, scheduleObj);
                baseObj.add(FILTERS_FIELD_NAME, filtersObj);
            }

            // recursive check for @schedule
            for (Map.Entry<String, JsonElement> entry: baseObj.entrySet()) {
                JsonElement entryEl = entry.getValue();
                if (entryEl.isJsonObject()) {
                    rewriteScheduleAsDataSource(entryEl.getAsJsonObject());
                } else if (entryEl.isJsonArray()) {
                    for (JsonElement arrayEl: entryEl.getAsJsonArray()) {
                        if (arrayEl.isJsonObject())
                            rewriteScheduleAsDataSource(arrayEl.getAsJsonObject());
                    }
                }
            }
        }

        /**
         * If the given JsonObject contains a member named "@probe", rewrites
         * the object as a ProbeDataSource consisting of the given probe.
         * 
         * For builtin probes, the "@probe" class name can be shortened to
         * the classname preceded by a ".", i.e. ".SampleProbe".
         * 
         * eg.
         * { "@probe": ".AlarmProbe", "interval": 300, "exact": false, "offset": 12345 }
         *   
         * will be rewritten to
         * 
         * { "@type": "edu.mit.media.funf.datasource.ProbeDataSource",
         *   "source": { "@probe": "edu.mit.media.funf.probe.builtin.AlarmProbe",
         *               "interval": 300, "exact": false, "offset": 12345 } }
         * 
         * Recursively checks for "@probe" annotations in member JsonObjects.
         * 
         * @param baseObj
         */
        private void rewriteProbeAsDataSource(JsonObject baseObj) {
            if (baseObj == null)
                return;
            
            if (baseObj.has(PROBE)) {
                
                JsonObject probeObj = JsonUtils.deepCopy(baseObj);
                String probeType = probeObj.remove(PROBE).getAsString();
                if (probeType.startsWith("."))
                    probeType = PROBE_BUILTIN + probeType;
                probeObj.addProperty(TYPE, probeType);

                // empty existing baseObj
                List<String> keys = new ArrayList<String>();
                for (Map.Entry<String, JsonElement> entry: baseObj.entrySet()) {
                    keys.add(entry.getKey());
                }
                
                for (String key: keys) {
                    baseObj.remove(key);
                }

                // populate baseObj with ProbeDataSource properties
                baseObj.addProperty(TYPE, PROBE_DS);
                baseObj.add(SOURCE_FIELD_NAME, probeObj);
            }
            
            // recursive check for @probe
            for (Map.Entry<String, JsonElement> entry: baseObj.entrySet()) {
                JsonElement entryEl = entry.getValue();
                if (entryEl.isJsonObject()) {
                    rewriteProbeAsDataSource(entryEl.getAsJsonObject());
                } else if (entryEl.isJsonArray()) {
                    for (JsonElement arrayEl: entryEl.getAsJsonArray()) {
                        if (arrayEl.isJsonObject())
                            rewriteProbeAsDataSource(arrayEl.getAsJsonObject());
                    }
                }
            }
        }

        private void renameJsonObjectKey(JsonObject object, String currentKey, String newKey) {
            if (object.has(currentKey)) {
                object.add(newKey, object.get(currentKey));
                object.remove(currentKey);
            }
        }

    }

    @Override
    public <T> Class<? extends T> getRuntimeType(JsonElement el, TypeToken<T> type) {
        return delegate.getRuntimeType(el, type);
    }
}
