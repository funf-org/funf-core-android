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

import edu.mit.media.funf.util.StringUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.test.AndroidTestCase;
import static edu.mit.media.funf.util.AsyncSharedPrefs.async;

public class AsyncSharedPrefsTest extends AndroidTestCase {

	public static final String TEST_PREFS = "TEST";
	
	private SharedPreferences innerPrefs;
	private SharedPreferences prefs;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("Clearing prefs");
		innerPrefs = getContext().getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE);
		prefs = async(getContext().getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE));
		prefs.edit().clear().commit();
	}


	public void testEdit() {
		prefs.edit().putBoolean("test1", true).commit();
		assertTrue(prefs.getBoolean("test1", false));
		assertTrue((Boolean)prefs.getAll().get("test1"));
		
		prefs.edit().putFloat("test2", 1.2f).commit();
		assertEquals(1.2f, prefs.getFloat("test2", 0.0f));
		
		prefs.edit().putInt("test3", 1).commit();
		assertEquals(1, prefs.getInt("test3", 0));
		
		prefs.edit().putLong("test4", 1L).commit();
		assertEquals(1L, prefs.getLong("test4", 0L));
		
		prefs.edit().putString("test5", "test").commit();
		assertEquals("test", prefs.getString("test5", ""));
		
		System.out.println("Entries: " + StringUtil.join(prefs.getAll().keySet(), ", "));
		
		assertEquals(5, prefs.getAll().size());
	}
	
	public void testEditMultiple() {
		prefs.edit().putBoolean("boolean", false).putString("string", "value").putInt("int", 3).commit();
		assertEquals(false, prefs.getBoolean("boolean", true));
		assertEquals("value", prefs.getString("string", ""));
		assertEquals(3, prefs.getInt("int", 0));
	}
	
	public void testNoCommit() {
		prefs.edit().putBoolean("boolean", true).commit();
		SharedPreferences.Editor editor = prefs.edit().putBoolean("boolean", false); // no commit
		assertEquals(true, prefs.getBoolean("boolean", false));
		editor.commit();
	}
	
	public void testDiskPersistenceDelay() throws InterruptedException {
		prefs.edit().putBoolean("boolean", true).commit();
		assertEquals(false, innerPrefs.getBoolean("boolean", false)); // not committed to disk yet
		assertEquals(true, prefs.getBoolean("boolean", false));
		Thread.sleep(1000);
		assertEquals(true, innerPrefs.getBoolean("boolean", false)); // committed later
	}
	
	private String keyUpdated, valueUpdated;
	public void testListeners() throws InterruptedException {
		keyUpdated = valueUpdated = null;
		OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				assertSame(prefs, sharedPreferences);
				keyUpdated = key;
				valueUpdated = sharedPreferences.getString(key, null);
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);
		
		prefs.edit().putString("listener_test", "text_value").commit();
		Thread.sleep(1000L);
		assertEquals("listener_test", keyUpdated);
		assertEquals("text_value", valueUpdated);
		
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		prefs.edit().putString("listener_test", "another_value").commit();
		keyUpdated = valueUpdated = null;
		
		Thread.sleep(1000L);
		assertNull(keyUpdated);
	}
	
	public void testMultipleConsecutiveCommits() throws InterruptedException {
		prefs.edit().putString("test", "1").commit();
		prefs.edit().putString("test", "2").commit();
		prefs.edit().putString("test", "3").commit();
		prefs.edit().putString("test", "4").commit();
		prefs.edit().putString("test", "5").commit();
		prefs.edit().putString("test", "6").commit();
		prefs.edit().putString("test", "7").commit();
		assertEquals("7", prefs.getString("test", ""));
		Thread.sleep(1000L);
		assertEquals("7", prefs.getString("test", ""));
	}
}
