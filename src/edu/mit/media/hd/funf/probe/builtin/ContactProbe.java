package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import edu.mit.media.hd.funf.probe.ContentProviderProbe;
import edu.mit.media.hd.funf.probe.CursorCell;

public class ContactProbe extends ContentProviderProbe {

	public static final String CONTACT_DATA = "CONTACT_DATA";
	
	@Override
	protected Cursor getCursor(String[] projection) {
		return getContentResolver().query(
				ContactsContract.Data.CONTENT_URI,
                projection, 
                null, //Data.MIMETYPE + " IN ('" + Email.CONTENT_ITEM_TYPE + "')",
                null,//new String[] {"('" + Utils.join(Arrays.asList(Email.MIMETYPE, Event.MIMETYPE), "','") +"')"}, 
                null);//Data.CONTACT_ID + " ASC");
                
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
	
	
	
	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Data._ID, intCell());
		projectionKeyToType.put(Data.MIMETYPE, stringCell());
		projectionKeyToType.put(Data.RAW_CONTACT_ID, longCell());
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
		projectionKeyToType.put(Data.CONTACT_ID, longCell());
		//projectionKeyToType.put(Data.AGGREGATION_MODE, intCell());
		//projectionKeyToType.put(Data.DELETED, intCell());
		//projectionKeyToType.put(Data.DISPLAY_NAME, hashedStringCell());
		//projectionKeyToType.put(Data.PHOTO_ID, longCell());
		//projectionKeyToType.put(Data.IN_VISIBLE_GROUP, intCell());
		//projectionKeyToType.put(Data.TIMES_CONTACTED, intCell());
		//projectionKeyToType.put(Data.LAST_TIME_CONTACTED, intCell());
		//projectionKeyToType.put(Data.STARRED, intCell());
		return projectionKeyToType;
	}
	
	@Override
	protected Bundle parseDataBundle(Cursor cursor, String[] projection, Map<String, CursorCell<?>> projectionMap) {
		Bundle contactBundle = new Bundle();
		ArrayList<Bundle> dataBundles = new ArrayList<Bundle>();
		long originalContactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
		contactBundle.putLong(Data.CONTACT_ID, originalContactId);
		long contactId = originalContactId;
		boolean hasNext = true;
		do {
			dataBundles.add(super.parseDataBundle(cursor, projection, projectionMap));
			hasNext = cursor.moveToNext();
			if (hasNext) {
				contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
			}
		} while(hasNext && originalContactId == contactId);
		if (hasNext) {
			cursor.moveToPrevious();
		}
		contactBundle.putParcelableArrayList(CONTACT_DATA, dataBundles);
		return contactBundle;
	}
	
	@Override
	public void sendProbeData() {
		// Send individually
		if (mostRecentScan != null ) {
			long timestamp = getTimestamp(mostRecentScan);
			for (Bundle contactBundle : mostRecentScan) {
				sendProbeData(timestamp, new Bundle(), contactBundle);
			}
		}
	}

	@Override
	protected long getTimestamp(List<Bundle> results) {
		return System.currentTimeMillis();
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.READ_CONTACTS
		};
	}


}
