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

import edu.mit.media.funf.config.Configurable;
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
    private int interval = 300;
    
    @Configurable
    private int delay = 10;
    
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            start();
        }
    };

    protected void onEnable() {
        String intentString = getClass().getName();
        Long intervalMillis = TimeUtil.secondsToMillis(interval);
        Long delayMillis = TimeUtil.secondsToMillis(delay);
        
        getContext().registerReceiver(receiver, new IntentFilter(intentString));
        pendingIntent = PendingIntent.getBroadcast(getContext(), 0, 
                new Intent(intentString), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager)(getContext().getSystemService(Context.ALARM_SERVICE));
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, delayMillis, intervalMillis, pendingIntent);
        Log.d(LogUtil.TAG, "alarm set");
    }

    protected void onStart() {
        Log.d(LogUtil.TAG, "alarm!");
        // Notify listeners of alarm event.
        JsonObject data = new JsonObject();
        sendData(data);
        stop();
    }
    
    protected void onDisable() {
        alarmManager.cancel(pendingIntent);
        getContext().unregisterReceiver(receiver);
        Log.d(LogUtil.TAG, "alarm reset");
    }
    
}
