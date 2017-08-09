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
package edu.mit.media.funf.config;

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.io.IOException;
import java.lang.reflect.Field;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.util.AnnotationUtil;

public class ListenerInjectorTypeAdapterFactory implements TypeAdapterFactory {

    private TypeAdapterFactory delegate;

    public ListenerInjectorTypeAdapterFactory(TypeAdapterFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final TypeAdapter<T> delegateAdapter = delegate.create(gson, type);
        if (delegateAdapter == null) {
            return null;
        } else {
            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegateAdapter.write(out, value);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    T value = delegateAdapter.read(in);
                    if (value != null && value instanceof DataSource) {
                        try {
                            Field filtersField = AnnotationUtil.getField(ConfigRewriteUtil.FILTER_FIELD_NAME, value.getClass());
                            Field delegatorField = AnnotationUtil.getField(ConfigRewriteUtil.DELEGATOR_FIELD_NAME, value.getClass());
                            boolean isDelegatorAccessible = delegatorField.isAccessible();
                            delegatorField.setAccessible(true);
                            DataListener delegator = (DataListener)delegatorField.get(value);
                            if (filtersField != null && delegator != null) {
                                boolean isAccessible = filtersField.isAccessible();
                                filtersField.setAccessible(true);
                                Object existingFilter = filtersField.get(value);
                                if (existingFilter == null)
                                    filtersField.set(value, delegator);
                                else
                                    injectListener(existingFilter, delegator);
                                filtersField.setAccessible(isAccessible);
                            }    
                            delegatorField.setAccessible(isDelegatorAccessible);
                        } catch (SecurityException e) {
                            // Swallow
                            Log.v(TAG, e.getMessage());
                        } catch (IllegalArgumentException e) {
                            // Swallow
                            Log.v(TAG, e.getMessage());
                        } catch (IllegalAccessException e) {
                            // Swallow
                            Log.v(TAG, e.getMessage());
                        }
                    }
                    return value;
                }

                private void injectListener(Object value, DataListener listener) {
                    
                    try {
                        Field listenerField = AnnotationUtil.getField(ConfigRewriteUtil.LISTENER_FIELD_NAME, value.getClass());
                        if (listenerField != null && DataListener.class.isAssignableFrom(listenerField.getType())) {
                            boolean isAccessible = listenerField.isAccessible();
                            listenerField.setAccessible(true);
                            Object existingListener = listenerField.get(value);
                            if (existingListener == null)
                                listenerField.set(existingListener, listener);
                            else
                                injectListener(existingListener, listener);
                            listenerField.setAccessible(isAccessible);
                            return;
                        }
                    } catch (SecurityException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    } catch (IllegalArgumentException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    } catch (IllegalAccessException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    }
                    
                    try {
                        Field targetField = AnnotationUtil.getField(ConfigRewriteUtil.TARGET_FIELD_NAME, value.getClass());
                        if (targetField != null) {
                            boolean isAccessible = targetField.isAccessible();
                            targetField.setAccessible(true);
                            Object existingTarget = targetField.get(value);
                            if (existingTarget != null)
                                injectListener(existingTarget, listener);
                            targetField.setAccessible(isAccessible);
                            return;
                        }
                    } catch (SecurityException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    } catch (IllegalArgumentException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    } catch (IllegalAccessException e) {
                        // Swallow
                        Log.v(TAG, e.getMessage());
                    }
                    
                    if (value instanceof DataSource) {
                        ((DataSource)value).setListener(listener);
                        return;
                    }
                }

            };
        }
    }

}
