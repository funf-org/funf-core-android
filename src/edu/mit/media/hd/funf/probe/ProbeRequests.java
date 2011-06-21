/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

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
		this.prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		prefs.edit().putString(PROBE_NAME_PREF_KEY, name).commit();
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
	
	private String getKey(final String requester, final String paramName) {
		return paramName + "__" + requester;
	}
	
	private boolean isRequesterKey(final String key) {
		return key.contains("__");
	}
	
	private String getRequester(final String key) {
		return key.split("__")[1];
	}
	
	private String getParamName(final String key) {
		return key.split("__")[0];
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
	public boolean put(final Bundle bundle) {
		if (bundle == null) {
			return false;
		}
		final String requester = bundle.getString(SystemParameter.REQUESTER.name);
		if (requester == null) {
			return false;
		}

		SharedPreferences.Editor editor = this.prefs.edit();
		// TODO: what to do with no requester attribute?  Should it be required in OPP?
		for (String paramName : bundle.keySet()) {
			Object value = bundle.get(paramName);
			if (value != null) {
				try {
					Utils.putInPrefs(editor, getKey(requester, paramName), value);
				} catch (UnstorableTypeException e) {
					Log.i(TAG, e.getLocalizedMessage());
					return false;
				}
			}
		}
		editor.commit();
		return true;
	}
	
	private Set<String> keysForRequester(final String requester) {
		Set<String> keys = new HashSet<String>();
		for (String key : prefs.getAll().keySet()) {
			if (key.endsWith("__" + requester)) {
				keys.add(key);
			}
		}
		return keys;
	}
	
	/**
	 * Delete the current request from the current requester
	 * @param requester
	 */
	public void remove(final String requester) {
		SharedPreferences.Editor editor = this.prefs.edit();
		for (String key : keysForRequester(requester)) {
			editor.remove(key);
		}
		editor.commit();
	}
	
	/**
	 * Get all stored requests for the probe
	 * @return
	 */
	public Set<Bundle> getAll() {
		Map<String,Bundle> bundles = new HashMap<String,Bundle>();
		for(Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
			if (isRequesterKey(entry.getKey())) {
				final String requester = getRequester(entry.getKey());
				final String paramName = getParamName(entry.getKey());
				Bundle bundle =  bundles.get(requester);
				if (bundle == null) {
					bundle = new Bundle();
					bundles.put(requester, bundle);
				}
				try {
					Utils.putInBundle(bundle, paramName, entry.getValue());
				} catch (UnstorableTypeException e) {
					Log.e("Funf.Schedule", "Unsupported type stored in preferences.");
				}
			}
		}
		return new HashSet<Bundle>(bundles.values());
	}
	
}
