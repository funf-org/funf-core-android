/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe;

import java.util.Set;

import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;

/**
 * Resolves all requests for data for a given probe, to provide the next run time and parameters.
 * @author alangardner
 *
 */
public class ProbeScheduleResolver {
	
	private static final long NO_PERIOD = 0L;
	
	private long nextRunTime;
	private Bundle nextRunParams;

	
	
	/**
	 * @param requests
	 * @param defaults
	 * @param lastRunTime in milliseconds
	 * @param lastRunParams
	 */
	public ProbeScheduleResolver(final Set<Bundle> requests, final Bundle defaults, final long lastRunTime, final Bundle lastRunParams) {
		this.nextRunTime = Long.MAX_VALUE;
		Log.i("ProbeScheduleResolver", "Number of requests: " + requests.size());
		if (!requests.isEmpty()) {
			this.nextRunParams = new Bundle();
			for (Bundle request : requests) {
				Bundle completeRequest = new Bundle();
				completeRequest.putAll(defaults);
				completeRequest.putAll(request);
				long period = Utils.secondsToMillis(Utils.getLong(completeRequest, Probe.Parameter.Builtin.PERIOD.name, NO_PERIOD));
				Log.i("ProbeScheduleResolver", "" + " Period:" + period);
				long start = Utils.getLong(completeRequest, Probe.Parameter.Builtin.START.name, Long.MIN_VALUE);
				if (start == 0L) {
					start = Long.MIN_VALUE;
				}
				long end = Utils.getLong(completeRequest, Probe.Parameter.Builtin.END.name, Long.MAX_VALUE);
				if (end == 0L) {
					end = Long.MAX_VALUE;
				}
				long scheduleNextTime = (lastRunTime == 0) ? System.currentTimeMillis() : lastRunTime + period;
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
	 * Calculates the parameters to configure the probe for the next run.  Returns null if no schedules are available.
	 * @return
	 */
	public Bundle getNextRunParams() {
		return nextRunParams;
	}
}
