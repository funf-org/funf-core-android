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
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.PhoneNumberCell;

@Schedule.DefaultSchedule(interval=36000)
@RequiredPermissions(android.Manifest.permission.READ_CONTACTS)
public class CallLogProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return CallLog.Calls.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Calls.DATE;
	}

	@Override
	protected Map<String,CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Calls._ID, intCell());
		projectionKeyToType.put(Calls.NUMBER, new SensitiveCell(new PhoneNumberCell()));
		projectionKeyToType.put(Calls.DATE, longCell());
		projectionKeyToType.put(Calls.TYPE, intCell());
		projectionKeyToType.put(Calls.DURATION, longCell());
		projectionKeyToType.put(Calls.CACHED_NAME, sensitiveStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_LABEL, sensitiveStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_TYPE, sensitiveStringCell());
		return projectionKeyToType;
	}

}