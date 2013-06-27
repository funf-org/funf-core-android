/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah    (pararthshah717@gmail.com)
 *            Klemen Peternel (klemen.peternel@gmail.com)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@DisplayName("Calendar Event Probe")
@RequiredPermissions(android.Manifest.permission.READ_CALENDAR)
@Schedule.DefaultSchedule(interval=3600)
public class CalendarEventProbe extends ContentProviderProbe implements PassiveProbe {

	// Filter out events that start after this delay in seconds from the current time
	@Configurable 
	private long maxStartDelay = 3600;  

	@Override
	protected Cursor getCursor(String[] projection) {
		if (!Arrays.asList(projection).contains("dtstart") || 
				!Arrays.asList(projection).contains("dtend")) {
			List<String> projectionList = new ArrayList<String>(Arrays.asList(projection));
			if (!Arrays.asList(projection).contains("dtstart"))
				projectionList.add("dtstart");
			if (!Arrays.asList(projection).contains("dtend"))
				projectionList.add("dtend");
			projection = new String[projectionList.size()];
			projectionList.toArray(projection);
		}

		String currentTime = String.valueOf(System.currentTimeMillis());
		String maxStartTime = String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxStartDelay));
		String dateFilter = null;
		String[] dateFilterParams = null;
		dateFilter = "(? < dtstart AND dtstart < ?) OR (? > dtstart AND ? < dtend)";
		dateFilterParams = new String[] {currentTime, maxStartTime, currentTime, currentTime };
		return getContext().getContentResolver().query(
				CalendarContract.Events.CONTENT_URI, // Uri.parse("content://com.android.calendar/events")
				projection, 
				dateFilter, 
				dateFilterParams, 
				"dtstart ASC");
	}

	private Map<String, CursorCell<?>> calendarEventProjectionMap;
	private Map<String, CursorCell<?>> getCalendarEventProjectionMap() {
		if (calendarEventProjectionMap == null) {
			Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
			projectionKeyToType.put(Events._ID, intCell());
			projectionKeyToType.put("calendar_id", intCell());
			projectionKeyToType.put("ownerAccount", sensitiveStringCell());
			projectionKeyToType.put("organizer", sensitiveStringCell());
			projectionKeyToType.put("title", stringCell());
			projectionKeyToType.put("eventLocation", stringCell());
			projectionKeyToType.put("eventTimezone", stringCell());
			projectionKeyToType.put("eventEndTimezone", stringCell());
			projectionKeyToType.put("dtstart", longCell());
			projectionKeyToType.put("dtend", longCell());
			projectionKeyToType.put("isOrganizer", stringCell());
			projectionKeyToType.put("allDay", intCell());
			projectionKeyToType.put("availability", intCell());
			calendarEventProjectionMap = projectionKeyToType;
		}
		return calendarEventProjectionMap;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.putAll(getCalendarEventProjectionMap());
		return projectionKeyToType;
	}
}