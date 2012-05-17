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

import java.lang.reflect.Type;
import java.util.Arrays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.mit.media.funf.Schedule.DefaultSchedule;

public class DefaultScheduleSerializer implements JsonSerializer<DefaultSchedule> {
	
	private static final String 
		VALUE = "value",
		INTERVAL = "interval",
		DURATION = "duration",
		OPPORTUNISTIC = "opportunistic",
		STRICT = "strict";
	
	static {
		// Ensure expected methods exist
		for (String methodName : Arrays.asList(VALUE, INTERVAL, DURATION, OPPORTUNISTIC, STRICT)) {
			try {
				DefaultSchedule.class.getMethod(methodName);
			} catch (SecurityException e) {
				throw new RuntimeException("Default schedule does not have expected accessible method.");
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Default schedule does not have expected accessible method.", e);
			}
		}
	}
	
	@Override
	public JsonElement serialize(DefaultSchedule src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jsonObject = new JsonObject();
		// TODO: figure out what to do with value
		if (!"".equals(src.value())) {
			jsonObject.addProperty(VALUE, src.value());
		}
		jsonObject.addProperty(RuntimeTypeAdapterFactory.TYPE, src.type().getName());
		jsonObject.addProperty(INTERVAL, src.interval());
		jsonObject.addProperty(DURATION, src.duration());
		jsonObject.addProperty(OPPORTUNISTIC, src.opportunistic());
		jsonObject.addProperty(STRICT, src.strict());
		return jsonObject;
	}

}
