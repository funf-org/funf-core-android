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

import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import edu.mit.media.funf.probe.CursorCell;
import edu.mit.media.funf.probe.DatedContentProviderProbe;
import edu.mit.media.funf.probe.CursorCell.HashedCell;
import edu.mit.media.funf.probe.CursorCell.PhoneNumberCell;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CallLogKeys;

public class CallLogProbe extends DatedContentProviderProbe {

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.READ_CONTACTS
		};
	}

	
	protected Map<String,CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Calls._ID, intCell());
		projectionKeyToType.put(Calls.NUMBER, new HashedCell(this, new PhoneNumberCell()));
		projectionKeyToType.put(Calls.DATE, longCell());
		projectionKeyToType.put(Calls.TYPE, intCell());
		projectionKeyToType.put(Calls.DURATION, longCell());
		projectionKeyToType.put(Calls.CACHED_NAME, hashedStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_LABEL, hashedStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_TYPE, hashedStringCell());
		return projectionKeyToType;
	}
	
	
	protected String getDataName() {
		return CallLogKeys.CALLS;
	}


	@Override
	protected Uri getContentProviderUri() {
		return CallLog.Calls.CONTENT_URI;
	}


	@Override
	protected String getDateColumnName() {
		return Calls.DATE;
	}
	
	@Override
	protected long getDefaultPeriod() {
		return 36000;
	}

}
