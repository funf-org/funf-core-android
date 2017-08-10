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
