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
package edu.mit.media.funf.json;

import java.io.IOException;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BundleTypeAdapter extends TypeAdapter<Bundle> {
	
	public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			if (Bundle.class.isAssignableFrom(type.getRawType())) {
				return (TypeAdapter<T>)new BundleTypeAdapter(gson);
			}
			return null;
		}
	};
	
	private Gson gson;
	
	public BundleTypeAdapter(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void write(JsonWriter out, Bundle value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.beginObject();
			for (String key : value.keySet()) {
				out.name(key);
				Object innerValue = value.get(key);
				if (innerValue == null) {
					out.nullValue();
				} else {
					gson.toJson(innerValue, innerValue.getClass(), out);
				}
			}
			out.endObject();
		}
	}

	@Override
	public Bundle read(JsonReader in) throws IOException {
		throw new IOException("Bundle reading not implemented");
	}
	
	/*
	@Override
	public JsonElement serialize(Bundle src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null) {
			return JsonNull.INSTANCE;
		} else {
			JsonObject jsonObject = new JsonObject();
			for (String key : src.keySet()) {
				Object innerValue = src.get(key);
				jsonObject.add(key, gson.toJsonTree(innerValue));
			}
			return jsonObject;
		}
	}
	*/

}
