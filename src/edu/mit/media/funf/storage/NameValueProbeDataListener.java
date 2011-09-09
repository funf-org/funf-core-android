package edu.mit.media.funf.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import edu.mit.media.funf.OppProbe;

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
