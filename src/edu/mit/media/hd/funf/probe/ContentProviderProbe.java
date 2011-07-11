package edu.mit.media.hd.funf.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.probe.CursorCell.AnyCell;
import edu.mit.media.hd.funf.probe.CursorCell.DoubleCell;
import edu.mit.media.hd.funf.probe.CursorCell.HashedCell;
import edu.mit.media.hd.funf.probe.CursorCell.IntCell;
import edu.mit.media.hd.funf.probe.CursorCell.LongCell;
import edu.mit.media.hd.funf.probe.CursorCell.StringCell;

public abstract class ContentProviderProbe extends Probe {

	protected ArrayList<Bundle> mostRecentScan;
	private Thread onRunThread;
	
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
		if (onRunThread == null) {
			onRunThread = new Thread(new Runnable() {
				@Override
				public void run() {
					mostRecentScan = parseCursorResults();
					sendProbeData();
					onRunThread = null;
				}
			});
			onRunThread.start();
		}
	}

	@Override
	protected void onStop() {
		if (onRunThread != null) {
			try {
				onRunThread.join(4000);
			} catch (InterruptedException e) {
				Log.e(TAG, "Didn't finish sending before probe was stopped");
			}
		}
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
	
	protected Bundle parseDataBundle(Cursor cursor, String[] projection, Map<String,CursorCell<?>> projectionMap) {
		Bundle b = new Bundle();
    	for (String key : projection) {
    		CursorCell<?> cursorCell = projectionMap.get(key);
    		if (cursorCell != null) {
	    		Object value = cursorCell.getData(cursor, key);
	    		if (value != null) {
	    			Utils.putInBundle(b, key,value);
	    		}
    		}
    	}
    	return b;
	}
	
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
            	Bundle b = parseDataBundle(c, projection, projectionMap);
            	if (b != null) {
            		bundles.add(b);
            	}
            }while(c.moveToNext()); 
        }
        c.close();
        Log.i(TAG, "returned " + bundles.size() + " bundles");
        return bundles;
	}
	
	// Convenience methods that can be used to cache and reuse CursorCell objects
	
	protected static IntCell intCell() {
		return new IntCell();
	}
	
	protected static LongCell longCell() {
		return new LongCell();
	}
	
	protected static DoubleCell doubleCell() {
		return new DoubleCell();
	}
	
	protected static StringCell stringCell() {
		return new StringCell();
	}
	
	protected static AnyCell anyCell() {
		return new AnyCell();
	}
	
	protected HashedCell hashedStringCell() {
		return hashedStringCell(this);
	}
	protected static HashedCell hashedStringCell(Context context) {
		return new HashedCell(context, stringCell());
	}
	

}
