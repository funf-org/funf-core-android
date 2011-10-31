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
package edu.mit.media.funf;

import static edu.mit.media.funf.AsyncSharedPrefs.async;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;
import edu.mit.media.funf.probe.ProbeExceptions.UnstorableTypeException;

public final class Utils {

	public static final String TAG = "Funf";
	
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
		if (value == null) {
			editor.putString(key, null);
			return editor;
		}
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
					Log.e(TAG, "Should never happen, since SharedPrefs stores only types Bundle can store: ",  e);
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
		} else if (Short.class.isAssignableFrom(valueClass)) {
				bundle.putShort(key, ((Short) value).shortValue());
		} else if (Integer.class.isAssignableFrom(valueClass)) {
			bundle.putInt(key, ((Integer) value).intValue());
		} else if (Long.class.isAssignableFrom(valueClass)) {
			bundle.putLong(key, ((Long) value).longValue());
		} else if (Float.class.isAssignableFrom(valueClass)) {
			bundle.putFloat(key, ((Float) value).floatValue());
		} else if (Double.class.isAssignableFrom(valueClass)) {
			bundle.putDouble(key, ((Double) value).doubleValue());
		}  else if (String.class.isAssignableFrom(valueClass)) {
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
		if (bundle == null) {
			return values;
		}
		for (String key : bundle.keySet()) {
			values.put(key, bundle.get(key));
		}
		return values;
	}
	
	public static <T extends Parcelable> ArrayList<T> getArrayList(Bundle bundle, String key) {
		if (bundle == null) {
			return null;
		}
        Object o = bundle.get(key);
        try {
        	return (ArrayList<T>) o;
        } catch (ClassCastException e) {
        	try {
        		return new ArrayList<T>(Arrays.asList((T[])o));
        	} catch (ClassCastException e2) {
        		Log.w(TAG, "Unable to succesfully parse ArrayList from '" + key + "'");
                return null;
			}
        }
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


	/**
	 * Useful function to coerce value to a long, no matter what type of object is in the bundle
	 * @param bundle
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static long getLong(Bundle bundle, String key, long defaultValue) {
		Object value = bundle.get(key);
		if (value instanceof Long) {
			return (Long)value;
		} else if (value instanceof Integer) {
			return ((Integer)value).longValue();
		} else if (value instanceof String) {
			try {
				return Long.valueOf((String)value);
			} catch (NumberFormatException e) {
				// We did our best, value is not a long
			}
		}
		return defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getCursorData(Cursor cursor, int columnIndex, Class<T> dataType) {
		if (dataType.equals(String.class)) {
			return (T)cursor.getString(columnIndex);
		} else if (dataType.equals(Short.class)) {
			return (T) Short.valueOf(cursor.getShort(columnIndex));
		} else if (dataType.equals(Integer.class)) {
			return (T) Integer.valueOf(cursor.getInt(columnIndex));
		} else if (dataType.equals(Long.class)) {
			return (T) Long.valueOf(cursor.getLong(columnIndex));
		} else if (dataType.equals(Float.class)) {
			return (T) Float.valueOf(cursor.getFloat(columnIndex));
		} else if (dataType.equals(Double.class)) {
			return (T) Double.valueOf(cursor.getDouble(columnIndex));
		} else {
			return null;
		}
	}
	
	public static final String FUNF_UTILS_PREFS = "edu.mit.media.funf.Utils";
	public static final String INSTALLATION_UUID_KEY = "INSTALLATION_UUID";
	public static String uuid = null;
	public static String getInstallationId(Context context) {
		if (uuid == null) {
			SharedPreferences prefs = async(context.getSharedPreferences(FUNF_UTILS_PREFS, Context.MODE_PRIVATE));
			uuid = prefs.getString(INSTALLATION_UUID_KEY, null);
			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
				prefs.edit().putString(INSTALLATION_UUID_KEY, uuid).commit();
			}
		}
		return uuid;
	}

	public static String getSdCardPath(Context context) {
		return new File(Environment.getExternalStorageDirectory(), context.getPackageName()) + "/";
	}
	
	/**
	 * Closes a stream, and swallows null cases our IOExceptions.
	 * @param stream
	 */
	public static boolean close(Closeable stream) {
		if(stream != null) {
			try {
				stream.close();
				return true;
			} catch (IOException e) {
				Log.e(TAG, "Error closing stream", e);
			}
		}
		return false;
	}
	
	public static long getTimestamp() {
		return millisToSeconds(System.currentTimeMillis());
	}
	
	public static long millisToSeconds(long millis) {
		return millis/1000L;
	}
	
	public static long secondsToMillis(long seconds) {
		return seconds*1000L;
	}
}
