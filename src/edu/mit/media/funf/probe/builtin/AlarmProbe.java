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

public class AlarmProbe extends Base implements PassiveProbe {

    @Configurable
    private int interval = 0;
    
    @Configurable
    private boolean exact = true;
    
    @Configurable
    private int offset = 0;
    
    Runnable operation = new Runnable() {
        @Override
        public void run() {
            Log.d(LogUtil.TAG, "alarm!");
            // Notify listeners of alarm event.
            JsonObject data = new JsonObject();
            sendData(data);
        }
    };
    
    FunfManager manager;
    
    public void setManager(FunfManager manager) {
        this.manager = manager;
    }

    protected void onEnable() {
        Long intervalMillis = TimeUtil.secondsToMillis(interval);
        Long delayMillis = TimeUtil.secondsToMillis(offset);
        if (manager != null) {
            manager.registerAlarm(delayMillis, intervalMillis, exact, this, operation);
            Log.d(LogUtil.TAG, "alarm set");
        }
        
    }
    
    protected void onDisable() {
        if (manager != null) {
            manager.unregisterAlarm(this);
            Log.d(LogUtil.TAG, "alarm reset");   
        }
    }
    
    @Override
    protected boolean isWakeLockedWhileRunning() {
        return false;
    }
    
}
