/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe;

import java.util.Set;

import android.os.Bundle;

/**
 * Resolves all requests for data for a given probe, to provide the next run time and parameters.
 * @author alangardner
 *
 */
public class ProbeScheduleResolver {
	
	private Set<Bundle> requests;
	private long nextRunTime;
	private Bundle nextRunParams;

	public ProbeScheduleResolver(final Set<Bundle> requests, final Bundle defaults, final long lastRunTime, final Bundle lastRunParams) {
		this.requests = requests;
		this.nextRunTime = Long.MAX_VALUE;
		this.nextRunParams = new Bundle();
		for (Bundle request : requests) {
			Bundle completeRequest = new Bundle();
			completeRequest.putAll(defaults);
			completeRequest.putAll(request);
			long period = 1000* completeRequest.getLong(Probe.SystemParameter.PERIOD.name, 0L);
			if (period != 0L) {
				long start = completeRequest.getLong(Probe.SystemParameter.START.name, Long.MIN_VALUE);
				long end = completeRequest.getLong(Probe.SystemParameter.END.name, Long.MAX_VALUE);
				long scheduleNextTime = lastRunTime == 0 ? System.currentTimeMillis() : lastRunTime + period;
				if (scheduleNextTime > start && scheduleNextTime < end && scheduleNextTime < nextRunTime) {
					nextRunTime = scheduleNextTime;
					nextRunParams = request; // Dumb implementation which just takes parameters for that run
				}
			}
		}
	}
	
	/**
	 * Calculates the soonest due date for a probe to be run, and returns it
	 * If this method returns 0 if there is no scheduled next time to run.
	 * @param lastRunTime
	 * @return
	 */
	public long getNextRunTime() {
		return nextRunTime == Long.MAX_VALUE ? 0L : nextRunTime;
	}
	
	/**
	 * Calculates the parameters to configure the probe for the next run.
	 * @return
	 */
	public Bundle getNextRunParams() {
		return nextRunParams;
	}
}
