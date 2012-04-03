/**
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
 */
package edu.mit.media.funf.configured;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import edu.mit.media.funf.EqualsUtil;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.ProbeExceptions.UnstorableTypeException;

/**
 * A convenience interface to access a Funf configuration stored in a SharedPreferences.
 * All data is stored in the SharedPreferences, and all edits follow the same transaction 
 * model as SharedPreferences. 
 *
 */
public class FunfConfig implements OnSharedPreferenceChangeListener {
	public static final String 
		NAME_KEY = "name",
		VERSION_KEY = "version",
		CONFIG_UPDATE_URL_KEY = "configUpdateUrl",
		CONFIG_UPDATE_PERIOD_KEY = "configUpdatePeriod",
		DATA_UPLOAD_URL_KEY = "dataUploadUrl",
		DATA_UPLOAD_PERIOD_KEY = "dataUploadPeriod",
		DATA_UPLOAD_ON_WIFI_ONLY_KEY = "dataUploadOnWifiOnly",		
		DATA_ARCHIVE_PERIOD_KEY = "dataArchivePeriod",
		DATA_REQUESTS_KEY = "dataRequests";
	public static final long 
		DEFAULT_VERSION = 0,
		DEFAULT_DATA_ARCHIVE_PERIOD = 3 * 60 * 60,  // 3 hours
		DEFAULT_DATA_UPLOAD_PERIOD = 6 * 60 * 60,  // 6 hours
		DEFAULT_CONFIG_UPDATE_PERIOD = 1 * 60 * 60; // 1 hour
	public static final boolean
		DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = false;
	public static final String DEFAULT_DATA_REQUESTS = "{}"; // No requests
	
	
	private final SharedPreferences prefs;
	
	private FunfConfig(SharedPreferences prefs) {
		assert prefs != null;
		this.prefs = prefs;
		prefs.registerOnSharedPreferenceChangeListener(this);
		dataRequests = new HashMap<String, Bundle[]>();
	}
	
	private static final Map<SharedPreferences, FunfConfig> instances = new HashMap<SharedPreferences, FunfConfig>();
	public static FunfConfig getInstance(SharedPreferences prefs) {
		FunfConfig config = instances.get(prefs);
		if (config == null) {
			synchronized (instances) {
				// Check one more time when we are synchronized
				config = instances.get(prefs);
				if (config == null) {
					config = new FunfConfig(prefs);
					instances.put(prefs, config);
				}
			}
		}
		return config;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (sharedPreferences == prefs && isDataRequestKey(key)) {
			synchronized (dataRequests) {
				dataRequests.remove(keyToProbename(key));
			}
		}
	}
		
	public String getName() {
		return prefs.getString(NAME_KEY, null);
	}

	public long getVersion() {
		return prefs.getLong(VERSION_KEY, DEFAULT_VERSION);
	}

	public String getConfigUpdateUrl() {
		return prefs.getString(CONFIG_UPDATE_URL_KEY, null);
	}

	public long getConfigUpdatePeriod() {
		return prefs.getLong(CONFIG_UPDATE_PERIOD_KEY, DEFAULT_CONFIG_UPDATE_PERIOD);
	}

	public String getDataUploadUrl() {
		return prefs.getString(DATA_UPLOAD_URL_KEY, null);
	}
	
	public boolean getDataUploadOnWifiOnly() {
		return prefs.getBoolean(DATA_UPLOAD_ON_WIFI_ONLY_KEY, DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY);
	}

	public long getDataUploadPeriod() {
		return prefs.getLong(DATA_UPLOAD_PERIOD_KEY, DEFAULT_DATA_UPLOAD_PERIOD);
	}

	public long getDataArchivePeriod() {
		return prefs.getLong(DATA_ARCHIVE_PERIOD_KEY, DEFAULT_DATA_ARCHIVE_PERIOD);
	}

	private Map<String, Bundle[]> dataRequests; // cache
	/**
	 * Returns a copy of the data requests that can be modified by the users, 
	 * without affecting the configuration object.
	 * @return
	 */
	public  Map<String, Bundle[]> getDataRequests() {
		Set<String> probeNames = prefs.getAll().keySet();
		synchronized (dataRequests) {
			// Make sure all keys have been cached
			for (String key : probeNames) {
				if (isDataRequestKey(key)) {
					String probeName = keyToProbename(key);
					if (!dataRequests.containsKey(probeName)) {
						getDataRequests(probeName);
					}
				}
			}
			return deepCopy(dataRequests); // Deep copy so users can modify
		}
	}

	public Bundle[] getDataRequests(String probeName) {
		synchronized (dataRequests) {
			// Check to see if it is cached first
			if (dataRequests.containsKey(probeName)) {
				return dataRequests.get(probeName);
			}
	
			// Check to see if we have a key for this
			String jsonString = prefs.getString(probeNameToKey(probeName), null);
			if (jsonString == null) {
				return null;
			}

			// If so parse value and store in cache
			try {
				JSONArray jsonArray = new JSONArray(jsonString);
				Bundle[] requests = getBundleArray(jsonArray);
				dataRequests.put(probeName, requests);
				return requests;
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private Map<String, Bundle[]> deepCopy(Map<String, Bundle[]> original) {
		Map<String,Bundle[]> copy = new HashMap<String, Bundle[]>();
		for (Map.Entry<String, Bundle[]> entry : original.entrySet()) {
			Bundle[] originalBundleArray = entry.getValue();
			Bundle[] copyBundleArray = new Bundle[originalBundleArray.length];
			for (int i=0; i<originalBundleArray.length; i++) {
				copyBundleArray[i] = new Bundle(originalBundleArray[i]);
			}
			copy.put(entry.getKey(), copyBundleArray);
		}
		return copy;
	}
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	

	public Editor edit() {
		return new Editor();
	}
	
	public class Editor {

		private SharedPreferences.Editor editor = getPrefs().edit();
		private Set<String> changedProbes = new HashSet<String>();
		private boolean clear = false;
		
		
		public Editor setName(String name) {
			editor.putString(NAME_KEY, name);
			return this;
		}
		
		public Editor setVersion(int version) {
			editor.putInt(VERSION_KEY, version);
			return this;
		}

		public Editor setConfigUpdateUrl(String configUpdateUrl) {
			editor.putString(CONFIG_UPDATE_URL_KEY, configUpdateUrl);
			return this;
		}

		public Editor setConfigUpdatePeriod(long configUpdatePeriod) {
			editor.putLong(CONFIG_UPDATE_PERIOD_KEY, configUpdatePeriod);
			return this;
		}

		public Editor setDataUploadUrl(String dataUploadUrl) {
			editor.putString(DATA_UPLOAD_URL_KEY, dataUploadUrl);
			return this;
		}
		
		public Editor setDataUploadOnWifiOnly(boolean dataUploadOnWifiOnly) {
			editor.putBoolean(DATA_UPLOAD_ON_WIFI_ONLY_KEY, dataUploadOnWifiOnly);
			return this;
		}

		public Editor setDataUploadPeriod(long dataUploadPeriod) {
			editor.putLong(DATA_UPLOAD_PERIOD_KEY, dataUploadPeriod);
			return this;
		}

		public Editor setDataArchivePeriod(long dataArchivePeriod) {
			editor.putLong(DATA_ARCHIVE_PERIOD_KEY, dataArchivePeriod);
			return this;
		}
		
		public Editor setDataRequests(Map<String, Bundle[]> dataRequests) {
			// Remove all of the items that don't exist in the new data requests
			for (String existingProbeName : getDataRequests().keySet()) {
				if (!dataRequests.containsKey(existingProbeName)) {
					editor.remove(probeNameToKey(existingProbeName));
					changedProbes.add(existingProbeName);
				}
			}
			for (Map.Entry<String, Bundle[]> dataRequestEntry : dataRequests.entrySet()) {
				setDataRequest(dataRequestEntry.getKey(), dataRequestEntry.getValue());
			}
			return this;
		}
		
		public Editor setDataRequest(String probeName, Bundle[] requests) {
			if (!EqualsUtil.areEqual(getDataRequests(probeName), requests)) {
				if (requests == null || requests.length == 0) {
					editor.remove(probeNameToKey(probeName));
				} else {
					editor.putString(probeNameToKey(probeName), toJSONArray(requests).toString());
				}
				changedProbes.add(probeName);
			}
			return this;
		}
		
		
		private void setString(JSONObject jsonObject, String key) {
			String value = jsonObject.optString(key, null);
			if (value == null) {
				editor.remove(key);
			} else {
				editor.putString(key, value);
			}
		}
		
		private void setBoolean(JSONObject jsonObject, String key) {
			if (jsonObject.has(key)) {
				editor.putBoolean(key, jsonObject.optBoolean(key));
			} else {
				editor.remove(key);
			}
		}
		
		private void setPositiveLong(JSONObject jsonObject, String key) {
			long value = jsonObject.optLong(key, 0L);
			if (value <= 0) {
				editor.remove(key);
			} else {
				editor.putLong(key, value);
			}
		}
		
		public Editor setAll(String jsonString) throws JSONException {
			JSONObject jsonObject = new JSONObject(jsonString);
			editor.clear();
			clear = true;
			setString(jsonObject, NAME_KEY);
			setPositiveLong(jsonObject, VERSION_KEY);
			setString(jsonObject, CONFIG_UPDATE_URL_KEY);
			setPositiveLong(jsonObject, CONFIG_UPDATE_PERIOD_KEY);
			setString(jsonObject, DATA_UPLOAD_URL_KEY);
			setBoolean(jsonObject, DATA_UPLOAD_ON_WIFI_ONLY_KEY);
			setPositiveLong(jsonObject, DATA_UPLOAD_PERIOD_KEY);
			setPositiveLong(jsonObject, DATA_ARCHIVE_PERIOD_KEY);
			
			// Add new probe requests
			JSONObject requestsJsonObject = jsonObject.getJSONObject(DATA_REQUESTS_KEY);
			Iterator requestsIterator = requestsJsonObject.keys();
			while (requestsIterator.hasNext()) {
				String probeName = (String)requestsIterator.next();
				JSONArray value = requestsJsonObject.getJSONArray(probeName);
				editor.putString(probeNameToKey(probeName), value.toString());
			}
			
			return this;
		}
		
		public Editor setAll(FunfConfig otherConfig) {
			editor.clear();
			clear = true;
			for (Map.Entry<String, ?> entry : otherConfig.getPrefs().getAll().entrySet()) {
				if (entry.getValue() != null) {
					Utils.putInPrefs(editor, entry.getKey(), entry.getValue());
				}
			}
			return this;
		}
		
		public Editor clear() {
			editor.clear();
			clear = true;
			return this;
		}
		
		public boolean commit() {
			if (clear || !changedProbes.isEmpty()) {
				synchronized (dataRequests) {
					if (clear) {
						dataRequests.clear();
					} else {
						for (String changedProbeName : changedProbes) {
							dataRequests.remove(changedProbeName);
						}
					}
					return editor.commit(); // Commit in synchronized block to prevent stale data request caches
				}
			} else {
				return editor.commit(); // Don't need to synchronize
			}
		}
	}
	
	public static boolean isDataRequestKey(String key) {
		return key != null && key.startsWith(DATA_REQUESTS_KEY);
	}
	
	public static String probeNameToKey(String probeName) {
		return DATA_REQUESTS_KEY + probeName;
	}
	
	public static String keyToProbename(String key) {
		return key.substring(DATA_REQUESTS_KEY.length());
	}
	
	private static JSONObject getDataRequestJsonObject(Map<String, Bundle[]> dataRequestMap) {
		JSONObject dataRequestsJson = new JSONObject();
		try {
			for (Map.Entry<String, Bundle[]> dataReqest : dataRequestMap.entrySet()) {
					dataRequestsJson.put(dataReqest.getKey(), toJSONArray(dataReqest.getValue()));
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return dataRequestsJson;
	}
	
	private static Map<String, Bundle[]> getDataRequestMap(JSONObject dataRequestsObject) {
		Map<String, Bundle[]> dataRequestMap = new HashMap<String, Bundle[]>();
		Iterator<String> probeNames = dataRequestsObject.keys();
		try {
			while (probeNames.hasNext()) {
				String probeName = probeNames.next();
				JSONArray requestsJsonArray = dataRequestsObject.getJSONArray(probeName);
				dataRequestMap.put(probeName, getBundleArray(requestsJsonArray));
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return dataRequestMap;
	}
	

	private static Bundle[] getBundleArray(JSONArray jsonArray) throws JSONException {
		Bundle[] request = new Bundle[jsonArray.length()];
		for (int i = 0; i < jsonArray.length(); i++) {
			request[i] = getBundle(jsonArray.getJSONObject(i));
		}
		return request;
	}
	
	private static JSONArray toJSONArray(Bundle[] bundles) {
		JSONArray jsonArray = new JSONArray();
		for (Bundle bundle : bundles) {
			jsonArray.put(toJSONObject(bundle));
		}
		return jsonArray;
	}
	
	@SuppressWarnings("unchecked")
	private static Bundle getBundle(JSONObject jsonObject) throws JSONException {
		Bundle requestPart = new Bundle();
		Iterator<String> paramNames = jsonObject.keys();
		while (paramNames.hasNext()) {
			String paramName = paramNames.next();
			try  {
			Utils.putInBundle(requestPart, paramName, jsonObject.get(paramName));
			} catch (UnstorableTypeException e) {
				throw new JSONException(e.getLocalizedMessage());
			}
		}
		return requestPart;
	}
	
	
	
	@Override
	public boolean equals(Object o) {
		return o != null 
		&& o instanceof FunfConfig 
		&& (prefs == ((FunfConfig)o).prefs // prefs is singleton
				|| prefs.getAll().equals(((FunfConfig)o).prefs.getAll())); // All internal values are the same
	}

	@Override
	public int hashCode() {
		return prefs.hashCode();
	}

	@Override
	public String toString() {
		try {
			return toJsonObject().toString();
		} catch (JSONException e) {
			// Swallowed to prevent crashes in debugger
			return super.toString();
		}
	}
	
	public String toString(boolean prettyPrint) {
		try {
			return prettyPrint ? toJsonObject().toString(4) : toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	JSONObject toJsonObject() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		JSONObject dataRequests = new JSONObject();
		jsonObject.put(DATA_REQUESTS_KEY, dataRequests);
		for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (isDataRequestKey(key)) {
				if (value != null) {
					String probeName = keyToProbename(key);
					dataRequests.put(probeName, new JSONArray((String)value));
				}
			} else {
				jsonObject.put(key, value);
			}
		}
		return jsonObject;
	}

	private static JSONObject toJSONObject(Bundle bundle) {
		return new JSONObject(Utils.getValues(bundle));
	}
	
	

}
