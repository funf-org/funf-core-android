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
package edu.mit.media.funf.probe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.CursorCell.AnyCell;
import edu.mit.media.funf.probe.CursorCell.BooleanCell;
import edu.mit.media.funf.probe.CursorCell.DoubleCell;
import edu.mit.media.funf.probe.CursorCell.HashedCell;
import edu.mit.media.funf.probe.CursorCell.IntCell;
import edu.mit.media.funf.probe.CursorCell.LongCell;
import edu.mit.media.funf.probe.CursorCell.StringCell;

public abstract class ContentProviderProbe extends Probe {

	protected Iterable<Bundle> mostRecentScan;
	private Thread onRunThread;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.PERIOD, getDefaultPeriod()),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L)
		};
	}
	
	protected long getDefaultPeriod() {
		return 3600L;
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
		Log.d(TAG, "onRun of ContentProviderProbe");
		if (onRunThread == null) {
			onRunThread = new Thread(new Runnable() {
				@Override
				public void run() {
					mostRecentScan = parseCursorResults();
					sendProbeData();
					onRunThread = null;
					stop();
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
			onRunThread = null;
		}
		disable();
	}

	private static final long THROTTLE_SLEEP_MILLIS = 10;
	private void throttle() {
		try {
			Thread.sleep(THROTTLE_SLEEP_MILLIS);
		} catch (InterruptedException e) {
			Log.e(TAG, "Throttled thread interrupted.", e);
		}
	}
	@Override
	public void sendProbeData() {
		// TODO: make this always run on separate thread
		if (mostRecentScan != null ) {
			if (sendEachRowSeparately()) {
				for (Bundle data : mostRecentScan) {
					if (data != null) {
						sendProbeData(getTimestamp(data), data);
						throttle();
					}
				}
			} else {
				ArrayList<Bundle> results = new ArrayList<Bundle>();
				
				long timestamp = 0L;
				for (Bundle item : mostRecentScan) {
					if (item != null) {
						if (timestamp == 0L) {
							timestamp = getTimestamp(item); // use first item for timestamp
						}
						results.add(item);
						throttle();
					}
					if (results.size() >= 100) {
						Bundle data = new Bundle();
						data.putParcelableArrayList(getDataName(), results);
						sendProbeData(timestamp, data);
						results = new ArrayList<Bundle>();
					}
				}
				Bundle data = new Bundle();
				data.putParcelableArrayList(getDataName(), results);
				if (timestamp == 0L) {
					timestamp = Utils.getTimestamp(); // use current time if no results
				}
				sendProbeData(timestamp, data);
			}
		}
	}
	
	protected boolean sendEachRowSeparately() {
		return false;
	}
	
	protected abstract String getDataName();
	
	protected abstract long getTimestamp(Bundle result);
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
	
	private Iterable<Bundle> parseCursorResults() {
        return new Iterable<Bundle>() {
			@Override
			public Iterator<Bundle> iterator() {
				return new ContentProviderIterator();
			}
        	
        };
	}
	
	class ContentProviderIterator implements Iterator<Bundle> {

		private final Cursor c;
		private final String[] projection;
		private final Map<String, CursorCell<?>> projectionMap;
		private boolean brandNew; // Next has not been called
		
		public ContentProviderIterator() {
			this.projectionMap = getProjectionMap();
			this.projection = new String[projectionMap.size()];
			projectionMap.keySet().toArray(projection);
			this.c = getCursor(projection);
			int count = c.getCount();
			this.brandNew = true;
			Log.v(TAG, "cursor returned " + count +" result");
		}
		
		@Override
		public boolean hasNext() {
			//Log.d(TAG, "Checking has next");
			boolean hasNext = brandNew ? c.moveToFirst() : !(c.isClosed() || c.isLast() || c.isAfterLast());
			if (!hasNext && !c.isClosed())
				c.close();
			return hasNext;
		}

		@Override
		public Bundle next() {
			if (brandNew) {
				c.moveToFirst();
				brandNew = false;
			} else {
				c.moveToNext();
			}
			Bundle dataBundle = null;
			try { 
				dataBundle =  parseDataBundle(c, projection, projectionMap);
			} catch (CursorIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			} finally {
				hasNext(); // Called to ensure the cursor is closed
			}
			return dataBundle;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	// Convenience methods that can be used to cache and reuse CursorCell objects
	
	protected static BooleanCell booleanCell() {
		return new BooleanCell();
	}
	
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
	
	protected CursorCell<String> hashedStringCell() {
		return hashedStringCell(this);
	}
	protected static CursorCell<String> hashedStringCell(Context context) {
		return new HashedCell(context, stringCell());
	}
	

}
