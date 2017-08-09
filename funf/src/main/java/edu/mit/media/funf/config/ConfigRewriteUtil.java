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

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.action.ActionAdapter;
import edu.mit.media.funf.action.StartableAction;
import edu.mit.media.funf.action.StartDataSourceAction;
import edu.mit.media.funf.action.StopDataSourceAction;
import edu.mit.media.funf.datasource.CompositeDataSource;
import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.datasource.ProbeDataSource;
import edu.mit.media.funf.filter.CompositeFilter;
import edu.mit.media.funf.filter.ProbabilisticFilter;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.AlarmProbe;

/**
 * Rewrites the given config JsonObject by transforming the special annotations
 * (starting with "@") to expanded json that is directly readable by the GSON 
 * TypeAdapterFactory code.
 * 
 * See the rewrite() function for details of the config file transformation.
 *
 */
public class ConfigRewriteUtil {

    public static final String SCHEDULES_FIELD_NAME = "schedules";
    public static final String DATA_FIELD_NAME = "data";
    public static final String SOURCE_FIELD_NAME = "source";
    public static final String DURATION_FIELD_NAME = "duration";
    public static final String TARGET_FIELD_NAME = "target";
    public static final String FILTER_FIELD_NAME = "filter";
    public static final String LISTENER_FIELD_NAME = "listener";
    public static final String DELEGATOR_FIELD_NAME = "delegator";

    public static final String TYPE = "@type";
    public static final String SCHEDULE = "@schedule";
    public static final String PROBE = "@probe";
    public static final String FILTER = "@filter";
    public static final String ACTION = "@action";
    public static final String TRIGGER = "@trigger";
    
    public static final String CLASSNAME_PREFIX = FunfManager.class.getPackage().getName();
    
    public static final String PROBE_PREFIX = AlarmProbe.class.getPackage().getName();
    public static final String FILTER_PREFIX = ProbabilisticFilter.class.getPackage().getName();
    public static final String ACTION_PREFIX = Action.class.getPackage().getName();
    
    public static final String DATA_LISTENER = DataListener.class.getName();
    public static final String DATA_SOURCE = DataSource.class.getName();
    public static final String PROBE_DS = ProbeDataSource.class.getName();
    public static final String COMPOSITE_DS = CompositeDataSource.class.getName();
    public static final String ALARM_PROBE = AlarmProbe.class.getName();
    public static final String ACTION_ADAPTER = ActionAdapter.class.getName();
    public static final String STARTABLE_ACTION = StartableAction.class.getName();
    public static final String START_DS_ACTION = StartDataSourceAction.class.getName();
    public static final String STOP_DS_ACTION = StopDataSourceAction.class.getName();
    public static final String COMPOSITE_FILTER = CompositeFilter.class.getName();

    public static JsonParser parser = new JsonParser();
    
    /**
     * Rewrite the given config json. The config can be specified in a compact
     * format using annotations (see wiki for details). This function expands 
     * each annotation to obtain a config file format that is easily parsable
     * by gson TypeAdapters.
     * 
     * The following annotations are replaced recursively (one at a time, and 
     * strictly in this order):
     * 
     * "@schedule", "@filters", "@actions", "@probe"
     * 
     * To see the details of how each annotation is rewritten, see the specific
     * functions, for eg. rewriteScheduleAnnotation().
     * 
     * @param base
     */
    public static void rewrite(JsonObject base) {
        recursiveReplace(base, SCHEDULE);
        recursiveReplace(base, FILTER);
        recursiveReplace(base, ACTION);
        recursiveReplace(base, PROBE);
    }
    
    /**
     * Performs a recursive check for the given annotation in the given base object.
     * 
     * If an object is found to contain the annotation, the appropriate rewrite 
     * function is called on that object, by using rewriteFnSelector().
     * 
     * In case of nested annotations, the innermost ones will be replaced first,
     * followed by outer ones.
     * 
     * @param base
     * @param annotation
     */
    public static void recursiveReplace(JsonObject base, String annotation) {
        if (base == null) {
            return;
        }
        
        // Due to the difficulty in doing in-place modification of JsonObject or 
        // JsonArray, the recursive search is performed in such a way that
        // a reference to the parent object of the JsonObject containing
        // the given annotation is always available.
        // If such a JsonObject is found, it is passed to the appropriate rewrite 
        // function, which returns a new transformed JsonObject. 
        // The old object is replaced by the new one by modifying the reference to 
        // it in the parent JsonObject.
        // In case of JsonArray, we cannot modify individual elements, so a new array 
        // is created from scratch, and the old array is replaced by it.
        for (Map.Entry<String, JsonElement> entry: base.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                JsonArray newArray = new JsonArray();
                for (JsonElement arrayEl: entry.getValue().getAsJsonArray()) {
                    JsonElement elToAdd = arrayEl;
                    if (arrayEl.isJsonObject()) {
                        JsonObject arrayObj = arrayEl.getAsJsonObject();
                        recursiveReplace(arrayObj, annotation);
                        if (arrayObj.has(annotation)) {
                            JsonObject newObj = rewriteFnSelector(annotation, arrayObj);
                            if (newObj != null) {
                                elToAdd = newObj;
                            }
                        }
                    }
                    newArray.add(elToAdd);
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
            } else if (FILTER.equals(annotation)) {
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

        // The CompositeDataSource which will be returned from this rewrite.
        JsonObject dataSourceObj = new JsonObject();
        dataSourceObj.addProperty(TYPE, COMPOSITE_DS);

        JsonObject scheduleObj = (JsonObject)baseObj.remove(SCHEDULE);

        // If scheduleObj is already a CompositeDataSource, it implies that
        // there was a nested @schedule annotation which has already been
        // taken care of by an earlier call to this function. In that case
        // the scheduleObj will simply be taken as a nested "source" of
        // the current CompositeDataSource.
        if (!isDataSourceObject(scheduleObj)) {
            // If it is not a data source, then the "@probe" annotation in the 
            // scheduleObj will be parsed as a data source by rewriteProbeAnnotation().
            // For compatibility, "@type" annotations indicating probes are
            // converted to "@probe" annotations.
            // For compatibility, if neither "@type" or "@probe" annotations
            // exist, an "AlarmProbe" is added (as that signifies the default behavior
            // of the earlier scheduling system).
            if (scheduleObj.has(TYPE)) {
                renameJsonObjectKey(scheduleObj, TYPE, PROBE);
            } else if (!scheduleObj.has(PROBE)){
                scheduleObj.addProperty(PROBE, ALARM_PROBE);
            }
        }

        Double duration = 0.0;
        if (scheduleObj.has(PROBE) && 
                ALARM_PROBE.equals(scheduleObj.get(PROBE).getAsString())) {
            renameJsonObjectKey(scheduleObj, "strict", "exact");
            if (scheduleObj.has(DURATION_FIELD_NAME))
                duration = scheduleObj.remove(DURATION_FIELD_NAME).getAsDouble();
        }

        JsonElement filtersEl = null;
        if (scheduleObj.has(FILTER)) {
            filtersEl = scheduleObj.remove(FILTER);
        }

        // If baseObj is itself a schedule object (i.e this is a nested schedule), 
        // a "@trigger" annotation would provide the action to be performed by
        // the outer schedule object. This must be kept separate from other members
        // of baseObj.
        JsonObject triggerObj = null;
        if (baseObj.has(TRIGGER)) {
            triggerObj = baseObj.remove(TRIGGER).getAsJsonObject();
        }
        
        // To select the action to be performed whenever this schedule object fires:
        // 1. First check if the baseObj is really a probe or a data source. If
        //    it is empty except for the "@schedule" tag, then don't register any action.
        //    (Happens in the direct "schedules" member.)
        // 2. If it is not empty, then check if a "@trigger" annotation exists, which denotes
        //    a user-specified custom action to be registered whenever scheduler fires.
        // 3. If no "@trigger" exists, then select a default Action. If "duration" was 
        //    specified in the schedule object, add a StartableAction to run 
        //    the dependent data source for that duration.
        // 4. If no non-zero duration was specified, add a StartDataSourceAction to simply start
        //    the dependent data source whenever this schedule object fires.
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
                    actionObj.addProperty(TYPE, STARTABLE_ACTION);
                    actionObj.addProperty(DURATION_FIELD_NAME, duration);        
                } else {
                    actionObj.addProperty(TYPE, START_DS_ACTION);
                }
            }
            actionObj.add(TARGET_FIELD_NAME, baseObj);    
        }

        dataSourceObj.add(SOURCE_FIELD_NAME, scheduleObj);
        if (filtersEl != null)
            dataSourceObj.add(FILTER, filtersEl);
        if (actionObj != null)
            dataSourceObj.add(ACTION, actionObj);
        if (triggerObj != null)
            dataSourceObj.add(TRIGGER, triggerObj);

        return dataSourceObj;
    }

    /**
     * Rewrites the filter array denoted by "@filters" to a "filters" member
     * with nested filters, in the order of their appearance in the array.
     * 
     * If the baseObj is not a CompositeDataSource, converts it into one, and
     * pushing the "@probe" annotation (if it exists) to the "source" field.
     * 
     * If the entire filter class name is not specified, it will be prefixed by
     * FILTER_PREFIX.
     * 
     * eg.
     * { "@probe": ".ActivityProbe",
     *   "sensorDelay": "FASTEST",
     *   "@filters": [{ "@type": ".KeyValueFilter", "matches": { "motionState": "Driving" } },
     *                { "@type": ".ProbabilityFilter", "probability": 0.5 } ] 
     * }
     * 
     * will be rewritten to
     * 
     * { "@type": "edu.mit.media.funf.datasource.CompositeDataSource",
     *   "source": { "@probe": ".ActivityProbe", "sensorDelay": "FASTEST" },
     *   "filters": { "@type": "edu.mit.media.funf.filter.KeyValueFilter", 
     *                "matches": { "motionState": "Driving" }
     *                "listener": { "@type": "edu.mit.media.funf.filter.ProbabilityFilter", 
     *                              "probability": 0.5 } }  
     * }
     * 
     * @param baseObj
     */
    public static JsonObject rewriteFiltersAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;
        
        JsonElement filterEl = baseObj.remove(FILTER);
        JsonObject filterObj = null;
        if (filterEl.isJsonArray()) {
            for (JsonElement filterIter: filterEl.getAsJsonArray()) {
                if (filterIter.isJsonObject()) {
                    // Add the filter class name prefix if not specified.
                    addTypePrefix(filterIter.getAsJsonObject(), FILTER_PREFIX);
                }
            }
            filterObj = new JsonObject();
            filterObj.addProperty(TYPE, COMPOSITE_FILTER);
            filterObj.add("filters", filterEl.getAsJsonArray());
        } else {
            filterObj = filterEl.getAsJsonObject();
        }
        
        // Add the filter class name prefix if not specified.
        addTypePrefix(filterObj.getAsJsonObject(), FILTER_PREFIX);
        
        // Insert the filter object denoted by "@filter" to the existing
        // "filter" field.
        insertFilter(baseObj, filterObj);

        // If the baseObj is not a data source, convert it into CompositeDataSource.
        if (!isDataSourceObject(baseObj)) {
            JsonObject dataSourceObj = new JsonObject();
            dataSourceObj.addProperty(TYPE, COMPOSITE_DS);
            dataSourceObj.add(FILTER_FIELD_NAME, baseObj.remove(FILTER_FIELD_NAME));
            if (baseObj.has(ACTION)) {
                dataSourceObj.add(ACTION, baseObj.remove(ACTION));
            }
            
            // The remaining fields of baseObj should be "@probe" annotation and 
            // probe parameters.
            dataSourceObj.add(SOURCE_FIELD_NAME, baseObj);
            return dataSourceObj;
        } else {
            return baseObj;
        }
    }
    
    /**
     * Rewrites "@action" annotation as an Action object, and adds it to the 
     * end of the "filter" chain.
     * 
     * If the specified Action does not implement the DataListener interface,
     * it is wrapped by an ActionAdapter. 
     * 
     * If the entire action class name is not specified, it will be prefixed by
     * ACTION_PREFIX.
     * 
     * @param baseObj
     */
    public static JsonObject rewriteActionAnnotation(JsonObject baseObj) {
        if (baseObj == null)
            return null;
        
        // Add prefix if entire class name is not specified.
        JsonObject actionObj = baseObj.remove(ACTION).getAsJsonObject();
        addTypePrefix(actionObj, ACTION_PREFIX);
        
        // Check if the specified Action type implements the DataListener
        // interface.
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
        
        // Wrap the Action with an ActionAdapter if it does not
        // implement the DataListener interface and insert the 
        // Action to the end of the "filter" chain.
        if (!isDataListener) {
            JsonObject actionAdapter = new JsonObject();
            actionAdapter.addProperty(TYPE, ACTION_ADAPTER);
            actionAdapter.add(TARGET_FIELD_NAME, actionObj);
            insertFilter(baseObj, actionAdapter);
        } else {
            insertFilter(baseObj, actionObj);
        }
        
        // If the baseObj is not a data source, convert it into CompositeDataSource.
        if (!isDataSourceObject(baseObj)) {
            JsonObject dataSourceObj = new JsonObject();
            dataSourceObj.addProperty(TYPE, COMPOSITE_DS);
            dataSourceObj.add(FILTER_FIELD_NAME, baseObj.remove(FILTER_FIELD_NAME));
            
            // The remaining fields of baseObj should be "@probe" annotation and 
            // probe parameters.
            dataSourceObj.add(SOURCE_FIELD_NAME, baseObj);
            return dataSourceObj;
        } else {
            return baseObj;
        }
    }
        
    /**
     * If the given JsonObject contains a member named "@probe", rewrite the object 
     * as a ProbeDataSource consisting of the given probe. The remaining members of
     * the baseObj should be parameters of the specified probe.
     * 
     * If the entire probe class name is not specified, it will be prefixed by
     * PROBE_PREFIX.
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
    
    /**
     * Check if the "@type" member of the given object is one of PROBE_DS 
     * or COMPOSITE_DS.
     * 
     * @param object
     * @return
     */
    public static boolean isDataSourceObject(JsonObject object) {
        if (object.has(TYPE)) {
            String type = object.get(TYPE).getAsString();
            try {
                Class<?> runtimeClass = Class.forName(type);
                return implementsInterface(runtimeClass, DATA_SOURCE);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private static boolean implementsInterface(Class<?> runtimeClass, String interfaceName) {
        if (runtimeClass == null) 
            return false;
        for (Class<?> runtimeInterface: runtimeClass.getInterfaces()) {
            if (interfaceName.equals(runtimeInterface.getName())) {
                return true;
            }
        }
        Class<?> parentClass = runtimeClass.getSuperclass();
        return implementsInterface(parentClass, interfaceName);
    }
    
    /**
     * Insert the given filter object to the end of the filters chain in
     * the "filter" member of given baseObj.
     * 
     * The array of filters is converted into nested filters, with each 
     * subsequent filter added as a listener of the previous filter, in the
     * order of presence in the given array.
     *  
     * @param baseObj
     * @param filters
     */
    public static void insertFilter(JsonObject baseObj, JsonObject filter) {
        // If the "filter" field already exists in the baseObj, iterate to the 
        // end of the chain and add the newFilters object.
        if (baseObj.has(FILTER_FIELD_NAME)) {
            JsonObject currFilters = baseObj.remove(FILTER_FIELD_NAME).getAsJsonObject();
            JsonObject iterFilter = currFilters;
            while (iterFilter.has(LISTENER_FIELD_NAME)) {
                iterFilter = currFilters.get(LISTENER_FIELD_NAME).getAsJsonObject();
            }
            iterFilter.add(LISTENER_FIELD_NAME, filter);
            baseObj.add(FILTER_FIELD_NAME, currFilters);
        } else {
            baseObj.add(FILTER_FIELD_NAME, filter);
        }        
    }

    /**
     * If the "@type" member of the given object is an incomplete type,
     * i.e. it starts with a ".", then add the given prefix to that type. 
     * 
     * @param object
     * @param prefix
     */
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
