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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.net.Uri;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.ContinuableProbe;
import edu.mit.media.funf.time.DecimalTimeUnit;

public abstract class DatedContentProviderProbe extends ContentProviderProbe implements ContinuableProbe {

	@Configurable
	protected BigDecimal afterDate = null;
	
	private BigDecimal latestTimestamp = null;
	
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
		String dateFilter = null;
		String[] dateFilterParams = null;
		if (afterDate != null || latestTimestamp != null) {
			dateFilter = dateColumn + " > ?";
			BigDecimal startingDate = afterDate == null ? latestTimestamp : 
						afterDate.max(latestTimestamp == null ? BigDecimal.ZERO : latestTimestamp);
			dateFilterParams = new String[] {String.valueOf(getDateColumnTimeUnit().convert(startingDate, DecimalTimeUnit.SECONDS))};
		}
		return getContext().getContentResolver().query(
				getContentProviderUri(),
				projection, // TODO: different platforms have different fields supported for content providers, need to resolve this
				dateFilter, 
				dateFilterParams,
                dateColumn + " DESC");
	}
	
	protected abstract Uri getContentProviderUri();
	
	protected abstract String getDateColumnName();

	protected DecimalTimeUnit getDateColumnTimeUnit() {
		return DecimalTimeUnit.MILLISECONDS;
	}
	
	
	@Override
	protected void sendData(JsonObject data) {
		super.sendData(data);
		latestTimestamp = getTimestamp(data);
	}

	@Override
	protected BigDecimal getTimestamp(JsonObject data) {
		return getDateColumnTimeUnit().toSeconds(data.get(getDateColumnName()).getAsLong());
	}

	@Override
	public JsonElement getCheckpoint() {
		return getGson().toJsonTree(latestTimestamp);
	}

	@Override
	public void setCheckpoint(JsonElement checkpoint) {
		latestTimestamp = checkpoint == null ? null : checkpoint.getAsBigDecimal();
	}
	
	
	
}
