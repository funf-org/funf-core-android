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
package edu.mit.media.funf.filter;

import java.util.Calendar;

import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.util.LogUtil;

/**
 * Passes on data only when the device local time is between the
 * configured start and end times.
 * 
 * Filters can be registered as a DataListener to any data source,
 * for eg. probes or other filters. Filters listen for data from
 * the source, and if certain specified conditions are met, 
 * forward the data to their own Data Listener.
 * 
 * For the LocalTimeOfDayFilter, the "start" and "end" variables
 * must be set to the start and end time of the day (in 24-hour 
 * clock format) between which this filter should let the data
 * through.
 * 
 * If the "start" and/or "end" values are invalid, the default 
 * values DEFAULT_START and/or DEFAULT_END will be used for 
 * filtering.
 *
 */
public class LocalTimeOfDayFilter implements DataListener, DataSource {

    public static final String DEFAULT_START = "00:00";
    public static final String DEFAULT_END = "24:00";
    
    /**
     * 24-hour time in HH:mm format
     */
    @Configurable
    private String start = DEFAULT_START;  
    
    /**
     * 24-hour time in HH:mm format
     */
    @Configurable
    private String end = DEFAULT_END; 
    
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
    
    private static int parseFormattedTime(String time) 
            throws IllegalArgumentException {
        if (time == null)
            throw new IllegalArgumentException();
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int min = Integer.parseInt(split[1]);
        if (hour < 0 || hour > 24 || min < 0 || min >= 60)
            throw new IllegalArgumentException();
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
            } catch (IllegalArgumentException e) {
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Error in parsing start time " + start);
                Log.e(LogUtil.TAG, "LocalTimeOfDayFilter: Reverting to default start time 00:00");
                startTime = 0;
            }
            try {
                endTime = parseFormattedTime(end);
            } catch (IllegalArgumentException e) {
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

    @Override
    public void setListener(DataListener listener) {
        this.listener = listener;
    }
}
