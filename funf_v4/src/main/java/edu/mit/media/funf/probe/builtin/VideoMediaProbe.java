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
import android.provider.MediaStore.Video;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.time.DecimalTimeUnit;

@DisplayName("Video File Stats Probe")
@Schedule.DefaultSchedule(interval=604800)
public class VideoMediaProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Video.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return  Video.Media.DATE_MODIFIED;
	}
	
	@Override
	protected DecimalTimeUnit getDateColumnTimeUnit() {
		return DecimalTimeUnit.SECONDS;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Video.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Video.Media.DATE_ADDED, longCell());
		projectionMap.put(Video.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Video.Media.DISPLAY_NAME, sensitiveStringCell());
		projectionMap.put(Video.Media.MIME_TYPE, stringCell());
		projectionMap.put(Video.Media.SIZE, longCell());
		projectionMap.put(Video.Media.TITLE, sensitiveStringCell());
		projectionMap.put(Video.Media.ALBUM, stringCell());
		projectionMap.put(Video.Media.ARTIST, stringCell());
		projectionMap.put(Video.Media.BOOKMARK, intCell());
		projectionMap.put(Video.Media.BUCKET_DISPLAY_NAME, stringCell());
		projectionMap.put(Video.Media.BUCKET_ID, stringCell());
		projectionMap.put(Video.Media.CATEGORY, stringCell());
		projectionMap.put(Video.Media.DATE_TAKEN, longCell());
		projectionMap.put(Video.Media.DESCRIPTION, stringCell());
		projectionMap.put(Video.Media.DURATION, longCell());
		projectionMap.put(Video.Media.IS_PRIVATE, intCell());
		projectionMap.put(Video.Media.LANGUAGE, stringCell());
		projectionMap.put(Video.Media.LATITUDE, doubleCell());
		projectionMap.put(Video.Media.LONGITUDE, doubleCell());
		projectionMap.put(Video.Media.MINI_THUMB_MAGIC, intCell());
		projectionMap.put(Video.Media.RESOLUTION, stringCell());
		projectionMap.put(Video.Media.TAGS, stringCell());
		return projectionMap;
	}

}
