package edu.mit.media.funf.probe.builtin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@DefaultSchedule(period=300)
@RequiredPermissions(android.Manifest.permission.BATTERY_STATS)
public class BatteryProbe extends Base implements PassiveProbe {

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				sendData(getGson().toJsonTree(intent.getExtras()).getAsJsonObject());
				stop();
			}
		}
	};
	
	@Override
	protected void onStart() {
		super.onStart();
		getContext().registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	@Override
	protected void onStop() {
		super.onStop();
		getContext().unregisterReceiver(receiver);
	}
}
