package edu.mit.media.funf.probe.builtin;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;

@DefaultSchedule(period=120, duration=15)
public class ActivityProbe extends Base implements ContinuousProbe {

	@Override
	protected void onEnable() {
		super.onEnable();
		// TODO: register listener for accelerometer
	}

	@Override
	protected void onStart() {
		super.onStart();
		// TODO: run accelerometer
	}

	@Override
	protected void onStop() {
		super.onStop();
		// TODO: stop accelerometer
	}

	@Override
	protected void onDisable() {
		// TODO Auto-generated method stub
		super.onDisable();
	}

	
}
