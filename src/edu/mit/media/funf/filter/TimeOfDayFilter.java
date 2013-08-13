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

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;

public class TimeOfDayFilter extends Filter {

    @Configurable
    private int start = 0600; // in 24-hour clock format 
    
    @Configurable
    private int end = 1800; // in 24-hour clock format
    
    protected void filterData(IJsonObject dataSourceConfig, IJsonObject data) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        int currentTime = hour*100 + min;
        if (start <= currentTime && currentTime < end) {
            sendData(dataSourceConfig, data);
        }
    }
}
