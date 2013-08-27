/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
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
 * 
 */
package edu.mit.media.funf.probe.builtin;

import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

@DisplayName("Alarm Probe")
@Schedule.DefaultSchedule(interval=Probe.DEFAULT_PERIOD, duration=ContinuousProbe.DEFAULT_DURATION)
public class AlarmProbe extends Base implements Runnable {

    @Configurable
    private Long interval = null; // In seconds. Only positive values considered as valid.

    @Configurable
    private boolean exact = false;

    @Configurable
    private Long offset = null; // In seconds. Only non-negative values considered as valid. 

    @Override
    public void run() {
        Log.d(LogUtil.TAG, "alarm!");
        // Notify listeners of alarm event.
        JsonObject data = new JsonObject();
        sendData(data);
    }

    /**
     * There are three kinds of schedules possible with this AlarmProbe:
     * 
     *     1. interval > 0, offset >= 0:
     *          This schedules the alarm to go off at every "interval" seconds,
     *          starting from the Unix timestamp referred to by "offset" (in seconds).
     *          If "offset" is in the past, the start time is set as the immediate next
     *          time in the future that occurs in the sequence starting at "offset"
     *          and having a period of "interval" seconds.
     *     2. interval > 0, offset = null or < 0:
     *          This schedules the alarm to go off at every "interval" seconds,
     *          starting from the instant when this function is executed.
     *     3. interval = null or <= 0, offset >= 0:
     *          This schedules a one-time alarm to go off at the Unix timestamp
     *          referred by "offset" (in seconds).
     * 
     * For types 1 and 2, the flag "exact" determines whether the alarm will go off exactly
     * or approximately at the specified times (inexact alarms use less battery power)
     * For type 3, the value of "exact" is immaterial; the alarm will go off exactly at
     * the specified time.
     */
    protected void onStart() {
        String probeConfig = JsonUtils.immutable(getGson().toJsonTree(this)).toString();
        long intervalMillis = (interval == null || interval < 0) ? 0 : TimeUtil.secondsToMillis(interval);
        long offsetMillis = (offset == null || offset < 0) ? -1 : TimeUtil.secondsToMillis(offset);
        long currentMillis = System.currentTimeMillis();
        if (intervalMillis > 0) {
            if (offsetMillis >= 0) {  // Type 1
                if (offsetMillis <= currentMillis) { // Offset is in the past.
                    long intervalDeltaMillis = (currentMillis - offsetMillis) % intervalMillis;
                    long timeToNextAlarmMillis = intervalMillis - intervalDeltaMillis;
                    offsetMillis = currentMillis + timeToNextAlarmMillis;
                }
                FunfManager.registerAlarm(getContext(), probeConfig, offsetMillis, intervalMillis, exact);    
            } else {  // Type 2
                FunfManager.registerAlarm(getContext(), probeConfig, currentMillis, intervalMillis, exact);
            }   
        } else {  // Type 3
            assert (offsetMillis >= 0); // Offset must be valid.
            // If offset is in the past, alarm will fire immediately.
            FunfManager.registerAlarm(getContext(), probeConfig, offsetMillis, intervalMillis, exact);
        }
        Log.d(LogUtil.TAG, "alarm set");
    }

    protected void onStop() {
        String probeConfig = JsonUtils.immutable(getGson().toJsonTree(this)).toString();
        FunfManager.unregisterAlarm(getContext(), probeConfig);
        Log.d(LogUtil.TAG, "alarm reset");   
    }

    @Override
    protected boolean isWakeLockedWhileRunning() {
        return false;
    }

}
