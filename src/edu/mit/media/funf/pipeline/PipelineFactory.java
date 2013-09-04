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

import edu.mit.media.funf.config.ConfigRewriteUtil;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.util.LogUtil;

public class PipelineFactory implements RuntimeTypeAdapterFactory {

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
            delegateAdapter = new ScheduleAnnotatedTypeAdapter<T>(delegateAdapter);
        }
        return delegateAdapter;
    }

    private static class ScheduleAnnotatedTypeAdapter<T> extends TypeAdapter<T> {

        private TypeAdapter<T> delegateAdapter;

        private ScheduleAnnotatedTypeAdapter(TypeAdapter<T> delegateAdapter) {
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

            JsonObject schedules = new JsonObject();
            
            // Inject "@schedule" annotation into existing schedules
            // i.e. { "key": { schedule-object } } will be transformed to
            //      { "key": { "@schedule": { schedule-object } } }
            if (el.has(ConfigRewriteUtil.SCHEDULES_FIELD_NAME)) {
                JsonObject directSchedules = el.remove(
                        ConfigRewriteUtil.SCHEDULES_FIELD_NAME).getAsJsonObject();
                for (Map.Entry<String,JsonElement> entry : directSchedules.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        JsonObject subConfig = entry.getValue().getAsJsonObject();
                        JsonObject scheduleConfig = new JsonObject(); 
                        scheduleConfig.add(ConfigRewriteUtil.SCHEDULE, 
                                subConfig.remove(ConfigRewriteUtil.SCHEDULE));
                        schedules.add(entry.getKey(), scheduleConfig);
                    }
                }
            }
            
            // strip @schedule annotations from non-data entries (eg. archive, upload, etc)
            for (Map.Entry<String,JsonElement> entry : el.entrySet()) {
                if (ConfigRewriteUtil.DATA_FIELD_NAME.equals(entry.getKey()))
                    continue;
                
                if (entry.getValue().isJsonObject()) {
                    JsonObject subConfig = entry.getValue().getAsJsonObject();
                    if (subConfig.has(ConfigRewriteUtil.SCHEDULE)) {
                        JsonObject scheduleConfig = new JsonObject(); 
                        scheduleConfig.add(ConfigRewriteUtil.SCHEDULE, 
                                subConfig.remove(ConfigRewriteUtil.SCHEDULE));
                        schedules.add(entry.getKey(), scheduleConfig);
                    }
                }
            }
            el.add(ConfigRewriteUtil.SCHEDULES_FIELD_NAME, schedules);
            
            ConfigRewriteUtil.rewrite(el);
            
            Log.d(LogUtil.TAG, el.toString());
            
            T result = delegateAdapter.read(new JsonTreeReader(el));

            return result;
        }
    }

    @Override
    public <T> Class<? extends T> getRuntimeType(JsonElement el, TypeToken<T> type) {
        return delegate.getRuntimeType(el, type);
    }
}
