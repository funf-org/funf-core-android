package edu.mit.media.hd.funf.probe.builtin;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;
import edu.mit.media.hd.funf.HashUtil;
import edu.mit.media.hd.funf.probe.Probe;

public class BrowserProbe extends Probe {

	public static final String BOOKMARKS = "bookmarks";
	public static final String BOOKMARK = "bookmark";
	public static final String BOOKMARK_ID = "_id";
	public static final String BOOKMARK_TITLE = "title";
	public static final String BOOKMARK_URL = "url";
	public static final String BOOKMARK_VISITS = "visits";
	public static final String BOOKMARK_DATE = "date";
	public static final String BOOKMARK_CREATED = "created";
	public static final String BOOKMARK_DESCRIPTION = "description";
	public static final String SEARCHES = "searches";
	public static final String SEARCH = "search";
	public static final String SEARCH_ID = "_id";
	public static final String SEARCH_DATE = "date";
	
	private static final String BROWSER_PROBE_SETTINGS = "browser_probe_settings";
	private static final String LAST_SCAN_DATE = "lastScanDate";
	
	private long lastScanDate;
	private Bundle lastBookmarks;
	private Bundle lastSearches;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 3600L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.READ_HISTORY_BOOKMARKS
		};
	}

	@Override
	protected void onEnable() {
		SharedPreferences prefs = this.getSharedPreferences(BROWSER_PROBE_SETTINGS, MODE_PRIVATE);
		lastScanDate = prefs.getLong(LAST_SCAN_DATE, 0L);
	}

	@Override
	protected void onDisable() {
		
	}
	
	private void saveLastScanDate() {
		SharedPreferences prefs = this.getSharedPreferences(BROWSER_PROBE_SETTINGS, MODE_PRIVATE);
		if (prefs.getLong(LAST_SCAN_DATE, 0L) != lastScanDate) {
			prefs.edit().putLong(LAST_SCAN_DATE, lastScanDate).commit(); 
		}
	}

	@Override
	protected void onRun(Bundle params) {
		lastBookmarks = readBookmarks(lastScanDate);
		lastSearches = readSearches(lastScanDate);
		lastScanDate = System.currentTimeMillis();
		sendProbeData();
		saveLastScanDate(); // Save afterward, in case sending is unsuccessful
	}

	@Override
	protected void onStop() {
		// Nothing to do
	}

	@Override
	public void sendProbeData() {
		if (lastBookmarks != null && lastSearches != null) {
			Bundle data = new Bundle();
			data.putBundle(BOOKMARKS, lastBookmarks);
			data.putBundle(SEARCHES, lastSearches);
			sendProbeData(lastScanDate, new Bundle(), data);
		}
	}
	
	

	private Bundle readBookmarks(long lastScanDate){
		String[] projection = new String[] {
				BOOKMARK_ID,
				BOOKMARK_TITLE,
				BOOKMARK_URL,
				BOOKMARK_VISITS,
				BOOKMARK_DATE,
				BOOKMARK_CREATED,
				BOOKMARK_DESCRIPTION,
				BOOKMARK
		};
        Cursor c = getContentResolver().query(
                  Browser.BOOKMARKS_URI,
                  projection,
                  BOOKMARK_DATE + " > ?",
                  new String[] {String.valueOf(lastScanDate)},
                  BOOKMARK_DATE);

		Log.i(TAG, "cursor return " + c.getCount()+ " result");
		
		Bundle bundles = new Bundle();
		
    	if (c.moveToFirst()){ //false if empty
        	do{
        		Bundle b = new Bundle();
        		b.putInt(BOOKMARK_ID, c.getInt(c.getColumnIndex("_id")));
                String temp = c.getString(c.getColumnIndex("title"));
                String encoded = (temp != null ? HashUtil.hashString(this, temp) : "");
        		b.putString(BOOKMARK_TITLE, encoded);
        		temp = c.getString(c.getColumnIndex("url"));
                encoded = (temp != null ? HashUtil.hashString(this, temp) : "");
                b.putString(BOOKMARK_URL, encoded);
        		b.putInt(BOOKMARK_VISITS, c.getInt(c.getColumnIndex("visits")));
        		b.putLong(BOOKMARK_DATE, c.getLong(c.getColumnIndex("date")));
        		b.putLong(BOOKMARK_CREATED, c.getLong(c.getColumnIndex("created")));
        	    temp = c.getString(c.getColumnIndex("description"));
                encoded = (temp != null ? HashUtil.hashString(this, temp) : "");
        		b.putString(BOOKMARK_DESCRIPTION, encoded);
        		b.putInt(BOOKMARK, c.getInt(c.getColumnIndex("bookmark")));
        		bundles.putBundle("bundle " + c.getPosition(), b);
        	} while(c.moveToNext());
        }
        c.close();
		return bundles;
   	}
	
	private Bundle readSearches(long lastScanDate){
		Cursor c = getContentResolver().query(
                  Browser.SEARCHES_URI,
                  null,
                  "date>" + lastScanDate,
                  null,
                  null);

		Log.i(TAG, "cursor return " + c.getCount() + " result");
		
		Bundle bundles = new Bundle();
    	
		if (c.moveToFirst()){ //false if empty
        	do{
        		Bundle b = new Bundle();
        		b.putInt(SEARCH_ID, c.getInt(c.getColumnIndex("_id")));
        		String temp = c.getString(c.getColumnIndex("search"));
                String encoded = (temp != null ? HashUtil.hashString(this, temp) : "");
        		b.putString(SEARCH, encoded);
        		b.putLong(SEARCH_DATE, c.getLong(c.getColumnIndex("date")));
        		bundles.putBundle("bundle " + c.getPosition(), b);
        	} while(c.moveToNext());
        }
        c.close();
		return bundles;
   	}
	

}
