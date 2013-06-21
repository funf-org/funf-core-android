/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Klemen Peternel (klemen.peternel@gmail.com)
 *            Pararth Shah    (pararthshah717@gmail.com)
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

import com.google.gson.JsonObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;

public class CallStateProbe extends Base implements PassiveProbe {
	
	private BroadcastReceiver callStateReceiver;

	@Override
	protected void onEnable() {
		IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		callStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
					String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		    		if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
		    			getLastCallDetails();
			    	}
				}
			}
		};
		getContext().registerReceiver(callStateReceiver, filter);
	}
	
	private void getLastCallDetails() {

		try {
			Thread.sleep(1000);
	        Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
	                null, null, null);
	        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
	        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
	        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
	        
	        cursor.moveToLast();
	        
	        boolean inPhoneBook = false;
	        String phNumber = cursor.getString(number);
	        String callType = cursor.getString(type);
	        String callDuration = cursor.getString(duration);
	        String dir = null;
	        int dircode = Integer.parseInt(callType);
	        switch (dircode) {
	        	case CallLog.Calls.OUTGOING_TYPE:
	        		dir = "OUTGOING";
	                break;
	
	            case CallLog.Calls.INCOMING_TYPE:
	                dir = "INCOMING";
	                break;
	
	            case CallLog.Calls.MISSED_TYPE:
	                dir = "MISSED";
	                break;
	        }
	        cursor.close();
	        
	        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
					Uri.encode(phNumber));
			Cursor cursor1 = getContext().getContentResolver().query(uri, new String[] 
			        {PhoneLookup.DISPLAY_NAME}, null, null, null);
			if (cursor1.moveToFirst()) {
				if (cursor1.getString(0) != null || cursor1.getString(0) != "") {
					inPhoneBook = true;
				}			
			}
			cursor1.close();
			
			JsonObject data = new JsonObject();
			data.addProperty("CALL_TYPE", dir);
			data.addProperty("CID", phNumber);
			data.addProperty("IS_IN_PHONEBOOK", inPhoneBook);
			data.addProperty("DURATION", callDuration);
			sendData(data);
		}
		catch (Exception e) {
			// Do nothing for now...
		}
    }

	@Override
	protected void onDisable() {
		getContext().unregisterReceiver(callStateReceiver);
		super.onDisable();
	}

	@Override
	protected void onStop() {
		// Only passive listener
	}
	
}