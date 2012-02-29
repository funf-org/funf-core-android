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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.ContentProviderProbe;
import edu.mit.media.funf.probe.CursorCell;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ContactKeys;

public class ContactProbe extends ContentProviderProbe implements ContactKeys {

	public static final Parameter FULL_PARAM = new Parameter("FULL", false, "FullScan", "If true, probe will return all contacts.  If false, the scan will only return contacts that have changed since the most recent scan."); 
	
	private static final String DATA_VERSIONS = "dataVersions";
	private Map<Integer,Integer> dataIdToVersion;
	private boolean isFullScan;
	
	
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.PERIOD, 36000L),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L),
			FULL_PARAM
		};
	}
	
	@Override
	protected String getDisplayName() {
		return "Contacts Probe";
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		SharedPreferences versionPrefs = getSharedPreferences(DATA_VERSIONS, MODE_PRIVATE);
		dataIdToVersion = new HashMap<Integer, Integer>();
		for (Map.Entry<String, ?> versionEntry: versionPrefs.getAll().entrySet()) {
			dataIdToVersion.put(Integer.valueOf(versionEntry.getKey()), (Integer)versionEntry.getValue());
		}
		isFullScan = false;
	}
	
	@Override
	protected void onDisable() {
		super.onDisable();
		SharedPreferences.Editor versionPrefs = getSharedPreferences(DATA_VERSIONS, MODE_PRIVATE).edit();
		versionPrefs.clear();
		for (Map.Entry<Integer, Integer> idToVersion : dataIdToVersion.entrySet()) {
			versionPrefs.putInt(String.valueOf(idToVersion.getKey()), idToVersion.getValue());
		}
		versionPrefs.commit();
	}

	

	@Override
	protected Cursor getCursor(String[] projection) {
		return getContentResolver().query(
				ContactsContract.Data.CONTENT_URI,
                projection, 
                null, //Data.MIMETYPE + " IN ('" + Email.CONTENT_ITEM_TYPE + "')",
                null,//new String[] {"('" + Utils.join(Arrays.asList(Email.MIMETYPE, Event.MIMETYPE), "','") +"')"}, 
                Data.CONTACT_ID + " ASC");
                
	}

	@Override
	protected String getDataName() {
		return CONTACT_DATA;
	}

	class ContactDataCell extends CursorCell<Object> {
		
		private Map<String,CursorCell<?>> cursorCells;
		
		ContactDataCell() {
			cursorCells = new HashMap<String, CursorCell<?>>();
			
			// Email
			cursorCells.put(getKey(Email.CONTENT_ITEM_TYPE, Data.DATA1), hashedStringCell());
			cursorCells.put(getKey(Email.CONTENT_ITEM_TYPE, Email.TYPE), intCell());
			cursorCells.put(getKey(Email.CONTENT_ITEM_TYPE, Email.LABEL), stringCell());
			cursorCells.put(getKey(Email.CONTENT_ITEM_TYPE, Email.DISPLAY_NAME), hashedStringCell());
			
			// Event
			cursorCells.put(getKey(Event.CONTENT_ITEM_TYPE, Event.START_DATE), stringCell());
			cursorCells.put(getKey(Event.CONTENT_ITEM_TYPE, Event.TYPE), intCell());
			cursorCells.put(getKey(Event.CONTENT_ITEM_TYPE, Event.LABEL), hashedStringCell());
			
			// Group Membership
			cursorCells.put(getKey(Event.CONTENT_ITEM_TYPE, GroupMembership.GROUP_ROW_ID), longCell());
			
			// IM Address
			cursorCells.put(getKey(Im.CONTENT_ITEM_TYPE, Im.DATA), hashedStringCell());
			cursorCells.put(getKey(Im.CONTENT_ITEM_TYPE, Im.TYPE), intCell());
			cursorCells.put(getKey(Im.CONTENT_ITEM_TYPE, Im.LABEL), hashedStringCell());
			cursorCells.put(getKey(Im.CONTENT_ITEM_TYPE, Im.PROTOCOL), stringCell());
			cursorCells.put(getKey(Im.CONTENT_ITEM_TYPE, Im.CUSTOM_PROTOCOL), stringCell());
			
			// Nickname
			cursorCells.put(getKey(Nickname.CONTENT_ITEM_TYPE, Nickname.NAME), hashedStringCell());
			cursorCells.put(getKey(Nickname.CONTENT_ITEM_TYPE, Nickname.TYPE), intCell());
			cursorCells.put(getKey(Nickname.CONTENT_ITEM_TYPE, Nickname.LABEL), stringCell());
			
			// Note
			cursorCells.put(getKey(Note.CONTENT_ITEM_TYPE, Note.NOTE), hashedStringCell());
			// TODO: do we need notes?
			
			// Organization
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.COMPANY), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.TYPE), intCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.LABEL), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.TITLE), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.DEPARTMENT), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.JOB_DESCRIPTION), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.SYMBOL), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.PHONETIC_NAME), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Organization.OFFICE_LOCATION), hashedStringCell());
			cursorCells.put(getKey(Organization.CONTENT_ITEM_TYPE, Data.DATA10), stringCell()); // Phonetic_Name_Style
			
			// Phone
			cursorCells.put(getKey(Phone.CONTENT_ITEM_TYPE, Phone.NUMBER), new PhoneNumberCell());
			cursorCells.put(getKey(Phone.CONTENT_ITEM_TYPE, Phone.TYPE), intCell());
			cursorCells.put(getKey(Phone.CONTENT_ITEM_TYPE, Phone.LABEL), stringCell());
			
			// Photo (Skipped)
			cursorCells.put(getKey(Photo.CONTENT_ITEM_TYPE, Photo.PHOTO), new NullCell());
			
			// Relation
			cursorCells.put(getKey(Relation.CONTENT_ITEM_TYPE, Relation.NAME), hashedStringCell());
			cursorCells.put(getKey(Relation.CONTENT_ITEM_TYPE, Relation.TYPE), intCell());
			cursorCells.put(getKey(Relation.CONTENT_ITEM_TYPE, Relation.LABEL), stringCell());
			
			// SipAddress (API 9) Used defaults
			
			// StructuredName
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.DISPLAY_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.GIVEN_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.FAMILY_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.PREFIX), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.MIDDLE_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.SUFFIX), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.PHONETIC_GIVEN_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.PHONETIC_MIDDLE_NAME), hashedStringCell());
			cursorCells.put(getKey(StructuredName.CONTENT_ITEM_TYPE, StructuredName.PHONETIC_FAMILY_NAME), hashedStringCell());
			
			// Structured Postal
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.FORMATTED_ADDRESS), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.TYPE), intCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.LABEL), stringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.STREET), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.POBOX), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.NEIGHBORHOOD), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.CITY), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.REGION), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.POSTCODE), hashedStringCell());
			cursorCells.put(getKey(StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.COUNTRY), hashedStringCell());
			
			// Website
			cursorCells.put(getKey(Website.CONTENT_ITEM_TYPE, Website.URL), hashedStringCell());
			cursorCells.put(getKey(Website.CONTENT_ITEM_TYPE, Website.TYPE), intCell());
			cursorCells.put(getKey(Website.CONTENT_ITEM_TYPE, Website.LABEL), stringCell());
			
		}
		
		private String getKey(String mimeType, String columnName) {
			return mimeType + "__" + columnName;
		}
		
		@Override
		public Object getData(Cursor cursor, int columnIndex) {
			String mimeType = stringCell().getData(cursor,Data.MIMETYPE);
			String columnName = cursor.getColumnName(columnIndex);
			CursorCell<?> cursorCell = cursorCells.get(getKey(mimeType, columnName));
			if (cursorCell == null) {
				cursorCell = anyCell(); // Default
			}
			return cursorCell.getData(cursor, columnIndex);
		}	
	}
	
	private ContactDataCell contactDataCell;
	protected ContactDataCell contactDataCell() {
		if (contactDataCell == null) {
			contactDataCell = new ContactDataCell();
		}
		return contactDataCell;
	}
	
	private Map<String, CursorCell<?>> dataProjectionMap;
	private Map<String, CursorCell<?>> getDataProjectionMap() {
		if (dataProjectionMap == null) {
			Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
			projectionKeyToType.put(Data._ID, intCell());
			projectionKeyToType.put(Data.RAW_CONTACT_ID, longCell());
			projectionKeyToType.put(Data.MIMETYPE, stringCell());
			projectionKeyToType.put(Data.IS_PRIMARY, intCell());
			projectionKeyToType.put(Data.IS_SUPER_PRIMARY, intCell());
			projectionKeyToType.put(Data.DATA_VERSION, intCell());
			projectionKeyToType.put(Data.DATA1, contactDataCell());
			projectionKeyToType.put(Data.DATA2, contactDataCell());
			projectionKeyToType.put(Data.DATA3, contactDataCell());
			projectionKeyToType.put(Data.DATA4, contactDataCell());
			projectionKeyToType.put(Data.DATA5, contactDataCell());
			projectionKeyToType.put(Data.DATA6, contactDataCell());
			projectionKeyToType.put(Data.DATA7, contactDataCell());
			projectionKeyToType.put(Data.DATA8, contactDataCell());
			projectionKeyToType.put(Data.DATA9, contactDataCell());
			projectionKeyToType.put(Data.DATA10, contactDataCell());
			projectionKeyToType.put(Data.DATA11, contactDataCell());
			projectionKeyToType.put(Data.DATA12, contactDataCell());
			projectionKeyToType.put(Data.DATA13, contactDataCell());
			projectionKeyToType.put(Data.DATA14, contactDataCell());
			projectionKeyToType.put(Data.DATA15, contactDataCell());
			dataProjectionMap = projectionKeyToType;
		}
		return dataProjectionMap;
	}
	
	private Map<String, CursorCell<?>> contactProjectionMap;
	private Map<String, CursorCell<?>> getContactProjectionMap() {
		if (contactProjectionMap == null) {
			Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
			projectionKeyToType.put(Data.CONTACT_ID, longCell());
			//projectionKeyToType.put(Data.AGGREGATION_MODE, intCell());  Doesn't exist for some reason
			//projectionKeyToType.put(Data.DELETED, intCell());  Doesn't exist for some reason
			projectionKeyToType.put(Data.LOOKUP_KEY, stringCell());
			projectionKeyToType.put(Data.DISPLAY_NAME, hashedStringCell());
			projectionKeyToType.put(Data.PHOTO_ID, longCell());
			projectionKeyToType.put(Data.IN_VISIBLE_GROUP, intCell());
			projectionKeyToType.put(Data.TIMES_CONTACTED, intCell());
			projectionKeyToType.put(Data.LAST_TIME_CONTACTED, intCell());
			projectionKeyToType.put(Data.STARRED, intCell());
			projectionKeyToType.put(Data.CUSTOM_RINGTONE, hashedStringCell());
			projectionKeyToType.put(Data.SEND_TO_VOICEMAIL, intCell());
			contactProjectionMap = projectionKeyToType;
		}
		return contactProjectionMap;
	}
	
	
	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.putAll(getDataProjectionMap());
		projectionKeyToType.putAll(getContactProjectionMap());
		return projectionKeyToType;
	}
	
	
	
	@Override
	protected void onRun(Bundle params) {
		isFullScan = params.getBoolean(FULL_PARAM.getName(), (Boolean)FULL_PARAM.getValue());

		Log.i(TAG, "Full scan = " + isFullScan);
		super.onRun(params);
	}
	

	@Override
	protected Bundle parseDataBundle(Cursor cursor, String[] projection, Map<String, CursorCell<?>> projectionMap) {
		Bundle contactBundle = super.parseDataBundle(cursor, projection, getContactProjectionMap());
		
		ArrayList<Bundle> dataBundles = new ArrayList<Bundle>();
		long originalContactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
		long contactId = originalContactId;
		boolean hasNext = true;
		boolean hasChanged = isFullScan;
		do {
			Bundle dataBundle = super.parseDataBundle(cursor, projection, getDataProjectionMap());
			dataBundles.add(dataBundle);
			int id = dataBundle.getInt(Data._ID);
			int version = dataBundle.getInt(Data.DATA_VERSION);
			Integer oldVersion = dataIdToVersion.get(id);
			if (oldVersion == null || !oldVersion.equals(version)) {
				hasChanged = true;
				dataIdToVersion.put(id, version);
			}
			hasNext = cursor.moveToNext();
			if (hasNext) {
				contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
			}
		} while(hasNext && originalContactId == contactId);
		if (hasNext) {
			cursor.moveToPrevious();
		}
		contactBundle.putParcelableArrayList(CONTACT_DATA, dataBundles);
		return hasChanged ? contactBundle : null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.READ_CONTACTS
		};
	}
	
	@Override
	protected long getTimestamp(List<Bundle> results) {
		return Utils.getTimestamp();
	}

	@Override
	protected long getTimestamp(Bundle result) {
		return Utils.getTimestamp();
	}

	@Override
	protected boolean sendEachRowSeparately() {
		return false;
	}


	
}
