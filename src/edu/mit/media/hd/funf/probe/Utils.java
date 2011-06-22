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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

public final class Utils {

	private static final String TAG = Utils.class.getName();
	
	/**
	 * Should not be instantiated.  Used only as a namespace for static Utils functions
	 */
	private Utils() {
		
	}
	
	
	private static String getStoredBundleParamKey(final String key, final String paramKey) {
		return key + "__" + paramKey;
	}
	
	private static boolean isStoredBundleParamKey(final String key, final String storedParamKey) {
		final String prefix = key + "__";
		return key.startsWith(prefix);
	}
	
	private static String getBundleParamKey(final String key, final String storedParamKey) {
		final String prefix = key + "__";
		assert key.startsWith(prefix);
		return storedParamKey.substring(prefix.length());
	}
	
	/**
	 * Convenience function for adding the object form of primitives to a SharedPreferences
	 * @param editor
	 * @param key
	 * @param value Must be Boolean, Float, Integer, Long, String, or Bundle (bundles must only contain the same)
	 * @return
	 * @throws UnstorableTypeException
	 */
	public static SharedPreferences.Editor putInPrefs(SharedPreferences.Editor editor, String key, Object value) throws UnstorableTypeException {
		Class<?> valueClass = value.getClass();
		if (Boolean.class.isAssignableFrom(valueClass)) {
			editor.putBoolean(key, ((Boolean)value).booleanValue());
		} else if (Integer.class.isAssignableFrom(valueClass)) {
			editor.putInt(key, ((Integer) value).intValue());
		} else if (Float.class.isAssignableFrom(valueClass)) {
			editor.putFloat(key, ((Float) value).floatValue());
		} else if (Long.class.isAssignableFrom(valueClass)) {
			editor.putLong(key, ((Long) value).longValue());
		} else if (String.class.isAssignableFrom(valueClass)) {
			editor.putString(key, ((String) value));
		} else if (Bundle.class.isAssignableFrom(valueClass)) {
			// Serialize the bundle using the key as a prefix
			Bundle bundle = ((Bundle) value);
			for (String bundleKey : bundle.keySet()) {
				Object bundleValue = bundle.get(bundleKey);
				putInPrefs(editor, getStoredBundleParamKey(key, bundleKey), bundleValue);
			}
		} else {
			throw new UnstorableTypeException(valueClass);
		}
		return editor;
	}
	
	/**
	 * Parse a bundle from data stored in a SharedPreferences
	 * @param prefs
	 * @param key
	 * @return
	 */
	public static Bundle getBundleFromPrefs(SharedPreferences prefs, String key) {
		Bundle bundle = new Bundle();
		Map<String,?> prefsMap = prefs.getAll();
		for (String prefsKey : prefsMap.keySet()) {
			if (isStoredBundleParamKey(key, prefsKey)) {
				try {
					putInBundle(bundle, getBundleParamKey(key, prefsKey), prefsMap.get(prefsKey));
				} catch (UnstorableTypeException e) {
					Log.e(TAG, "Should never happen, since SharedPrefs stores only types Bundle can store: " + e.getLocalizedMessage());
				}
			}
		}
		return bundle;
	}
	
	/**
	 * Convenience function for putting the object form of primitives into a Bundle
	 * @param bundle
	 * @param key
	 * @param value Must be Boolean, Float, Integer, Long, or String
	 * @throws UnstorableTypeException
	 */
	public static void putInBundle(Bundle bundle, String key, Object value) throws UnstorableTypeException {
		Class<?> valueClass = value.getClass();
		if (Boolean.class.isAssignableFrom(valueClass)) {
			bundle.putBoolean(key, ((Boolean)value).booleanValue());
		} else if (Integer.class.isAssignableFrom(valueClass)) {
			bundle.putInt(key, ((Integer) value).intValue());
		} else if (Float.class.isAssignableFrom(valueClass)) {
			bundle.putFloat(key, ((Float) value).floatValue());
		} else if (Long.class.isAssignableFrom(valueClass)) {
			bundle.putLong(key, ((Long) value).longValue());
		} else if (String.class.isAssignableFrom(valueClass)) {
			bundle.putString(key, ((String) value));
		} else {
			throw new UnstorableTypeException(valueClass);
		}
	}
	
	
	/**
	 * Convenience function for joining strings using a delimeter
	 * @param strings
	 * @param delimeter
	 * @return
	 */
	public static String join(final Collection<?> strings, String delimeter) {
		if (delimeter == null) {
			delimeter = ",";
		}
		StringBuffer joined = new StringBuffer();
		Iterator<?> stringIter = strings.iterator();
		joined.append(stringIter.next().toString());
		while (stringIter.hasNext()) {
			joined.append(delimeter);
			joined.append(stringIter.next().toString());
		}
		return joined.toString();
	}
	
	/**
	 * Conveneicne function for concatenating two arrays
	 * @param <T>
	 * @param first
	 * @param second
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = (T[])new Object[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	////////////////////////////
	// OPP
	////////////////////////////
	
	/**
	 * @return OPP status action
	 */
	public static String getStatusAction() {
		// TODO: make this an OPP name
		return "edu.mit.hd.funf.STATUS";
	}

	/**
	 * @return OPP Status request action
	 */
	public static String getStatusRequestAction() {
		// TODO: make this an OPP name
		return "edu.mit.hd.funf.REQUEST_STATUS";
	}
	
	/**
	 * @return OPP Status request action
	 */
	public static String getStatusRequestAction(Class<? extends Probe> probeClass) {
		// TODO: make this an OPP name
		return probeClass.getName() + ".REQUEST_STATUS";
	}
	
	/**
	 * @param probeClass
	 * @return OPP Data action for probe with class
	 */
	public static String getDataAction(Class<? extends Probe> probeClass) {
		return getDataAction(probeClass.getName());
	}
	
	public static String getDataAction(String probeName) {
		return probeName + ".DATA";
	}
	
	public static boolean isDataAction(final String action) {
		return action != null && action.endsWith(".DATA");
	}
	
	public static String getProbeName(final String action) {
		if (action == null) {
			return null;
		}
		final String actionString = action.endsWith(".DATA") ? ".DATA" 
				: action.endsWith(".GET") ? ".GET" 
				: null;
		return (actionString == null) ? null 
				: action.substring(0, action.length() - actionString.length());
	}
	
	public static Map<String,Object> getValues(final Bundle bundle) {
		HashMap<String, Object> values = new HashMap<String, Object>();
		for (String key : bundle.keySet()) {
			values.put(key, bundle.get(key));
		}
		return values;
	}
	
	/**
	 * @param probeClass
	 * @return  OPP Data action for probe with class
	 */
	public static String getDataRequestAction(Class<? extends Probe> probeClass) {
		return probeClass.getName() + ".GET";
	}
	
	public static boolean isStatusRequest(final String action) {
		return getStatusRequestAction().equals(action);
	}
	
	public static boolean isDataRequest(final String action) {
		return action != null && action.endsWith(".GET");
	}
	
	/**
	 * Scans the manifest for registered services, returning the set that are instances of Probe
	 * @param context
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<Class<? extends Probe>> getAvailableProbeClasses(Context context) {
		Set<Class<? extends Probe>> probes = new HashSet<Class<? extends Probe>>();
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SERVICES);
			for (ServiceInfo serviceInfo : info.services) {
				try {
					Class<?> probeServiceClass = Class.forName(serviceInfo.name);
					if (Probe.class.isAssignableFrom(probeServiceClass)) {
						probes.add((Class<? extends Probe>) probeServiceClass);
					}
				} catch (ClassNotFoundException e) {
					Log.e(TAG, e.getLocalizedMessage());
				}
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		return probes;
	}
	
	/**
	 * Get the class instance for the probe specified by the action, from the set of probe classes passed in.
	 * @param probeClasses
	 * @param action
	 * @return probe class that matches action, or null if it doesn't exist
	 */
	public static Class<? extends Probe> getProbeClass(final Set<Class<? extends Probe>> probeClasses, final String action) {
		// TODO: deal with registration of more than just class name for action
		// work with interfaces as well, e.g. the common OPP interfaces
		for (Class<? extends Probe> probeClass : probeClasses) {
			if (action != null && action.startsWith(probeClass.getName())) {
				return probeClass;
			}
		}
		return null;
	}
	

	/**
	 * Convenience function for returning an empty string array if null is returned
	 * @param array
	 * @return
	 */
	public static String[] nonNullStrings(String[] array) {
		return (array == null) ? new String[] {} : array;
	}
	
	public static PowerManager.WakeLock getWakeLock(Context context) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().getName());
		lock.acquire();
		return lock;
	}
}
