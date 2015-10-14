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
package edu.mit.media.funf.config;

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;

import org.apache.http.ParseException;

import android.content.Context;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DefaultRuntimeTypeAdapterFactory<E> implements RuntimeTypeAdapterFactory {

	private final Context context;
	private final Class<E> baseClass;
	private final Class<? extends E> defaultClass;
	private TypeAdapterFactory delegateFactory;
	
	/**
	 * Use the base class as the default class.
	 * @param context
	 * @param baseClass
	 */
	public DefaultRuntimeTypeAdapterFactory(Context context, Class<E> baseClass) {
		this(context, baseClass, null);
	}
	
	public DefaultRuntimeTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
		this(context, baseClass, defaultClass, null);
	}
	
	/**
	 * @param context
	 * @param baseClass  
	 * @param defaultClass  Setting this to null will cause a ParseException if the runtime type information is incorrect or unavailable.
	 */
	public DefaultRuntimeTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass, TypeAdapterFactory delegateFactory) {
		assert context != null && baseClass != null;
		if (defaultClass != null && !isInstantiable(defaultClass)) {
			throw new RuntimeException("Default class does not have a default contructor.");
		}
		this.context = context;
		this.baseClass = baseClass;
		this.defaultClass = defaultClass;
		if (delegateFactory == null) {
			this.delegateFactory = new ReflectiveTypeAdapterFactory(
					new ConstructorConstructor(Collections.<Type, InstanceCreator<?>>emptyMap()), 
					FieldNamingPolicy.IDENTITY, 
					Excluder.DEFAULT);
		} else {
			this.delegateFactory = delegateFactory;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<? extends T> getRuntimeType(JsonElement el, TypeToken<T> type) {
		if (baseClass.isAssignableFrom(type.getRawType())) {
			// 1 use type if instatiable
			// 2 use default if type cannot be instantiated and type is assignable from type
			// 3 fail
			final boolean canUseDefaultClass = defaultClass != null && type.getRawType().isAssignableFrom(defaultClass);
			final boolean typeIsInstantiable = DefaultRuntimeTypeAdapterFactory.isTypeInstatiable(type.getRawType());
			final Class<? extends T> defautRuntimeClass = (Class<? extends T>) (typeIsInstantiable ? type.getRawType() : (canUseDefaultClass ? defaultClass : null));
			return DefaultRuntimeTypeAdapterFactory.getRuntimeType(el, (Class<T>)type.getRawType(), defautRuntimeClass);
		}
		return null;
	}

	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (baseClass.isAssignableFrom(type.getRawType())) {
			return new TypeAdapter<T>() {
				@Override
				public void write(JsonWriter out, T value) throws IOException {
					if (value == null) {
						out.nullValue();
						return;
					}
					// TODO: cache these only once per runtime type
					final TypeAdapter delegate = delegateFactory.create(gson, TypeToken.get(value.getClass()));
					JsonTreeWriter treeWriter = new JsonTreeWriter();
					delegate.write(treeWriter, value);
					JsonElement el = treeWriter.get();
					
					if (el.isJsonObject()) {
						JsonObject elObject = el.getAsJsonObject();
						elObject.addProperty(RuntimeTypeAdapterFactory.TYPE, value.getClass().getName());
						Streams.write(elObject, out);
					} else {
						Streams.write(el, out);
					}
				}

				@Override
				public T read(JsonReader in) throws IOException {
					// TODO: need to handle null
					JsonElement el = Streams.parse(in);
					Class<? extends T> runtimeType = getRuntimeType(el, type);
					if (runtimeType == null) {
						throw new ParseException("RuntimeTypeAdapter: Unable to parse runtime type.");
					}
					// TODO: cache these only once per runtime type
					final TypeAdapter<? extends T> delegate = delegateFactory.create(gson, TypeToken.get(runtimeType));
					
					if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
						JsonObject typeObject = new JsonObject();
						typeObject.addProperty(TYPE, el.getAsString());
						el = typeObject;
					}
					
					return delegate.read(new JsonTreeReader(el));
				}
				
			};
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> getRuntimeType(JsonElement el, Class<T> baseClass, Class<? extends T> defaultClass) {
		Class<? extends T> type = defaultClass;
		String typeString = null;
		if (el != null) {
			try {
				if (el.isJsonObject()) {
					JsonObject jsonObject = el.getAsJsonObject();
					if (jsonObject.has(RuntimeTypeAdapterFactory.TYPE)) {
						typeString = jsonObject.get(RuntimeTypeAdapterFactory.TYPE).getAsString();
					}
				} else if (el.isJsonPrimitive()){
					typeString = el.getAsString();
				}
			} catch (ClassCastException e) {
			}
		}
		// TODO: expand string to allow for builtin to be specified as ".SampleProbe"
		if (typeString != null) {
			try {
				Class<?> runtimeClass = Class.forName(typeString);
				if (baseClass.isAssignableFrom(runtimeClass)) {
					type = (Class<? extends T>)runtimeClass;
				} else {
					Log.w(TAG, "RuntimeTypeAdapter: Runtime class '" + typeString + "' is not assignable from default class '" + defaultClass.getName() + "'.");
				}
			} catch (ClassNotFoundException e) {
				Log.w(TAG, "RuntimeTypeAdapter: Runtime class '" + typeString + "' not found.");
			}
		}
		return type;
	}

	public static boolean isTypeInstatiable(Class<?> type) {
		int modifiers = type.getModifiers();
		if (!(Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))) {
			try {
				Constructor<?> noArgConstructor = type.getConstructor();
				return Modifier.isPublic(noArgConstructor.getModifiers());
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
		}
		return false;
	}

	public static boolean isInstantiable(Class<?> type) {
		try {
			type.newInstance();
			return true;
		} catch (IllegalAccessException e) {
		} catch (InstantiationException e) {
		}
		return false;
	}
}
