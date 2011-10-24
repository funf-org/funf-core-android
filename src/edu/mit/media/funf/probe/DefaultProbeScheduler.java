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

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe.Parameter;
import edu.mit.media.funf.probe.Probe.Parameter.Builtin;

/**
 * Schedules probes based on PERIOD, DURATION, START, END,and PASSIVE parameters.
 * This class is naive to all other parameters.  Classes requiring other parameters,
 * should extend this class to ensure those parameters are merged correctly.
 *
 */
public class DefaultProbeScheduler implements ProbeScheduler {

	/* (non-Javadoc)
	 * @see edu.mit.media.funf.probe.ProbeScheduler#shouldBeEnabled(edu.mit.media.funf.probe.Probe, java.util.Collection)
	 */
	@Override
	public boolean shouldBeEnabled(Probe probe, Collection<Intent> requests) {
		return !requests.isEmpty(); // Has a requester
	}

	/* (non-Javadoc)
	 * @see edu.mit.media.funf.probe.ProbeScheduler#startRunningNow(edu.mit.media.funf.probe.Probe, android.content.Intent[])
	 */
	@Override
	public Bundle startRunningNow(Probe probe, Collection<Intent> requests) {
		// Merge parameters
		Bundle params = mergeParameters(probe, requests);
		if (params == null) {
			return null;
		}

		// Check if should run now
		long period = Utils.secondsToMillis(params.getLong(Parameter.Builtin.PERIOD.name, 0L));
		long startTime = Utils.secondsToMillis(Utils.getLong(params, Parameter.Builtin.START.name, 0L));
		long endTime = Utils.secondsToMillis(Utils.getLong(params, Parameter.Builtin.END.name, 0L));
		long mostRecentTimeRun = Utils.secondsToMillis(probe.getPreviousRunTime());
		long currentTime = System.currentTimeMillis();
		boolean shouldRun = (startTime == 0 || startTime <= currentTime) // After start time (if exists)
			&& (endTime == 0 || currentTime <= endTime)   // Before end time (if exists)
			&& (period == 0 || (mostRecentTimeRun + period) <= currentTime);
		
		if (shouldRun) {
			// Register stop alarm
			// 0L duration if duration param is not supported for probe.  Stop queued up immediately.
			long duration = Utils.secondsToMillis(params.getLong(Parameter.Builtin.DURATION.name, 0L));
			scheduleAlarm(probe, Probe.ACTION_INTERNAL_STOP, currentTime + duration);
			return params;
		} else {
			return null;
		}
	}
	

	/* (non-Javadoc)
	 * @see edu.mit.media.funf.probe.ProbeScheduler#scheduleNextRun(edu.mit.media.funf.probe.Probe, android.content.Intent[])
	 */
	@Override
	public Long scheduleNextRun(Probe probe, Collection<Intent> requests) {
		Parameter periodParam = getAvailableParameter(probe, Parameter.Builtin.PERIOD);
		if (periodParam == null) {
			return null;
		}
		Bundle params = mergeParameters(probe, requests);
		if (params == null) {
			return null;
		}
		
		long period = Utils.secondsToMillis(params.getLong(Parameter.Builtin.PERIOD.name, 0L));
		long mostRecentTimeRun = Utils.secondsToMillis(probe.getPreviousRunTime());
		scheduleAlarm(probe, Probe.ACTION_INTERNAL_RUN, mostRecentTimeRun + period);
		return Utils.millisToSeconds(mostRecentTimeRun + period);
	}
	
	/**
	 * Sets the alarm for this probe.	
	 * @param probe
	 * @param action
	 * @param time
	 */
	protected void scheduleAlarm(Probe probe, String action, long time) {
		Intent intent = new Intent(action, null, probe, probe.getClass());
		PendingIntent pi = PendingIntent.getService(probe, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager manager = (AlarmManager)probe.getSystemService(Context.ALARM_SERVICE);
		manager.set(AlarmManager.RTC_WAKEUP, time, pi);
	}
	
	/**
	 * Uses all of the outstanding data requests to determine the parameters the probe will run with.
	 * @param probe
	 * @param requests
	 * @return
	 */
	protected Bundle mergeParameters(Probe probe, Collection<Intent> requests) {
		Bundle params = new Bundle();
		putMergedParam(params, getAvailableParameter(probe, Parameter.Builtin.PERIOD), requests, false);
		putMergedParam(params, getAvailableParameter(probe, Parameter.Builtin.DURATION), requests, true);
		putMergedParam(params, getAvailableParameter(probe, Parameter.Builtin.START), requests, false);
		putMergedParam(params, getAvailableParameter(probe, Parameter.Builtin.END), requests, true);
		return params;
	}
	
	public static void putMergedParam(Bundle params, Probe.Parameter parameter, Collection<Intent> requests, boolean returnLargest) {
		if (parameter == null || requests == null || requests.isEmpty()) {
			return;
		}
		long defaultValue = (Long)parameter.getValue();
		String paramName = parameter.getName();
		long mergedValue = returnLargest ? Long.MIN_VALUE : Long.MAX_VALUE;
		for(Intent request : requests) {
			ArrayList<Bundle> dataRequests = request.getParcelableArrayListExtra(Probe.REQUESTS_KEY);
			for (Bundle dataRequest : dataRequests) {
				long value = Utils.getLong(dataRequest, paramName, defaultValue);
				if ((returnLargest && value > mergedValue) || (!returnLargest && value < mergedValue)) {
					mergedValue = value;
				}
			}
		}
		params.putLong(paramName, mergedValue);
	}
	
	public static Parameter getAvailableParameter(Probe probe, Parameter.Builtin systemParam) {
		for (Parameter p : getParametersNotNull(probe.getAvailableParameters())) {
			if(systemParam.name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
	
	public static Parameter[] getParametersNotNull(Parameter[] availableParameters) {
		return (availableParameters == null) ? new Parameter[] {} : availableParameters;
	}
	
	public static Bundle getDefaultParameters(Probe probe) {
		Bundle params = new Bundle();
		for(Parameter param : getParametersNotNull(probe.getAvailableParameters())) {
			Utils.putInBundle(params, param.getName(), param.getValue());
		}
		return params;
	}
	
	public static Bundle getCompleteParams(Probe probe, Bundle params) {
		if (params == null) {
			return null;
		}
		Bundle completeParams = getDefaultParameters(probe);
		// TODO: only use parameters that are specified
		// for (Parameter param : probe.getAvailableParameters()) {
    	// Utils.putInBundle(params, param.getName(), param.getValue());
        //}
		completeParams.putAll(params);
		return completeParams;
	}
}
