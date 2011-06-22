package edu.mit.media.hd.funf.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.probe.Utils;

public class ProbeDataListener extends BroadcastReceiver {

	private final String databaseName;
	private final Class<? extends DatabaseService> databaseServiceClass;
	private final BundleSerializer bundleSerializer;
	
	public ProbeDataListener(String databaseName, Class<? extends DatabaseService> databaseServiceClass, BundleSerializer bundleSerializer) {
		this.databaseName = databaseName;
		this.databaseServiceClass = databaseServiceClass;
		this.bundleSerializer = bundleSerializer;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Utils.isDataAction(action)) {
			String dataJson = bundleSerializer.serialize(intent.getExtras());
			String probeName = Utils.getProbeName(action);
			long timestamp = intent.getLongExtra("TIMESTAMP", 0L);
			Bundle b = new Bundle();
			b.putString(DatabaseService.DATABASE_NAME_KEY, databaseName);
			b.putLong(DatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(DatabaseService.NAME_KEY, probeName);
			b.putString(DatabaseService.VALUE_KEY, dataJson);
			Intent i = new Intent(context, databaseServiceClass);
			i.putExtras(b);
			Log.i(getClass().getName(), "Starting db service: " + probeName);
			context.startService(i);
		}
	}
}
