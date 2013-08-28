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

import java.text.ParseException;
import java.util.Calendar;

import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.util.LogUtil;

public class LocalTimeOfDayFilter implements DataListener {

    /**
     * 24-hour time in HH:mm format
     */
    @Configurable
    private String start = "00:00";  
    
    /**
     * 24-hour time in HH:mm format
     */
    @Configurable
    private String end = "24:00"; 
    
    @Configurable
    private DataListener listener;
    
    private Calendar calendar;
    
    private int startTime = 0;
    private int endTime = 0;
    private boolean isParsingDone = false;
    
    private boolean isDataReceived = false;
        
    LocalTimeOfDayFilter() {
    }
    
    public LocalTimeOfDayFilter(DataListener listener) {
        this.listener = listener;
        calendar = Calendar.getInstance();
    }
        
    public void setTimeInterval(String start, String end) {
        this.start = start;
        this.end = end;
        isParsingDone = false;
    }
    
    private int parseFormattedTime(String time) throws ParseException {
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int min = Integer.parseInt(split[1]);
        return hour*60 + min;   
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
            try {
                startTime = parseFormattedTime(start);
            } catch (ParseException e) {
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Error in parsing start time " + start);
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Reverting to default start time 00:00");
                startTime = 0;
            }
            try {
                endTime = parseFormattedTime(end);
            } catch (ParseException e) {
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Error in parsing end time " + end);
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Reverting to default end time 24:00");
                endTime = 24*60;
            }
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
