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
		Log.i(TAG, "Previous Date Sent Time: " + getDateColumnTimeUnit().convert(getPreviousDataSentTime(), TimeUnit.SECONDS));
		return getContentResolver().query(
				getContentProviderUri(),
				projection, // TODO: different platforms have different fields supported for content providers, need to resolve this
                dateColumn + " > ?", 
                new String[] {String.valueOf(getDateColumnTimeUnit().convert(getPreviousDataSentTime() + 1, TimeUnit.SECONDS))}, // Add one because of unit truncation
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
