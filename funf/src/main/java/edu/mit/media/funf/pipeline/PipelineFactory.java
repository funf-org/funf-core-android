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
