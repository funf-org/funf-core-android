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
                        Field delegateField = AnnotationUtil.getField(ConfigRewriteUtil.DELEGATE_FIELD_NAME, value.getClass());
                        if (delegateField != null) {
                            boolean isAccessible = delegateField.isAccessible();
                            delegateField.setAccessible(true);
                            Object existingDelegate = delegateField.get(value);
                            if (existingDelegate != null)
                                injectListener(existingDelegate, listener);
                            delegateField.setAccessible(isAccessible);
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
