package edu.mit.media.funf.probe;

import android.os.Bundle;
import edu.mit.media.funf.Utils;

public abstract class SynchronousProbe extends Probe {

	private long mostRecentTimestamp;
	private Bundle mostRecentData;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 3600L),
			new Parameter(SystemParameter.START, 0L),
			new Parameter(SystemParameter.END, 0L)
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
		mostRecentData = null; // Prevent sending old data with new timestamp
		mostRecentTimestamp = getTimestamp();
		mostRecentData = getData();
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		if (mostRecentData != null) {
			sendProbeData(mostRecentTimestamp, new Bundle(), mostRecentData);
		}
	}

	/**
	 * Build and return the data that will get sent out as a data broadcast.
	 * @return
	 */
	protected abstract Bundle getData();
	
	/**
	 * Return the timestamp that should be sent with data
	 * @return
	 */
	protected long getTimestamp() {
		return Utils.getTimestamp();
	}
	
}
