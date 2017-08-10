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
