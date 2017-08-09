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

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.mit.media.funf.util.AnnotationUtil;

public class ContextInjectorTypeAdapaterFactory implements TypeAdapterFactory {

	public static final String CONTEXT_FIELD = "context";
	
	private Context context;
	private TypeAdapterFactory delegate;
	
	public ContextInjectorTypeAdapaterFactory(Context context, TypeAdapterFactory delegate) {
		if (context == null) {
			throw new RuntimeException("Context cannot be null.");
		}
		this.context = context;
		this.delegate = delegate;
	}
	
	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
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
					if (value != null) {
						try {
							Field contextField = AnnotationUtil.getField(CONTEXT_FIELD, value.getClass());
							if (contextField != null && Context.class.isAssignableFrom(contextField.getType())) {
								boolean isAccessible = contextField.isAccessible();
								contextField.setAccessible(true);
								contextField.set(value, context);
								contextField.setAccessible(isAccessible);
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
					}
					return value;
				}
			};
		}
	}

}
