package edu.mit.media.funf;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {

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
				if (value.isJsonObject() && destination.get(key).isJsonObject()) {
					deepCopyOnto(value.getAsJsonObject(), destination.get(key).getAsJsonObject(), replace, false);
				} else {
					destination.add(key, value);
				}
			}
		}
	}
}
