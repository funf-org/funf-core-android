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
package edu.mit.media.funf.opp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import edu.mit.media.funf.storage.BundleSerializer;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;

public class NameValueProbeDataListener extends BroadcastReceiver {
	
	private final String databaseName;
	private final Class<? extends DatabaseService> databaseServiceClass;
	private final BundleSerializer bundleSerializer;
	
	public NameValueProbeDataListener(String databaseName, Class<? extends DatabaseService> databaseServiceClass, BundleSerializer bundleSerializer) {
		this.databaseName = databaseName;
		this.databaseServiceClass = databaseServiceClass;
		this.bundleSerializer = bundleSerializer;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (OppProbe.isDataAction(action)) {
			String dataJson = bundleSerializer.serialize(intent.getExtras());
			String probeName = OppProbe.getProbeName(action);
			long timestamp = intent.getLongExtra("TIMESTAMP", 0L);
			Bundle b = new Bundle();
			b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, databaseName);
			b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(NameValueDatabaseService.NAME_KEY, probeName);
			b.putString(NameValueDatabaseService.VALUE_KEY, dataJson);
			Intent i = new Intent(context, databaseServiceClass);
			i.setAction(DatabaseService.ACTION_RECORD);
			i.putExtras(b);
			context.startService(i);
		}
	}
}
