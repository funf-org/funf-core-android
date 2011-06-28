/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

public final class Utils {

	public static final String TAG = Utils.class.getName();
	
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
		if (strings.isEmpty()) {
			return "";
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
	 * Convenience function for concatenating two arrays
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
	

	public static Bundle[] copyBundleArray(Parcelable[] parcelables) {
		if (parcelables == null) {
			return new Bundle[0];
		}
		Bundle[] bundles = new Bundle[parcelables.length];
		System.arraycopy(parcelables, 0, bundles, 0, parcelables.length);
		return bundles;
	}
	
	public static Map<String,Object> getValues(final Bundle bundle) {
		HashMap<String, Object> values = new HashMap<String, Object>();
		for (String key : bundle.keySet()) {
			values.put(key, bundle.get(key));
		}
		return values;
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
