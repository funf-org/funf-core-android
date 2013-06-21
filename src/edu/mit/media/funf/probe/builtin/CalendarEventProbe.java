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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.util.Log;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.util.LogUtil;

@DisplayName("Calendar Event Probe")
@RequiredPermissions(android.Manifest.permission.READ_CALENDAR)
@Schedule.DefaultSchedule(interval=3600)
public class CalendarEventProbe extends ContentProviderProbe implements PassiveProbe {

	// Filter out events that start after this number of minutes from the current time
	@Configurable 
	private long maxStartDelay = 60; 	
	private long minutesToMilliSecs = 60000;
	
	@Override
	protected Cursor getCursor(String[] projection) {
		return getContext().getContentResolver().query(
				Uri.parse("content://com.android.calendar/events"), //CalendarContract.Events.CONTENT_URI
                projection, 
                null, 
                null, 
                null);
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
	
	@Override
	protected JsonObject parseData(Cursor cursor, String[] projection, Map<String, CursorCell<?>> projectionMap) {
		JsonObject calendarEvent = super.parseData(cursor, projection, getCalendarEventProjectionMap());
		if (calendarEvent != null) {
			long dtstart = calendarEvent.get("dtstart").getAsLong();
			long dtend = calendarEvent.get("dtend").getAsLong();
			long currentTime = System.currentTimeMillis();
			if (((dtstart - currentTime) < maxStartDelay*minutesToMilliSecs && currentTime < dtstart) || 
					(currentTime > dtstart && currentTime < dtend)) {
				Log.d(LogUtil.TAG, "adding calendar event");
				return calendarEvent;
			} else {
				//Log.d(LogUtil.TAG, "skipping calendar event");
			}
		}
		return null;
	}
}