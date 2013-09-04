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
package edu.mit.media.funf.config;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConfigRewriteUtil {

    public static final String SCHEDULES_FIELD_NAME = "schedules";
    public static final String DATA_FIELD_NAME = "data";
    public static final String SOURCE_FIELD_NAME = "source";
    public static final String DURATION_FIELD_NAME = "duration";
    public static final String DELEGATE_FIELD_NAME = "delegate";
    public static final String FILTERS_FIELD_NAME = "filters";
    public static final String LISTENER_FIELD_NAME = "listener";

    public static final String TYPE = "@type";
    public static final String SCHEDULE = "@schedule";
    public static final String PROBE = "@probe";
    public static final String FILTERS = "@filters";
    public static final String ACTION = "@action";
    public static final String TRIGGER = "@trigger";
    
    public static final String CLASSNAME_PREFIX = "edu.mit.media.funf";
    
    public static final String PROBE_PREFIX = CLASSNAME_PREFIX + ".probe.builtin";
    public static final String FILTER_PREFIX = CLASSNAME_PREFIX + ".filter";
    public static final String ACTION_PREFIX = CLASSNAME_PREFIX + ".action";
    
    public static final String PROBE_DS = CLASSNAME_PREFIX + ".datasource.ProbeDataSource";
    public static final String COMPOSITE_DS = CLASSNAME_PREFIX + ".datasource.CompositeDataSource";
    
    public static final String DATA_LISTENER = CLASSNAME_PREFIX + ".probe.DataListener";
    
    public static final String ALARM_PROBE = PROBE_PREFIX + ".AlarmProbe";
    
    public static final String ACTION_ADAPTER = ACTION_PREFIX + ".ActionAdapter";
    public static final String REGISTER_DURATION_ACTION = ACTION_PREFIX + ".RegisterDurationAction";
    public static final String START_DS_ACTION = ACTION_PREFIX + ".StartDataSourceAction";
    public static final String STOP_DS_ACTION = ACTION_PREFIX + ".StopDataSourceAction";

    public static JsonParser parser = new JsonParser();
    
    public static void rewrite(JsonObject base) {
        recursiveReplace(base, SCHEDULE);
        recursiveReplace(base, FILTERS);
        recursiveReplace(base, ACTION);
        recursiveReplace(base, PROBE);
    }
    
    public static void recursiveReplace(JsonObject base, String annotation) {
        if (base == null) {
            return;
        }
        
        for (Map.Entry<String, JsonElement> entry: base.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                JsonArray newArray = new JsonArray();
                for (JsonElement arrayEl: entry.getValue().getAsJsonArray()) {
                    boolean isReplaced = false;
                    if (arrayEl.isJsonObject()) {
                        JsonObject arrayObj = arrayEl.getAsJsonObject();
                        recursiveReplace(arrayObj, annotation);
                        if (arrayObj.has(annotation)) {
                            JsonObject newObj = rewriteFnSelector(annotation, arrayObj);
                            if (newObj != null) {
                                newArray.add(newObj);
                                isReplaced = true;
                            }
                        }
                    }
                    if (!isReplaced) {
                        newArray.add(arrayEl);
                    }
                }
                entry.setValue(newArray);
            } else if (entry.getValue().isJsonObject()) {
                JsonObject entryObj = entry.getValue().getAsJsonObject();
                recursiveReplace(entryObj, annotation);
                if (entryObj.has(annotation)) {
                    JsonObject newObj = rewriteFnSelector(annotation, entryObj);
                    if (newObj != null)
                        entry.setValue(newObj);
                }
            }         
        } 
    }

    public static JsonObject rewriteFnSelector(String annotation, JsonObject baseObj) {
        if (annotation != null && baseObj != null) {
            if (SCHEDULE.equals(annotation)) {
                return rewriteScheduleAnnotation(baseObj);
            } else if (FILTERS.equals(annotation)) {
                return rewriteFiltersAnnotation(baseObj);
            } else if (ACTION.equals(annotation)) {
                return rewriteActionAnnotation(baseObj);
            } else if (PROBE.equals(annotation)) {
                return rewriteProbeAnnotation(baseObj);
            } 
        }
        return null;
    }

    /**
     * If the given JsonObject contains a member named "@schedule", returns
     * a CompositeDataSource.
     *  
     * Renames the "strict" property in the schedule object to "exact" in
     * the AlarmProbe.
     *  
     * eg.
     * { "@probe": ".AccelerometerSensorProbe",
     *   "sensorDelay": "MEDIUM",
     *   "@schedule": { "@probe": ".ActivityProbe",
     *                  "sensorDelay": "FASTEST",
     *                  "@schedule": { "interval": 60, "offset": 12345 },
     *                  "@filters": [
     *                      { "@type": ".KeyValueFilter",
     *                        "matches": { "motionState": "Driving" } },
     *                      { "@type": ".ProbabilityFilter", "probability": 0.5 }
     *                  ] }
     * }
     * 
     * will be rewritten as:
     * 
     * { "@type": "edu.mit.media.fun.datasource.CompositeDataSource",
     *   "source": { "@type": "edu.mit.media.fun.datasource.CompositeDataSource",
     *               "source": { "@probe": ".AlarmProbe", "interval": 60, "offset": 12345 },
     *               "@action": { "@type": ".StartDataSourceAction",
     *                            "delegate": { "@probe": ".ActivityProbe",
     *                                          "sensorDelay": "FASTEST",
     *                                          "@filters": [
     *                                              { "@type": ".KeyValueFilter",
     *                                                "matches": { "motionState": "Driving" } },
     *                                              { "@type": ".ProbabilityFilter", 
     *                                                "probability": 0.5 }
     *                                          ] } } }
     *   "@action": { "@type: ".StartDataSourceAction",
     *                "delegate": { "@probe": ".AccelerometerSensorProbe",
     *                              "sensorDelay": "MEDIUM" } } } 
     * 
     * @param baseObj The JsonObject which contains a member named "@schedule"
     */
    public static JsonObject rewriteScheduleAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;

        JsonObject dataSourceObj = new JsonObject();
        dataSourceObj.addProperty(TYPE, COMPOSITE_DS);

        JsonObject scheduleObj = (JsonObject)baseObj.remove(SCHEDULE);

        if (!isDataSourceObject(scheduleObj)) {
            if (scheduleObj.has(TYPE)) {
                renameJsonObjectKey(scheduleObj, TYPE, PROBE);
            } else {
                scheduleObj.addProperty(PROBE, ALARM_PROBE);
            }
        }

        int duration = 0;
        if (scheduleObj.has(PROBE) && 
                ALARM_PROBE.equals(scheduleObj.get(PROBE).getAsString())) {
            renameJsonObjectKey(scheduleObj, "strict", "exact");
            if (scheduleObj.has(DURATION_FIELD_NAME))
                duration = scheduleObj.remove(DURATION_FIELD_NAME).getAsInt();
        }

        JsonElement filtersEl = null;
        if (scheduleObj.has(FILTERS)) {
            filtersEl = scheduleObj.remove(FILTERS);
        }

        JsonObject triggerObj = null;
        // if baseObj is itself a schedule object (nested schedule), then 
        // the @trigger in the base object must be kept separate from the
        // other members which relate to probe action.
        if (baseObj.has(TRIGGER)) {
            triggerObj = baseObj.remove(TRIGGER).getAsJsonObject();
        }
        
        JsonObject actionObj = null;
        if (baseObj.has(PROBE) || baseObj.has(TYPE)) {
            if (!isDataSourceObject(baseObj)) {
                renameJsonObjectKey(baseObj, TYPE, PROBE);
            }
            if (scheduleObj.has(TRIGGER)) {
                actionObj = scheduleObj.remove(TRIGGER).getAsJsonObject();
            } else {
                actionObj= new JsonObject();
                if (duration > 0) {
                    actionObj.addProperty(TYPE, REGISTER_DURATION_ACTION);
                    actionObj.addProperty(DURATION_FIELD_NAME, duration);        
                } else {
                    actionObj.addProperty(TYPE, START_DS_ACTION);
                }
            }
            actionObj.add(DELEGATE_FIELD_NAME, baseObj);    
        }

        dataSourceObj.add(SOURCE_FIELD_NAME, scheduleObj);
        if (filtersEl != null)
            dataSourceObj.add(FILTERS, filtersEl);
        if (actionObj != null)
            dataSourceObj.add(ACTION, actionObj);
        if (triggerObj != null)
            dataSourceObj.add(TRIGGER, triggerObj);

        return dataSourceObj;
    }

    public static JsonObject rewriteFiltersAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;
        
        JsonArray filtersArr = baseObj.remove(FILTERS).getAsJsonArray();
        for (JsonElement filter: filtersArr) {
            if (filter.isJsonObject())
                addTypePrefix(filter.getAsJsonObject(), FILTER_PREFIX);
        }
        
        insertFilters(baseObj, filtersArr);

        if (!isDataSourceObject(baseObj)) {
            JsonObject dataSourceObj = new JsonObject();
            dataSourceObj.addProperty(TYPE, COMPOSITE_DS);
            dataSourceObj.add(FILTERS_FIELD_NAME, baseObj.remove(FILTERS_FIELD_NAME));
            if (baseObj.has(ACTION)) {
                dataSourceObj.add(ACTION, baseObj.remove(ACTION));
            }
            dataSourceObj.add(SOURCE_FIELD_NAME, baseObj);
            return dataSourceObj;
        } else {
            return baseObj;
        }
    }
    
    public static JsonObject rewriteActionAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;
        
        JsonObject actionObj = baseObj.remove(ACTION).getAsJsonObject();
        addTypePrefix(actionObj, ACTION_PREFIX);
        
        String actionType = actionObj.get(TYPE).getAsString();
        boolean isDataListener = false;
        try {
            Class<?> runtimeClass = Class.forName(actionType);
            for (Class<?> runtimeInterface: runtimeClass.getInterfaces()) {
                if (DATA_LISTENER.equals(runtimeInterface.getName())) {
                    isDataListener = true;
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        JsonArray actionArr = new JsonArray();
        if (!isDataListener) {
            JsonObject actionAdapter = new JsonObject();
            actionAdapter.addProperty(TYPE, ACTION_ADAPTER);
            actionAdapter.add(DELEGATE_FIELD_NAME, actionObj);
            actionArr.add(actionAdapter);
        } else {
            actionArr.add(actionObj);
        }
        insertFilters(baseObj, actionArr);

        if (!isDataSourceObject(baseObj)) {
            JsonObject dataSourceObj = new JsonObject();
            dataSourceObj.addProperty(TYPE, COMPOSITE_DS);
            dataSourceObj.add(FILTERS_FIELD_NAME, baseObj.remove(FILTERS_FIELD_NAME));
            dataSourceObj.add(SOURCE_FIELD_NAME, baseObj);
            return dataSourceObj;
        } else {
            return baseObj;
        }
    }
        
    /**
     * If the given JsonObject contains a member named "@probe", rewrites
     * the object as a ProbeDataSource consisting of the given probe.
     * 
     * For builtin probes, the class name can be shortened to
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
     * @param baseObj
     */
    public static JsonObject rewriteProbeAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;

        JsonObject dataSourceObj = new JsonObject();
        dataSourceObj.addProperty(TYPE, PROBE_DS);
        
        renameJsonObjectKey(baseObj, PROBE, TYPE);
        addTypePrefix(baseObj, PROBE_PREFIX);

        dataSourceObj.add(SOURCE_FIELD_NAME, baseObj);
        return dataSourceObj;
    }

    public static void renameJsonObjectKey(JsonObject object, String currentKey, String newKey) {
        if (object.has(currentKey)) {
            object.add(newKey, object.get(currentKey));
            object.remove(currentKey);
        }
    }
    
    public static boolean isDataSourceObject(JsonObject object) {
        if (object.has(TYPE)) {
            String type = object.get(TYPE).getAsString();
            if (PROBE_DS.equals(type) || COMPOSITE_DS.equals(type)) {
                return true;
            }
        }
        return false;
    }
    
    public static void insertFilters(JsonObject baseObj, JsonArray filters) {
        JsonObject newFilters = null;
        boolean isFirst = true;
        JsonObject nextFilter = null;
        for (JsonElement arrEl: filters) {
            if (isFirst) {
                newFilters = arrEl.getAsJsonObject();
                nextFilter = newFilters;
                isFirst = false;
            } else {
                JsonObject arrObj = arrEl.getAsJsonObject();
                nextFilter.add(LISTENER_FIELD_NAME, arrObj);
                nextFilter = arrObj;
            }
        }

        if (baseObj.has(FILTERS_FIELD_NAME)) {
            JsonObject currFilters = baseObj.remove(FILTERS_FIELD_NAME).getAsJsonObject();
            JsonObject iterFilter = currFilters;
            while (iterFilter.has(LISTENER_FIELD_NAME)) {
                iterFilter = currFilters.get(LISTENER_FIELD_NAME).getAsJsonObject();
            }
            iterFilter.add(LISTENER_FIELD_NAME, newFilters);
            baseObj.add(FILTERS_FIELD_NAME, currFilters);
        } else {
            baseObj.add(FILTERS_FIELD_NAME, newFilters);
        }        
    }

    public static void addTypePrefix(JsonObject object, String prefix) {
        if (object.has(TYPE)) {
            String type = object.get(TYPE).getAsString();
            if (type.startsWith(".")) {
                type = prefix + type;
                object.addProperty(TYPE, type);
            }
        }   
    }

}
