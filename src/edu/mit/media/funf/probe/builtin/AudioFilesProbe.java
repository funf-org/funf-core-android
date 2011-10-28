/**
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
 */
package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.net.Uri;
import android.provider.MediaStore.Audio;
import edu.mit.media.funf.probe.CursorCell;
import edu.mit.media.funf.probe.DatedContentProviderProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AudioFilesKeys;

public class AudioFilesProbe extends DatedContentProviderProbe implements AudioFilesKeys {

	@Override
	protected Uri getContentProviderUri() {
		return Audio.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Audio.Media.DATE_MODIFIED;
	}

	@Override
	protected String getDataName() {
		return AUDIO_FILES;
	}
	
	@Override
	protected String getDisplayName() {
		return "Audio File Stats Probe";
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Audio.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Audio.Media.DATE_ADDED, longCell());
		projectionMap.put(Audio.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Audio.Media.DISPLAY_NAME, hashedStringCell());
		projectionMap.put(Audio.Media.MIME_TYPE, stringCell());
		projectionMap.put(Audio.Media.SIZE, longCell());
		projectionMap.put(Audio.Media.TITLE, hashedStringCell());
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
	protected TimeUnit getDateColumnTimeUnit() {
		return TimeUnit.SECONDS;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

	@Override
	protected long getDefaultPeriod() {
		return 36000L;
	}
}
