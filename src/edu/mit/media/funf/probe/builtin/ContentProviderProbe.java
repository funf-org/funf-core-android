/**
 * 
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
 * 
 */
package edu.mit.media.funf.probe.builtin;



import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.AnyCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.BooleanCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.DoubleCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.HashedCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.IntCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.LongCell;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.StringCell;
import edu.mit.media.funf.security.HashUtil;
import edu.mit.media.funf.util.LogUtil;

@Schedule.DefaultSchedule(interval=3600)
public abstract class ContentProviderProbe extends ImpulseProbe {

	private Gson gson;
	
	@Override
	protected void onStart() {
		super.onStart();
		gson = getGson();
		for (JsonObject data : parseCursorResults()) {
			if (data != null) {
				BigDecimal customTimestamp = getTimestamp(data);
				if (customTimestamp != null) {
					data.addProperty(TIMESTAMP, customTimestamp);
				}
				sendData(data);
			}
		}
		stop();
	}

	

	private Iterable<JsonObject> parseCursorResults() {
        return new Iterable<JsonObject>() {
			@Override
			public Iterator<JsonObject> iterator() {
				return new ContentProviderIterator();
			}
        	
        };
	}
	
	class ContentProviderIterator implements Iterator<JsonObject> {

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
			Log.v(LogUtil.TAG, "cursor returned " + count +" result");
		}
		
		@Override
		public boolean hasNext() {
			//Log.d(TAG, "Checking has next");
			boolean hasNext = brandNew ? c.moveToFirst() : !(c.isClosed() || c.isLast() || c.isAfterLast());
			if (!hasNext)
				c.close();
			return hasNext;
		}

		@Override
		public JsonObject next() {
			if (brandNew) {
				c.moveToFirst();
				brandNew = false;
			} else {
				c.moveToNext();
			}
			JsonObject data = null;
			try { 
				data =  parseData(c, projection, projectionMap);
			} catch (CursorIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			} finally {
				if (!hasNext() && !c.isClosed()) {
					//Log.d(TAG, "CLOSING cursor");
					c.close();
				}
			}
			return data;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	protected abstract Map<String,CursorCell<?>> getProjectionMap();
	
	protected abstract Cursor getCursor(String[] projection);
	
	protected BigDecimal getTimestamp(JsonObject data) {
		return null;
	}
	
	protected JsonObject parseData(Cursor cursor, String[] projection, Map<String,CursorCell<?>> projectionMap) {
		JsonObject data = new JsonObject();
    	for (String key : projection) {
    		CursorCell<?> cursorCell = projectionMap.get(key);
    		if (cursorCell != null) {
	    		Object value = cursorCell.getData(cursor, key);
	    		if (value != null) {
	    			data.add(key, gson.toJsonTree(value));
	    		}
    		}
    	}
    	return data;
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
	
	protected CursorCell<String> sensitiveStringCell() {
		return new SensitiveCell(stringCell()); // TODO: do standard normalizing of string (i.e. remove everything bug word chars, and lower case)
	}
	
	protected class SensitiveCell extends CursorCell<String> {
		// TODO: Possible return a json object, instead of a string
		private CursorCell<String> upstreamCell = stringCell();
		
		public SensitiveCell(CursorCell<String> upstreamCell) {
			this.upstreamCell = upstreamCell;
		}
		
		@Override
		public String getData(Cursor cursor, int columnIndex) {
			return sensitiveData(upstreamCell.getData(cursor, columnIndex));
		}
		
	}
	
	
	protected static CursorCell<String> hashedStringCell(Context context) {
		return new HashedCell(context, stringCell());
	}
	


	public static abstract class CursorCell<T> {
		public abstract T getData(Cursor cursor, int columnIndex);
		public T getData(Cursor cursor, String columnName) {
			int index = cursor.getColumnIndex(columnName);
			if (index < 0) {
				return null; // Different devices have different columns available
			}
			return getData(cursor, cursor.getColumnIndex(columnName));
		}
		
		public static class StringCell extends CursorCell<String> {
			public String getData(Cursor cursor, int columnIndex) {
				return cursor.getString(columnIndex);
			}
		}
		
		public static class BooleanCell extends CursorCell<Boolean> {
			public Boolean getData(Cursor cursor, int columnIndex) {
				return cursor.getInt(columnIndex) != 0;
			}
		}
		
		public static class ShortCell extends CursorCell<Short> {
			public Short getData(Cursor cursor, int columnIndex) {
				return cursor.getShort(columnIndex);
			}
		}
		
		public static class IntCell extends CursorCell<Integer> {
			public Integer getData(Cursor cursor, int columnIndex) {
				return cursor.getInt(columnIndex);
			}
		}
		
		public static class LongCell extends CursorCell<Long> {
			public Long getData(Cursor cursor, int columnIndex) {
				return cursor.getLong(columnIndex);
			}
		}
		
		public static class FloatCell extends CursorCell<Float> {
			public Float getData(Cursor cursor, int columnIndex) {
				return cursor.getFloat(columnIndex);
			}
		}
		public static class DoubleCell extends CursorCell<Double> {
			public Double getData(Cursor cursor, int columnIndex) {
				return cursor.getDouble(columnIndex);
			}
		}
		
		/**
		 * Always return null.  Used to block data from being returned.
		 */
		public static class NullCell extends CursorCell<Object> {
			@Override
			public Object getData(Cursor cursor, int columnIndex) {
				return null;
			}
		}
		
		/**
		 * Try every type until success
		 */
		public static class AnyCell extends CursorCell<Object> {
			@Override
			public Object getData(Cursor cursor, int columnIndex) {
				if (cursor.isNull(columnIndex)) {
					return null;
				}
				try { return cursor.getShort(columnIndex); } catch (Exception e) {}
				try { return cursor.getInt(columnIndex); } catch (Exception e) {}
				try { return cursor.getLong(columnIndex); } catch (Exception e) {}
				try { return cursor.getFloat(columnIndex); } catch (Exception e) {}
				try { return cursor.getDouble(columnIndex); } catch (Exception e) {}
				try { return cursor.getString(columnIndex); } catch (Exception e) {}
				// Not returning blobs to contain size of bundles and prevent FAILED BINDER TRANSACTION
				//try { return cursor.getBlob(columnIndex); } catch (Exception e) {}
				return null;
			}
		}
		
		public static class PhoneNumberCell extends StringCell {
			@Override
			public String getData(Cursor cursor, int columnIndex) {
				return HashUtil.formatPhoneNumber(super.getData(cursor, columnIndex));
			}
		}
		
		/**
		 * TODO: redo this to pass in the hash strategy
		 *
		 */
		public static class HashedCell extends CursorCell<String> {
			
			private final CursorCell<?> upstreamCell;
			private final Context context;
			
			public HashedCell(Context context, CursorCell<?> upstreamCell) {
				this.upstreamCell = upstreamCell;
				this.context = context;
			}
			
			public String getData(Cursor cursor, int columnIndex) {
				Object value = upstreamCell.getData(cursor, columnIndex);
				return (value == null) ? "" : HashUtil.hashString(context, String.valueOf(value));
			}
		}
	}

}
