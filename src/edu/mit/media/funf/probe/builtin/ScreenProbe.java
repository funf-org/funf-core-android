package edu.mit.media.funf.probe.builtin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ScreenKeys;

@DisplayName("Screen On/Off")
@Description("Records when the screen turns off and on.")
@DefaultSchedule(opportunistic=true)
public class ScreenProbe extends Base implements ContinuousProbe, ScreenKeys  {

	private BroadcastReceiver screenReceiver;
	
	@Override
	protected void onEnable() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		screenReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();

				JsonObject data = new JsonObject();
				if (Intent.ACTION_SCREEN_OFF.equals(action)) {
					data.addProperty(SCREEN_ON, false);
					sendData(data);
				} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
					data.addProperty(SCREEN_ON, true);
					sendData(data);
				}
			}
		};
		getContext().registerReceiver(screenReceiver, filter);
	}
	
	

	@Override
	protected void onStart() {
		super.onStart();
		stop(); // Passive Only, Don't ever keep the device awake
	}



	@Override
	protected void onDisable() {
		getContext().unregisterReceiver(screenReceiver);
	}

	
}
