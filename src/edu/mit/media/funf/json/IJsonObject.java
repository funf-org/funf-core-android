package edu.mit.media.funf.json;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * An immutable JsonObject that orders keys alphabetically for consistency in
 * serialization.  This contains no mutation functions, and only contains immutable JsonElements in the tree.
 * 
 * @author Alan Gardner
 */
public class IJsonObject extends JsonElement {
	/**
	 * The immutable map of entries
	 */
	private final SortedMap<String, JsonElement> members;

	public IJsonObject(IJsonObject jsonObject) {
		this.members = jsonObject.members;
	}
	
	public IJsonObject(JsonObject jsonObject) {
		if (jsonObject == null) {
			throw new IllegalStateException("Cannot create null IJsonObject");
		}
		SortedMap<String, JsonElement> map = new TreeMap<String, JsonElement>();
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			map.put(entry.getKey(), JsonUtils.immutable(entry.getValue()));
		}
		members = Collections.unmodifiableSortedMap(map);
	}

	@Override
	public boolean isJsonObject() {
		return true;
	}
	
	@Override
	public JsonObject getAsJsonObject() {
		JsonObject jsonObject = new JsonObject();
		for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
			jsonObject.add(entry.getKey(), entry.getValue());
		}
		return jsonObject;
	}

	/**
	 * Returns a set of members of this object. The set is ordered alphabetically.
	 * 
	 * @return a set of members of this object.
	 */
	public Set<Map.Entry<String, JsonElement>> entrySet() {
		return Collections.unmodifiableSet(members.entrySet());
	}

	/**
	 * Convenience method to check if a member with the specified name is
	 * present in this object.
	 * 
	 * @param memberName
	 *            name of the member that is being checked for presence.
	 * @return true if there is a member with the specified name, false
	 *         otherwise.
	 */
	public boolean has(String memberName) {
		return members.containsKey(memberName);
	}

	/**
	 * Returns the member with the specified name.
	 * 
	 * @param memberName
	 *            name of the member that is being requested.
	 * @return the member matching the name. Null if no such member exists.
	 */
	public JsonElement get(String memberName) {
		if (members.containsKey(memberName)) {
			JsonElement member = members.get(memberName);
			return member == null ? JsonNull.INSTANCE : member;
		}
		return null;
	}

	/**
	 * Convenience method to get the specified member as a JsonPrimitive
	 * element.
	 * 
	 * @param memberName
	 *            name of the member being requested.
	 * @return the JsonPrimitive corresponding to the specified member.
	 */
	public JsonPrimitive getAsJsonPrimitive(String memberName) {
		return (JsonPrimitive) members.get(memberName);
	}

	/**
	 * Convenience method to get the specified member as a JsonArray.
	 * 
	 * @param memberName
	 *            name of the member being requested.
	 * @return the JsonArray corresponding to the specified member.
	 */
	public IJsonArray getAsJsonArray(String memberName) {
		return (IJsonArray) members.get(memberName);
	}

	/**
	 * Convenience method to get the specified member as a JsonObject.
	 * 
	 * @param memberName
	 *            name of the member being requested.
	 * @return the JsonObject corresponding to the specified member.
	 */
	public IJsonObject getAsJsonObject(String memberName) {
		return (IJsonObject) members.get(memberName);
	}

	@Override
	public boolean equals(Object o) {
		return (o == this) || (o instanceof IJsonObject 
				&& ((IJsonObject) o).members.equals(members));
	}

	@Override
	public int hashCode() {
		return members.hashCode();
	}

	private String toStringCache = null;
	@Override
	public String toString() {
		// Since this is immutable, the string result can be cached
		// Does not need to be synchronized, last one is kept
		if (toStringCache == null) {
			toStringCache = super.toString();
		}
		return toStringCache;
	}
	
	
}
