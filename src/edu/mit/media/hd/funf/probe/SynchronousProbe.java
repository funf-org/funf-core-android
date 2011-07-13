package edu.mit.media.hd.funf.probe;

import android.os.Bundle;

public abstract class SynchronousProbe extends Probe {

	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 3600L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	protected void onDisable() {
		// Nothing
	}

	@Override
	protected void onEnable() {
		// Nothing
	}

	@Override
	protected void onRun(Bundle params) {
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		// Nothing
	}

}
