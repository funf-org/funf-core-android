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
import android.provider.MediaStore.Audio;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.time.DecimalTimeUnit;

@DisplayName("Audio Media File Stats Probe")
@Schedule.DefaultSchedule(interval=36000)
public class AudioMediaProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Audio.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Audio.Media.DATE_MODIFIED;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Audio.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Audio.Media.DATE_ADDED, longCell());
		projectionMap.put(Audio.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Audio.Media.DISPLAY_NAME, stringCell());
		projectionMap.put(Audio.Media.MIME_TYPE, stringCell());
		projectionMap.put(Audio.Media.SIZE, longCell());
		projectionMap.put(Audio.Media.TITLE, stringCell());
		projectionMap.put(Audio.Media.ALBUM, stringCell());
		//projectionMap.put(Audio.Media.ALBUM_ART, stringCell());  Not there for some reason
		projectionMap.put(Audio.Media.ALBUM_ID, longCell());
		//projectionMap.put(Audio.Media.ALBUM_KEY, stringCell()); Not relevant, Internal android use
		projectionMap.put(Audio.Media.ARTIST, stringCell());
		projectionMap.put(Audio.Media.ARTIST_ID, longCell());
		// projectionMap.put(Audio.Media.ARTIST_KEY, stringCell()); Not relevant, Internal android use
		projectionMap.put(Audio.Media.COMPOSER, stringCell());
		projectionMap.put(Audio.Media.DURATION, longCell());
		projectionMap.put(Audio.Media.IS_ALARM, booleanCell());
		projectionMap.put(Audio.Media.IS_MUSIC, booleanCell());
		projectionMap.put(Audio.Media.IS_NOTIFICATION, booleanCell());
		projectionMap.put(Audio.Media.IS_RINGTONE, booleanCell());
		//projectionMap.put(Audio.Media.TITLE_KEY, stringCell());  Not relevant, Internal android use
		projectionMap.put(Audio.Media.TRACK, intCell());
		projectionMap.put(Audio.Media.YEAR, intCell());
		
		return projectionMap;
	}
	
	@Override
	protected DecimalTimeUnit getDateColumnTimeUnit() {
		return DecimalTimeUnit.SECONDS;
	}


}
