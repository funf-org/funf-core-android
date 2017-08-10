/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
	
	@SuppressWarnings("unchecked")
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
