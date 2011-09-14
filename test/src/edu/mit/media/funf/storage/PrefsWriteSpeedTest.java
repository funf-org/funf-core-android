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
