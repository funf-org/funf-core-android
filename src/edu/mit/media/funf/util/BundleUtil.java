package edu.mit.media.funf.util;

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class BundleUtil {

	private BundleUtil() {
		
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
}
