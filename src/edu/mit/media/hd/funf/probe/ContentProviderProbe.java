package edu.mit.media.hd.funf.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.probe.CursorCell.HashedCell;
import edu.mit.media.hd.funf.probe.CursorCell.IntCell;
import edu.mit.media.hd.funf.probe.CursorCell.LongCell;
import edu.mit.media.hd.funf.probe.CursorCell.StringCell;

public abstract class ContentProviderProbe extends Probe {

	private ArrayList<Bundle> mostRecentScan;
	
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
	protected void onEnable() {
		// Nothing
	}

	@Override
	protected void onDisable() {
		// Nothing
	}

	@Override
	protected void onRun(Bundle params) {
		mostRecentScan = parseCursorResults();
		sendProbeData();
	}

	@Override
	protected void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		if (mostRecentScan != null ) {
			Bundle data = new Bundle();
			data.putParcelableArrayList(getDataName(), mostRecentScan);
			sendProbeData(getTimestamp(mostRecentScan), new Bundle(), data);
		}
	}
	
	protected abstract String getDataName();
	
	protected abstract long getTimestamp(List<Bundle> results);
	
	protected abstract Map<String,CursorCell<?>> getProjectionMap();
	
	protected abstract Cursor getCursor(String[] projection);
	
	private ArrayList<Bundle> parseCursorResults() {
		Map<String,CursorCell<?>> projectionMap = getProjectionMap();
		String[] projection = new String[projectionMap.size()];
		projectionMap.keySet().toArray(projection);
        Cursor c = getCursor(projection);
		Log.i(TAG, "cursor returned "+c.getCount()+" result");

    	ArrayList<Bundle> bundles = new ArrayList<Bundle>();
    	
    	//Save into bundles
        if (c.moveToFirst()){ //false if empty
            do{
            	Bundle b = new Bundle();
            	for (String key : projection) {
            		Object value = projectionMap.get(key).getData(c, key);
            		Utils.putInBundle(b, key,value);
            	}
            	bundles.add(b);
            }while(c.moveToNext()); 
        }
        c.close();
        return bundles;
	}
	
	// Convenience methods that can be used to cache and reuse CursorCell objects
	
	protected IntCell intCell() {
		return new IntCell();
	}
	
	protected LongCell longCell() {
		return new LongCell();
	}
	
	protected StringCell stringCell() {
		return new StringCell();
	}
	
	protected HashedCell hashedStringCell() {
		return new HashedCell(this, stringCell());
	}

}
