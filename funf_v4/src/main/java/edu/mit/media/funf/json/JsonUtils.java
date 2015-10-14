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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class JsonUtils {

	private JsonUtils() {}
	
	private static final Comparator<Entry<String,JsonElement>> JSON_OBJECT_ENTRY_SET_COMPARATOR = new Comparator<Map.Entry<String,JsonElement>>() {
		@Override
		public int compare(Entry<String, JsonElement> lhs, Entry<String, JsonElement> rhs) {
			return lhs==null ?
		    		 (rhs==null ? 0 : -1) :
		    			 (rhs==null ? 1 : lhs.getKey().compareTo(rhs.getKey()));
		}
	};
	
	/**
	 * Sort all elements in Json objects, to ensure that they are identically serialized.
	 * @param el
	 * @return
	 */
	public static JsonElement deepSort(JsonElement el) {
		if (el == null) {
			return null;
		} else if (el.isJsonArray()) {
			JsonArray sortedArray = new JsonArray();
			for (JsonElement subEl : (JsonArray)el) {
				sortedArray.add(deepSort(subEl));
			}
			return sortedArray;
		} else if (el.isJsonObject()){
			List<Entry<String,JsonElement>> entrySet = new ArrayList<Entry<String,JsonElement>>(((JsonObject)el).entrySet());
			Collections.sort(entrySet, JSON_OBJECT_ENTRY_SET_COMPARATOR);
			JsonObject sortedObject = new JsonObject();
			for (Entry<String,JsonElement> entry : entrySet) {
				sortedObject.add(entry.getKey(), deepSort(entry.getValue()));
			}
			return sortedObject;
		} else {	
			return el;
		} 
	}
	
	/**
	 * Returns a deep copy of the JsonElement.  This is not thread safe.
	 * @param el
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> T deepCopy(T el) {
		if (el == null) {
			return (T)JsonNull.INSTANCE;
		} else if (el.isJsonArray()) {
			JsonArray array = new JsonArray();
			for (JsonElement subEl : el.getAsJsonArray()) {
				array.add(deepCopy(subEl));
			}
			return (T)array;
		} else if (el.isJsonObject()) {
			JsonObject object = new JsonObject();
			for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
				object.add(entry.getKey(), deepCopy(entry.getValue()));
			}
			return (T)object;
		} else {
			return (T)el;
		}
	}

	/**
	 * Return an immutable JsonElement.  Either IJsonObject, IJsonArray, or other immutable JsonPrimitive.
	 * @param el
	 * @return
	 */
	public static JsonElement immutable(JsonElement el) {
		if (el instanceof JsonObject) {
			return new IJsonObject(el.getAsJsonObject());
		} else if (el instanceof JsonArray) {
			return new IJsonArray(el.getAsJsonArray());
		} else if (el instanceof JsonPrimitive && el.getAsJsonPrimitive().isNumber()) {
			// Ensure that all LazilyParsedNumbers have been parsed into a consistent Number type.
			// Otherwise hash codes are not consistent, because LazilyParsedNumbers are never seen as integral.
			return new JsonPrimitive(el.getAsBigDecimal());
		}
		return el;
	}
	
	/**
	 * In place copy of one objects values onto another, with option to replace existing values in copy.
	 * @param source
	 * @param destination
	 * @param replace
	 */
	public static void deepCopyOnto(JsonObject source, JsonObject destination, boolean replace) {
		if (source == null || destination == null) {
			throw new RuntimeException("Both source and destination must exist while copying values from one to another.");
		}
		JsonObject sourceCopy = new JsonParser().parse(source.toString()).getAsJsonObject(); // Defensive copy to ensure the source is never modified
		deepCopyOnto(sourceCopy, destination, replace, true);
	}
	
	private static void deepCopyOnto(JsonObject source, JsonObject destination, boolean replace, boolean initial) {
		for (Map.Entry<String, JsonElement> sourceVal : source.entrySet()) {
			if (replace || !destination.has(sourceVal.getKey())) {
				String key = sourceVal.getKey();
				JsonElement value = sourceVal.getValue();
				if (value.isJsonObject() && destination.has(key) && destination.get(key).isJsonObject()) {
					deepCopyOnto(value.getAsJsonObject(), destination.get(key).getAsJsonObject(), replace, false);
				} else {
					destination.add(key, value);
				}
			}
		}
	}
	
	public static final Gson GSON = new Gson();
	
	public static final String JSON_SCHEME = "json";
	
	/**
	 * Transform json into an immutable Uri
	 * @param el
	 * @return
	 */
	public static Uri toUri(JsonElement el) {
		return new Uri.Builder()
		.scheme(JSON_SCHEME)
		.appendPath(immutable(el).toString())
		.build();
	}
	
	/**
	 * Parse json data from a json:// uri
	 * @param uri
	 * @return
	 */
	public static JsonElement fromUri(Uri uri) {
		return JSON_SCHEME.equals(uri.getScheme()) ?
				new JsonParser().parse(uri.getPath()) :
					null;
	}
}
