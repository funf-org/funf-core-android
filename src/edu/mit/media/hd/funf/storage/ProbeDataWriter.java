package edu.mit.media.hd.funf.storage;

import edu.mit.media.hd.funf.probe.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public abstract class ProbeDataWriter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Utils.isDataAction(action)) {
			String dataJson = getBundleSerializer().serialize(intent.getExtras());
			String probeName = Utils.getProbeName(action);
			long timestamp = intent.getLongExtra("TIMESTAMP", 0L);
			
			Bundle b = new Bundle();
			b.putLong(DatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(DatabaseService.NAME_KEY, probeName);
			b.putString(DatabaseService.VALUE_KEY, dataJson);
			Intent i = new Intent(context, getDatabaseService());
			i.putExtras(b);
			Log.i(getClass().getName(), "Starting db service: " + probeName);
			context.startService(i);
		}
	}
	
	public abstract Class<? extends DatabaseService> getDatabaseService();
	public abstract BundleSerializer getBundleSerializer();

}
