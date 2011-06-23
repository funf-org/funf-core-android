package edu.mit.media.hd.funf.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.mit.media.hd.funf.Utils;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_VALUE = "value";
	public static final Table DATA_TABLE = new Table("data", 
			Arrays.asList(new Column(COLUMN_NAME, "TEXT"), // ACTION from data broadcast
					      new Column(COLUMN_TIMESTAMP, "INTEGER"), // TIMESTAMP in data broadcast
					      new Column(COLUMN_VALUE, "TEXT"))); // JSON representing 
	
	public DatabaseHelper(Context context, String name, int version) {
		super(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATA_TABLE.getCreateTableSQL());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Nothing yet
	}
	
	
	// TODO: Consider moving these to an external utils class
	/**
	 * Immutable Table Definition
	 */
	public static class Table {
		private static final String CREATE_TABLE_FORMAT = "CREATE TABLE %s (_id INTEGER primary key autoincrement, %s);";
		
		public final String name;
		private final List<Column> columns;
		public Table(final String name, final List<Column> columns) {
			this.name = name;
			this.columns = new ArrayList<Column>(columns);
		}
		public List<Column> getColumns() { return new ArrayList<Column>(columns); }
		public String getCreateTableSQL() {
			return String.format(CREATE_TABLE_FORMAT, name, Utils.join(columns, ", "));
		}
	}

	/**
	 * Immutable Column Definition
	 *
	 */
	public static class Column {
		public final String name, type;
		public Column(final String name, final String type) {
			this.name = name;
			this.type = type;
		}
		@Override
		public String toString() {
			return name + " " + type;
		}
	}

}
