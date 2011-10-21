package edu.mit.media.funf.probe;

import java.util.Collection;

import android.content.Intent;
import android.os.Bundle;

public interface ProbeScheduler {
	
	/**
	 * Returns true if probe should be enabled, otherwise false.
	 * @param probe
	 * @param requests
	 * @return
	 */
	public boolean shouldBeEnabled(Probe probe, Collection<Intent> requests);
	
	/**
	 * Returns Bundle of parameters the probe should use to run now.  If null is returned, probe should not run.
	 * This method takes care of scheduling intents to stop the probe at the correct time.
	 * @param probe
	 * @param requests
	 */
	public Bundle startRunningNow(Probe probe, Collection<Intent> requests);
	

	/**
	 * Schedules an alarm for the next time this probe should be run.
	 * Returns a timestamp of the next run time, or null if not scheduled.
	 * @param probe
	 * @param requests
	 * @return
	 */
	public Long scheduleNextRun(Probe probe, Collection<Intent> requests);
}
