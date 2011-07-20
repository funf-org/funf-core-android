package edu.mit.media.hd.funf.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
	public static final String COLUMN_DEVICE = "device";
	public static final String COLUMN_UUID = "uuid";
	public static final String COLUMN_CREATED = "created";
	public static final Table FILE_INFO_TABLE = new Table("file_info", 
			Arrays.asList(new Column(COLUMN_DEVICE, "TEXT"), // Hashed device id
				      	  new Column(COLUMN_UUID, "TEXT"), // Universally Unique Id for file 
					      new Column(COLUMN_CREATED, "INTEGER"))); // TIMESTAMP in data broadcast
	
	private Context context;
	
	public DatabaseHelper(Context context, String name, int version) {
		super(context, name, null, version);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATA_TABLE.getCreateTableSQL());
		db.execSQL(FILE_INFO_TABLE.getCreateTableSQL());
		
		// Insert file identifier information
		String deviceIdHash = Utils.getDeviceId(context);
		String fileUuid = UUID.randomUUID().toString();
		long createdTime = Utils.getTimestamp();
		db.execSQL(String.format("insert into %s (%s, %s, %s) values ('%s', '%s', %d)", 
				FILE_INFO_TABLE.name, 
				COLUMN_DEVICE, COLUMN_UUID, COLUMN_CREATED,
				deviceIdHash, fileUuid, createdTime));
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
