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
package edu.mit.media.funf.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StatFs;
import android.test.AndroidTestCase;
import android.util.Log;

public class PrefsWriteSpeedTest extends AndroidTestCase {

	public static final String TAG = "FunfTest";
	
	public void testSpeed() {
		long now = System.currentTimeMillis();
		for (int i=0; i< 100; i++) {
			SharedPreferences prefs = getContext().getSharedPreferences("ASDF", Context.MODE_PRIVATE);
			prefs.edit().clear().putString("TEST_KEY", "TEST_VALUE").commit();
		}
		long total = System.currentTimeMillis() - now;
		Log.i(TAG, "Total time: " + total + "ms");
	}
	
	public void testBlockSize() {
		StatFs stats = new StatFs("/");
		Log.i(TAG, "Root block size:" + stats.getBlockSize());
		stats.restat("/sdcard");
		Log.i(TAG, "SDCard block size:" + stats.getBlockSize());
	}
}
