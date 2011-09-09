package edu.mit.media.funf.probe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;

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
		Log.i(TAG, "Previous Date Sent Time: " + getDateColumnTimeUnit().convert(getPreviousDataSentTime(), TimeUnit.MILLISECONDS));
		return getContentResolver().query(
				getContentProviderUri(),
				projection, // TODO: different platforms have different fields supported for content providers, need to resolve this
                dateColumn + " > ?", 
                new String[] {String.valueOf(getDateColumnTimeUnit().convert(getPreviousDataSentTime(), TimeUnit.MILLISECONDS))}, 
                dateColumn + " DESC");
	}
	
	protected abstract Uri getContentProviderUri();
	
	protected abstract String getDateColumnName();

	protected TimeUnit getDateColumnTimeUnit() {
		return TimeUnit.MILLISECONDS;
	}
	
	@Override
	protected long getTimestamp(List<Bundle> results) {
		if (results == null || results.isEmpty()) {
			return Utils.getTimestamp();
		} else {
			return getTimestamp(results.get(0));
		}
	}
	
	protected long getTimestamp(Bundle result) {
		return TimeUnit.SECONDS.convert(result.getLong(getDateColumnName()), getDateColumnTimeUnit());
	}
	

}
