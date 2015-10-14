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


import java.lang.reflect.Field;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class ConfigurableTypeAdapterFactory implements TypeAdapterFactory {
	 private ReflectiveTypeAdapterFactory delegate;
	 
	 public ConfigurableTypeAdapterFactory() {
		 delegate = new ReflectiveTypeAdapterFactory(
				 new ConstructorConstructor(),
				 new ConfigurableFieldNamingStrategy(), 
				new Excluder().withExclusionStrategy(new ConfigurableExclusionStrategy(), true, true));
	 }
	 
	 public class ConfigurableExclusionStrategy implements ExclusionStrategy {

		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return f.getAnnotation(Configurable.class) == null;
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
		 
	 }

	 public class ConfigurableFieldNamingStrategy implements FieldNamingStrategy {

		@Override
		public String translateName(Field f) {
			Configurable configAnnotation = f.getAnnotation(Configurable.class);
			if (configAnnotation == null || "".equals(configAnnotation.name())) {
				return FieldNamingPolicy.IDENTITY.translateName(f);
			} else {
				return configAnnotation.name();
			}
		}
		 
	 }

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		return delegate.create(gson, type);
	}
	 
	 
}

//public class ConfigurableTypeAdapterFactory<E> {
//	private final Context context;
//	private final Class<E> baseClass;
//	private final Class<? extends E> defaultClass;
//	
//	/**
//	 * Use the base class as the default class.
//	 * @param context
//	 * @param baseClass
//	 */
//	public ConfigurableTypeAdapterFactory(Context context, Class<E> baseClass) {
//		this(context, baseClass, null);
//	}
//	
//	/**
//	 * @param context
//	 * @param baseClass  
//	 * @param defaultClass  Setting this to null will cause a ParseException if the runtime type information is incorrect or unavailable.
//	 */
//	public ConfigurableTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
//		assert context != null && baseClass != null;
//		if (defaultClass != null && !DefaultRuntimeTypeAdapterFactory.isInstantiable(defaultClass)) {
//			throw new RuntimeException("Default class does not have a default contructor.");
//		}
//		this.context = context;
//		this.baseClass = baseClass;
//		this.defaultClass = defaultClass;
//	}
//	
//	@Override
//	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
//		if (baseClass.isAssignableFrom(type.getRawType())) {
//			// TODO: create caching data structures
//			return new TypeAdapter<T>() {
//				@Override
//				public void write(JsonWriter out, T value) throws IOException {
//					// TODO: need to handle null
//					JsonObject jsonObject = new JsonObject();
//					jsonObject.addProperty(RuntimeTypeAdapterFactory.TYPE, value.getClass().getName());
//					List<Field> configurableFields = new ArrayList<Field>();
//					AnnotationUtil.getAllFieldsWithAnnotation(configurableFields, value.getClass(), Configurable.class);
//					for (Field field : configurableFields) {
//						String fieldJsonName = field.getAnnotation(Configurable.class).name();
//						if ("".equals(fieldJsonName)) {
//							fieldJsonName = field.getName();
//						}
//						boolean currentAccessibility = field.isAccessible();
//						try {
//							field.setAccessible(true);
//							jsonObject.add(fieldJsonName, gson.toJsonTree(field.get(value)));
//						} catch (IllegalArgumentException e) {
//							Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
//						} catch (IllegalAccessException e) {
//							Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
//						} finally {
//							field.setAccessible(currentAccessibility);
//						}
//					}
//					Streams.write(jsonObject, out);
//				}
//
//				@Override
//				public T read(JsonReader in) throws IOException {
//					// TODO: need to handle null
//					JsonElement el = Streams.parse(in);
//					Class<? extends T> runtimeType = DefaultRuntimeTypeAdapterFactory.getRuntimeType(el, type);
//					if (runtimeType == null) {
//						throw new ParseException("RuntimeTypeAdapter: Unable to parse runtime type.");
//					}
//
//					T object = null;
//					try {
//						object = runtimeType.newInstance();
//					} catch (IllegalAccessException e) {
//						throw new RuntimeException("RuntimeTypeAdapter: Runtime class '" + runtimeType.getName() + "' does not have a visible default contructor.");
//					} catch (InstantiationException e) {
//						throw new RuntimeException("RuntimeTypeAdapter: Runtime class '" + runtimeType.getName() + "' does not have a default contructor.");
//					}
//					
//					// Inject Configuration
//					if (el.isJsonObject()) {
//						JsonObject jsonObject = el.getAsJsonObject();
//						// Loop over configurable fields for customization
//						List<Field> configurableFields = new ArrayList<Field>();
//						AnnotationUtil.getAllFieldsWithAnnotation(configurableFields, runtimeType, Configurable.class);
//						for (Field field : configurableFields) {
//							// TODO: check that object doesn't have field of it's own type (to prevent infinite recursion)
//							String fieldJsonName = field.getAnnotation(Configurable.class).name();
//							if ("".equals(fieldJsonName)) {
//								fieldJsonName = field.getName();
//							}
//							boolean currentAccessibility = field.isAccessible();
//							try {
//								field.setAccessible(true);
//								if (jsonObject.has(fieldJsonName)) {
//									field.set(object, gson.fromJson(jsonObject.get(fieldJsonName), field.getGenericType()));
//								}
//							} catch (IllegalArgumentException e) {
//								Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
//							} catch (IllegalAccessException e) {
//								Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
//							} finally {
//								field.setAccessible(currentAccessibility);
//							}
//						}
//					} 
//					
//					// Inject Context
//					List<Field> contextFields = new ArrayList<Field>();
//					AnnotationUtil.getAllFieldsOfType(contextFields, runtimeType, Context.class);
//					for (Field field : contextFields) {
//						boolean currentAccessibility = field.isAccessible();
//						field.setAccessible(true);
//						try {
//							field.set(object, context);
//						} catch (IllegalArgumentException e) {
//							Log.e(LogUtil.TAG, "Bad access of Context fields!!", e);
//						} catch (IllegalAccessException e) {
//							Log.e(LogUtil.TAG, "Bad access of Context fields!!", e);
//						}
//						field.setAccessible(currentAccessibility);
//					}
//					
//					return object;
//				}
//				
//			};
//		}
//		return null;
//	}
//	
//
//}
