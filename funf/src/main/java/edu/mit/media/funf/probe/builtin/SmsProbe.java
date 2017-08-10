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
