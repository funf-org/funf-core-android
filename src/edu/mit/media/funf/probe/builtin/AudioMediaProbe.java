package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.MediaStore.Audio;
import edu.mit.media.funf.DecimalTimeUnit;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.DisplayName;

@DisplayName("Audio Media File Stats Probe")
@DefaultSchedule(period=36000)
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
		projectionMap.put(Audio.Media.DISPLAY_NAME, sensitiveStringCell());
		projectionMap.put(Audio.Media.MIME_TYPE, stringCell());
		projectionMap.put(Audio.Media.SIZE, longCell());
		projectionMap.put(Audio.Media.TITLE, sensitiveStringCell());
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
