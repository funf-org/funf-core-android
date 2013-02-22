/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
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

import android.net.Uri;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInternal.Sms;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SmsKeys;

/**
 * @author alangardner
 *
 */
@Schedule.DefaultSchedule(interval=36000)
@DisplayName("SMS Log Probe")
@RequiredPermissions(android.Manifest.permission.READ_SMS)
public class SmsProbe extends DatedContentProviderProbe implements SmsKeys {

	@Override
	protected Uri getContentProviderUri() {
		return Sms.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Sms.DATE;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Sms.TYPE, intCell());
		projectionMap.put(Sms.THREAD_ID, intCell());
		projectionMap.put(Sms.ADDRESS, sensitiveStringCell()); // TODO: figure out if we have to normalize this first (maybe phone number)
		projectionMap.put(Sms.PERSON_ID, longCell());
		projectionMap.put(Sms.DATE, longCell());
		projectionMap.put(Sms.READ, booleanCell());
		//projectionMap.put(Sms.SEEN, booleanCell()); //Not Supported on all devices
		projectionMap.put(Sms.STATUS, intCell());
		projectionMap.put(Sms.SUBJECT, sensitiveStringCell());
		projectionMap.put(Sms.BODY, sensitiveStringCell());
		projectionMap.put(Sms.PERSON, sensitiveStringCell());
		projectionMap.put(Sms.PROTOCOL, intCell());
		projectionMap.put(Sms.REPLY_PATH_PRESENT, booleanCell());
		projectionMap.put(Sms.SERVICE_CENTER, stringCell());
		projectionMap.put(Sms.LOCKED, booleanCell());
		//projectionMap.put(Sms.ERROR_CODE, intCell());  //Not Supported on all devices
		//projectionMap.put(Sms.META_DATA, hashedStringCell());  Doesn't exist for some reason
		return projectionMap;
	}

}
