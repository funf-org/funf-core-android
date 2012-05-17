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
	public static final TypeToken<Map<String,Schedule>> SCHEDULES_FIELD_TYPE_TOKEN = new TypeToken<Map<String,Schedule>>(){};
	public static final String SCHEDULE = "@schedule";
	
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
			for (Map.Entry<String,JsonElement> entry : el.entrySet()) {
				JsonElement entryEl = entry.getValue();
				if (entryEl.isJsonObject()) {
					JsonObject subConfig = entryEl.getAsJsonObject();
					if (subConfig.has(SCHEDULE)) {
						JsonElement scheduleConfig = subConfig.remove(SCHEDULE);
						annotatedSchedules.add(entry.getKey(), scheduleConfig);
					}
				}
			}
			
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
	}

	@Override
	public <T> Class<? extends T> getRuntimeType(JsonElement el, TypeToken<T> type) {
		return delegate.getRuntimeType(el, type);
	}
}
