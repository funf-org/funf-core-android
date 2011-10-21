package edu.mit.media.funf.probe;

import java.util.Arrays;
import java.util.Collection;

import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe.Parameter;
import edu.mit.media.funf.probe.Probe.SystemParameter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DefaultProbeScheduler implements ProbeScheduler {


	/* (non-Javadoc)
	 * @see edu.mit.media.funf.probe.ProbeScheduler#startRunningNow(edu.mit.media.funf.probe.Probe, android.content.Intent[])
	 */
	public Bundle startRunningNow(Probe probe, Collection<Intent> requests) {
		return null; // TODO: implement
	}
	

	/* (non-Javadoc)
	 * @see edu.mit.media.funf.probe.ProbeScheduler#scheduleNextRun(edu.mit.media.funf.probe.Probe, android.content.Intent[])
	 */
	public Long scheduleNextRun(Probe probe, Collection<Intent> requests) {
		return null; // TODO: implement
	}
	
	private Parameter getAvailableSystemParameter(Probe probe, SystemParameter systemParam) {
		for (Parameter p : probe.getAvailableParametersNotNull()) {
			if(systemParam.name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
	
	private Parameter[] getAvailableParametersNotNull() {
		Parameter[] availableParameters = getAvailableParameters();
		return (availableParameters == null) ? new Parameter[] {} : availableParameters;
	}
	
	private Bundle getDefaultParameters() {
		Bundle params = new Bundle();
		for(Parameter param :getAvailableParametersNotNull()) {
			Utils.putInBundle(params, param.getName(), param.getValue());
		}
		return params;
	}
	
	protected Bundle getCompleteParams(Bundle params) {
		if (params == null) {
			return null;
		}
		Bundle completeParams = getDefaultParameters();
		// TODO: only use parameters that are specified
		// for (Parameter param : probe.getAvailableParameters()) {
    	// Utils.putInBundle(params, param.getName(), param.getValue());
        //}
		completeParams.putAll(params);
		return completeParams;
	}
	
	

	private boolean shouldRunNow(Bundle params) {
		if (params == null) {
			Log.v(TAG, "shouldRunNow is false because no params were specified");
			return false;
		}
		long period = Utils.secondsToMillis(Utils.getLong(params, SystemParameter.PERIOD.name, 0L));
		long startTime = Utils.secondsToMillis(Utils.getLong(params, SystemParameter.START.name, 0L));
		long endTime = Utils.secondsToMillis(Utils.getLong(params, SystemParameter.END.name, 0L));
		long currentTime = System.currentTimeMillis();
		Log.v(TAG, "Period, Start, End, Current, LastRun ->" + Utils.join(Arrays.asList(period, startTime, endTime, currentTime, mostRecentTimeRun), ", "));
		return (startTime == 0 || startTime <= currentTime) // After start time (if exists)
			&& (endTime == 0 || currentTime <= endTime)   // Before end time (if exists)
			&& (period == 0 || (mostRecentTimeRun + period) <= currentTime); // At least one period since last run
	}	
	
	private void scheduleNextRun() {
		Parameter periodParam = getAvailableSystemParameter(SystemParameter.PERIOD);
		if (periodParam == null) {
			Log.v(TAG, "PERIOD parameter is not valid for this probe");
			return;
		}
		if (periodParam.isSupportedByProbe()) {
			Log.v(TAG, "PERIOD parameter schedule is supported by probe");
			return;
		}
		
		ProbeScheduleResolver scheduleResolver = new ProbeScheduleResolver(allRequests.getAll(), getDefaultParameters(), getPreviousRunTime(), getPreviousRunParams());
		Bundle nextRunParams = scheduleResolver.getNextRunParams();
		if (nextRunParams != null) {
			Intent nextRunIntent = new Intent(this, getClass());
			nextRunIntent.putExtras(nextRunParams);
			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextRunIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			nextRunTime = scheduleResolver.getNextRunTime();
			Log.d(TAG, "Next run time: " + nextRunTime);
			if (nextRunTime != 0L) {
				Log.d(TAG, "LAST_TIME: " + mostRecentTimeRun);
				Log.d(TAG, "CURRENT_TIME: " + System.currentTimeMillis());
				Log.d(TAG, "DIFFERENCE: " + (nextRunTime - System.currentTimeMillis()));
				am.set(AlarmManager.RTC_WAKEUP, nextRunTime, pendingIntent);
			}
		}
	}
	

	private void cancelNextRun() {
		Intent nextRunIntent = new Intent(this, getClass());
		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextRunIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(pendingIntent);
		this.nextRunTime = 0L;
	}
}
