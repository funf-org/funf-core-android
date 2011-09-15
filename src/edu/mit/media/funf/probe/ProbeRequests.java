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
package edu.mit.media.funf.probe;

import static edu.mit.media.funf.AsyncSharedPrefs.async;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.ProbeExceptions.UnstorableTypeException;

/**
 * Stores all of the requests that have come in for a probe.
 * Stores only the most recent per requester.
 * @author alangardner
 *
 */
public class ProbeRequests {
	
	private static final String TAG = ProbeRequests.class.getName();
	public static final String PROBE_NAME_PREF_KEY = "_PROBE_NAME";
	
	private SharedPreferences prefs;
	
	private ProbeRequests(final Context context, final String name) {
		this.prefs = async(context.getSharedPreferences(name, Context.MODE_PRIVATE));
		if (!prefs.contains(PROBE_NAME_PREF_KEY)) {
			prefs.edit().putString(PROBE_NAME_PREF_KEY, name).commit();
		}
	}

	public static ProbeRequests getRequestsForProbe(final Context context, final SharedPreferences probePrefs) {
		String name = probePrefs.getString(ProbeRequests.PROBE_NAME_PREF_KEY, null);
		if (name == null) {
			Log.e("FUNF_Schedule", "Unable to find probe name in preferences");
			return null;
		} else {
			return getRequestsForProbe(context, name);
		}
	}
	
	/**
	 * Returns the underlying storage container so components can register for changes
	 * 
	 * TODO: should encapsulate this
	 * @return
	 */
	public SharedPreferences getSharedPreferences() {
		// TODO: consider encapsulating this and creating an listener interface for probe requests
		// instead of just easy access to shared prefs
		return prefs;
	}
	
	/**
	 * Factory method for getting the ProbeRequests for a given probe name.
	 * TODO: could be used for caching access to requests
	 * @param context
	 * @param probeName
	 * @return
	 */
	public static ProbeRequests getRequestsForProbe(final Context context, final String probeName) {
		return new ProbeRequests(context, probeName);
	}
	
	private String getKey(final String requester, final String requestId, final String paramName, final int index) {
		return paramName + "__" + requester + "__" + requestId + "__" + index;
	}
	
	private boolean isRequesterKey(final String key) {
		return key.contains("__");
	}
	
	private String getRequester(final String key) {
		return key.split("__")[1];
	}
	
	private String getRequestId(final String key) {
		return key.split("__")[2];
	}
	
	private String getParamName(final String key) {
		return key.split("__")[0];
	}
	
	private String getIndex(final String key) {
		return key.split("__")[3];
	}
	
	/**
	 * @return The name of the Probe these requests are for
	 */
	public String getName() {
		return prefs.getString(PROBE_NAME_PREF_KEY, null);
	}
	
	/**
	 * Marshal a probe data request bundle from a requester to persistent storage
	 * @param bundle
	 * @return true if request was succesfully stored, false otherwise
	 */
	public boolean put(final String requester, String requestId, final Bundle... bundles) {
		if (requester == null) {
			return false;
		}
		requestId = (requestId==null) ? "" : requestId;
		
		// Remove existing first
		remove(requester, requestId);

		SharedPreferences.Editor editor = this.prefs.edit();
		// TODO: what to do with no requester attribute?  Should it be required in OPP?
		// TODO: what about empty bundle requests?
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (bundle.isEmpty()) {
				Utils.putInPrefs(editor, getKey(requester, requestId, "", i), "");
			}
			for (String paramName : bundle.keySet()) {
				Object value = bundle.get(paramName);
				Log.i(TAG, paramName + " = " + value.toString());
				if (value != null) {
					try {
						Utils.putInPrefs(editor, getKey(requester, requestId, paramName, i), value);
					} catch (UnstorableTypeException e) {
						Log.e(TAG, e.getLocalizedMessage());
						return false;
					}
				}
			}
		}
		editor.commit();
		return true;
	}
	
	private Set<String> keysForRequester(final String requester, final String requestId) {
		Set<String> keys = new HashSet<String>();
		for (String key : prefs.getAll().keySet()) {
			if (key.contains("__" + requester + "__" + requestId)) {
				keys.add(key);
			}
		}
		return keys;
	}
	
	/**
	 * Delete the current request from the current requester
	 * @param requester
	 */
	public void remove(final String requester, final String requestId) {
		SharedPreferences.Editor editor = this.prefs.edit();
		for (String key : keysForRequester(requester, requestId)) {
			editor.remove(key);
		}
		editor.commit();
	}
	
	private static <V> V getOrCreate(List<V> list, int index, Class<? extends V> defaultType) {
		while(list.size() <= index) {
			list.add(null);
		}
		V value = list.get(index);
		if (value == null) {
			try {
				value = defaultType.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Unable to create instance of type: " + defaultType.getName(), e);
			}
			list.set(index, value);
		}
		return value;
	}
	
	private static <K,V> V getOrCreate(Map<K,V> map, K key, Class<? extends V> defaultType) {
		V value = map.get(key);
		if (value == null) {
			try {
				value = (V) defaultType.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Unable to create instance of type: " + defaultType.getName(), e);
			}
			map.put(key, value);
		}
		return value;
	}
	
	// The following classes are used to get around the limitations of generics
	// You cannot specify HashMap<String,List<Bundle>>.class due to type erasure
	@SuppressWarnings("serial")
	static class RequestsById extends HashMap<String,List<Bundle>> {}
	@SuppressWarnings("serial")
	static class RequestList extends ArrayList<Bundle> {}
	
	public Map<String,Map<String,List<Bundle>>> getByRequesterByRequestId() {
		Map<String,Map<String,List<Bundle>>> requests = new HashMap<String, Map<String,List<Bundle>>>();
		for(Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
			if (isRequesterKey(entry.getKey())) {
				final String requester = getRequester(entry.getKey());
				final String requestId = getRequestId(entry.getKey());
				final int index = Integer.parseInt(getIndex(entry.getKey()));
				final String paramName = getParamName(entry.getKey());
				Map<String,List<Bundle>> requestsForRequester = getOrCreate(requests, requester, RequestsById.class);
				List<Bundle> requestsForId = getOrCreate(requestsForRequester, requestId, RequestList.class);
				Bundle bundle = getOrCreate(requestsForId, index, Bundle.class);
					/*
					Map<String,List<Bundle>> requestsForRequester = requests.get(requester);
					if (requestsForRequester == null) {
						requestsForRequester = new HashMap<String, List<Bundle>>();
						requests.put(requester, requestsForRequester);
					}
					List<Bundle> requestsForId = requestsForRequester.get(requestId);
					if (requestsForId == null) {
						requestsForId = new ArrayList<Bundle>();
						requestsForRequester.put(requestId, requestsForId);
					}
					while(requestsForId.size() < index) {
						requestsForId.add(null);
					}
					Bundle bundle = requestsForId.get(index);
					if (bundle == null) {
						bundle = new Bundle();
						requestsForId.set(index, bundle);
					}*/ 

				// Ignore parameters that exist for empty bundles
				if (paramName.length() > 0) {
					Utils.putInBundle(bundle, paramName, entry.getValue());
				}
			}
		}
		return requests;
	}
	
	/**
	 * Get all stored requests for the probe
	 * @return
	 */
	public Set<Bundle> getAll() {
		Set<Bundle> allBundles = new HashSet<Bundle>();
		for (Map<String,List<Bundle>> requestIdToBundles : getByRequesterByRequestId().values()) {
			for (List<Bundle> bundles : requestIdToBundles.values()) {
				allBundles.addAll(bundles);
			}
		}
		return allBundles;
	}
	
}
