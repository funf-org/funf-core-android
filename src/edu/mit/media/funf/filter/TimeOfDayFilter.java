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
package edu.mit.media.funf.filter;

import java.util.Calendar;

import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.util.LogUtil;

public class TimeOfDayFilter implements DataListener {

    @Configurable
    private String start = "00:00"; // 24-hour time in HH:mm format 
    
    @Configurable
    private String end = "23:59"; // 24-hour time in HH:mm format
    
    @Configurable
    private DataListener listener;
    
    private Calendar calendar;
    
    private int startTime = 0;
    private int endTime = 0;
    private boolean isParsingDone = false;
    
    private boolean isDataReceived = false;
        
    TimeOfDayFilter() {
    }
    
    public TimeOfDayFilter(DataListener listener) {
        this.listener = listener;
        calendar = Calendar.getInstance();
    }
        
    public void setTimeInterval(String start, String end) {
        this.start = start;
        this.end = end;
        isParsingDone = false;
    }
    
    private int parseFormattedTime(String time) {
        String[] split = time.split(":");
        try {
            int hour = Integer.parseInt(split[0]);
            int min = Integer.parseInt(split[1]);
            return hour*60 + min;   
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "TimeOfDayFilter: Error in parsing time: " + time);
            return 0;
        }
    }
    
    private int getCurrentTime() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        return hour*60 + min;
    }

    @Override
    public void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        int currentTime = getCurrentTime();
        if (!isParsingDone) {
            startTime = parseFormattedTime(start);
            endTime = parseFormattedTime(end);
            isParsingDone = true;
                
        }
        if (startTime <= currentTime && currentTime < endTime) {
            listener.onDataReceived(dataSourceConfig, data);
            isDataReceived = true;
        }
    }

    @Override
    public void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        if (isDataReceived) {
            listener.onDataCompleted(dataSourceConfig, checkpoint);   
            isDataReceived = false;
        }
    }
}
