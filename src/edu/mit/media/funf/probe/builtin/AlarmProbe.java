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
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class AlarmProbe extends Base implements PassiveProbe, Runnable {

    @Configurable
    private Long interval = null;

    @Configurable
    private boolean exact = false;

    @Configurable
    private Long offset = null;

    @Override
    public void run() {
        Log.d(LogUtil.TAG, "alarm!");
        // Notify listeners of alarm event.
        JsonObject data = new JsonObject();
        sendData(data);
    }

    protected void onEnable() {
        String probeConfig = JsonUtils.immutable(getGson().toJsonTree(this)).toString();
        Long intervalMillis = interval == null ? null : TimeUtil.secondsToMillis(interval);
        if (exact) {
            Long offsetMillis = offset == null ? null : TimeUtil.secondsToMillis(offset);
            Long currentMillis = System.currentTimeMillis();
            if (intervalMillis != null && offsetMillis != null && intervalMillis != 0) {
                Long startMillis = currentMillis - (currentMillis % intervalMillis)
                        + (offsetMillis % intervalMillis) + intervalMillis;
                FunfManager.registerAlarm(getContext(), probeConfig, startMillis, intervalMillis, exact);    
            } else {
                FunfManager.registerAlarm(getContext(), probeConfig, currentMillis, intervalMillis, exact);
            }   
        } else {
            FunfManager.registerAlarm(getContext(), probeConfig, 0L, intervalMillis, exact);
        }
        
        Log.d(LogUtil.TAG, "alarm set");
    }

    protected void onDisable() {
        String probeConfig = JsonUtils.immutable(getGson().toJsonTree(this)).toString();
        FunfManager.unregisterAlarm(getContext(), probeConfig);
        Log.d(LogUtil.TAG, "alarm reset");   
    }

    @Override
    protected boolean isWakeLockedWhileRunning() {
        return false;
    }

}
