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
package edu.mit.media.funf.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.StringUtil;
import edu.mit.media.funf.util.UuidUtil;

public class NameValueDatabaseHelper extends SQLiteOpenHelper {

	public static final int CURRENT_VERSION = 1;
	
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_VALUE = "value";
	public static final Table DATA_TABLE = new Table("data", 
			Arrays.asList(new Column(COLUMN_NAME, "TEXT"), // ACTION from data broadcast
					      new Column(COLUMN_TIMESTAMP, "FLOAT"), // TIMESTAMP in data broadcast
					      new Column(COLUMN_VALUE, "TEXT"))); // JSON representing 
	public static final String COLUMN_DATABASE_NAME= "dbname";
	public static final String COLUMN_INSTALLATION = "device";
	public static final String COLUMN_UUID = "uuid";
	public static final String COLUMN_CREATED = "created";
	public static final Table FILE_INFO_TABLE = new Table("file_info", 
			Arrays.asList(new Column(COLUMN_DATABASE_NAME, "TEXT"), // Name of this database
						  new Column(COLUMN_INSTALLATION, "TEXT"), // Universally Unique Id for device installation 
				      	  new Column(COLUMN_UUID, "TEXT"), // Universally Unique Id for file 
					      new Column(COLUMN_CREATED, "FLOAT"))); // TIMESTAMP in data broadcast
	
	
	private final Context context;
	private final String databaseName;
	
	public NameValueDatabaseHelper(Context context, String name, int version) {
		super(context, name, null, version);
		this.context = context;
		this.databaseName = name;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATA_TABLE.getCreateTableSQL());
		db.execSQL(FILE_INFO_TABLE.getCreateTableSQL());
		// Insert file identifier information
		String installationUuid = UuidUtil.getInstallationId(context);
		String fileUuid = UUID.randomUUID().toString();
		double createdTime = TimeUtil.getTimestamp().doubleValue();
		db.execSQL(String.format(Locale.US, "insert into %s (%s, %s, %s, %s) values ('%s', '%s', '%s', %f)", 
				FILE_INFO_TABLE.name, 
				COLUMN_DATABASE_NAME, COLUMN_INSTALLATION, COLUMN_UUID, COLUMN_CREATED,
				databaseName, installationUuid, fileUuid, createdTime));
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
			return String.format(CREATE_TABLE_FORMAT, name, StringUtil.join(columns, ", "));
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
