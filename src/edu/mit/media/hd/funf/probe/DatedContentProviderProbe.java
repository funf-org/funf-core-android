package edu.mit.media.hd.funf.probe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.mit.media.hd.funf.Utils;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public abstract class DatedContentProviderProbe extends ContentProviderProbe {

	@Override
	protected Cursor getCursor(String[] projection) {
		String dateColumn = getDateColumnName();
		// Used the code below when we specified projection exactly
		List<String> projectionList = Arrays.asList(projection);
		if (!Arrays.asList(projection).contains(dateColumn)) {
			projectionList = new ArrayList<String>(projectionList);
			projectionList.add(dateColumn);
			projection = new String[projectionList.size()];
			projectionList.toArray(projection);
		}
		return getContentResolver().query(
				getContentProviderUri(),
				projection, // TODO: different platforms have different fields supported for content providers, need to resolve this
                dateColumn + " > ?", 
                new String[] {String.valueOf(getPreviousDataSentTime())}, 
                dateColumn + " DESC");
	}
	
	protected abstract Uri getContentProviderUri();
	
	protected abstract String getDateColumnName();

	@Override
	protected long getTimestamp(List<Bundle> results) {
		if (results == null || results.isEmpty()) {
			return Utils.getTimestamp();
		} else {
			return getTimestamp(results.get(0));
		}
	}
	
	protected long getTimestamp(Bundle result) {
		return Utils.millisToSeconds(result.getLong(getDateColumnName()));
	}


}
