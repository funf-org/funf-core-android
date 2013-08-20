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

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class TimeOfDayFilter implements DataListener {

    @Configurable
    private String start = "06:00"; // 24-hour time in HH:mm format 
    
    @Configurable
    private String end = "18:00"; // 24-hour time in HH:mm format
    
    private Calendar calendar;
    
    private final DataListener listener;
    
    public TimeOfDayFilter(DataListener listener) {
        this.listener = listener;
        calendar = Calendar.getInstance();
    }
        
    public void setTimeInterval(String start, String end) {
        this.start = start;
        this.end = end;
    }
    
    private int parseFormattedTime(String time) {
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int min = Integer.parseInt(split[1]);
        return hour*100 + min;
    }

    @Override
    public void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int currentTime = hour*100 + min;
        int startTime = parseFormattedTime(start);
        int endTime = parseFormattedTime(end);
        if (startTime <= currentTime && currentTime < endTime) {
            listener.onDataReceived(dataSourceConfig, data);
        }
    }

    @Override
    public void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        listener.onDataCompleted(dataSourceConfig, checkpoint);
    }
}
