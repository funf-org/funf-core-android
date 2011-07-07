package edu.mit.media.hd.funf.probe;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import edu.mit.media.hd.funf.HashUtil;

public abstract class CursorCell<T> {
	public abstract T getData(Cursor cursor, int columnIndex);
	public T getData(Cursor cursor, String columnName) {
		return getData(cursor, cursor.getColumnIndex(columnName));
	}
	
	public static class StringCell extends CursorCell<String> {
		public String getData(Cursor cursor, int columnIndex) {
			return cursor.getString(columnIndex);
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