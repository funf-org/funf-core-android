/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
@DisplayName("Alarm Probe")
@Schedule.DefaultSchedule(interval=Probe.DEFAULT_PERIOD, duration=ContinuousProbe.DEFAULT_DURATION)
public class AlarmProbe extends Base implements ContinuousProbe, Runnable {

    /**
     * In seconds. Only positive values considered as valid.
     */
    @Configurable
    private Long interval = null;

    @Configurable
    private boolean exact = false;

    /**
     * In seconds. Only non-negative values considered as valid.
     */
    @Configurable
    private Long offset = null; 

    @Override
    public void run() {
        Log.d(LogUtil.TAG, "alarm!");
        // Notify listeners of alarm event.
        JsonObject data = new JsonObject();
        sendData(data);
    }

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
