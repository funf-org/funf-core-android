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
package edu.mit.media.funf.storage;

import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class NameValueProbeDataListener extends BroadcastReceiver {
	
	private final String databaseName;
	private final Class<? extends DatabaseService> databaseServiceClass;
	private final BundleSerializer bundleSerializer;
	
	public NameValueProbeDataListener(String databaseName, Class<? extends DatabaseService> databaseServiceClass, BundleSerializer bundleSerializer) {
		this.databaseName = databaseName;
		this.databaseServiceClass = databaseServiceClass;
		this.bundleSerializer = bundleSerializer;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (isDataAction(action)) {
			String dataJson = bundleSerializer.serialize(intent.getExtras());
			String probeName = getProbeName(action);
			long timestamp = intent.getLongExtra("TIMESTAMP", 0L);
			Bundle b = new Bundle();
			b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, databaseName);
			b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(NameValueDatabaseService.NAME_KEY, probeName);
			b.putString(NameValueDatabaseService.VALUE_KEY, dataJson);
			Intent i = new Intent(context, databaseServiceClass);
			i.setAction(DatabaseService.ACTION_RECORD);
			i.putExtras(b);
			context.startService(i);
		}
	}
	
	////////////////////////////
	// OPP
	////////////////////////////
	
	public static final String ACTION_SEPERATOR = ".";
	public static final String ACTION_POLL = "POLL";
	public static final String ACTION_STATUS = "STATUS";
	public static final String ACTION_REQUEST = "REQUEST";
	public static final String ACTION_DATA = "DATA";
	
	// TODO: make this an OPP namespace
	public static final String OPP_NAMESPACE = "edu.mit.media.funf";
	
	
	/**
	* @param action
	* @return the OPP action that is being invoked with action
	*/
	public static String getOppAction(String action) {
	if (action == null) {
	return null;
	}
	final String[] dividedAction = action.split(Pattern.quote(ACTION_SEPERATOR));
	final String oppAction = dividedAction[dividedAction.length - 1];
	if ( oppAction.equals(ACTION_DATA) || oppAction.equals(ACTION_REQUEST) || oppAction.equals(ACTION_POLL) || oppAction.equals(ACTION_STATUS)) {
	return oppAction;
	} else {
	return null;
	}
	}
	
	/**
	* @param action
	* @return the probe that is being invoked with action
	*/
	public static String getProbeName(final String action) {
	final String oppAction = getOppAction(action);
	// Remove .<oppAction> at the end of action
	final String ending = ACTION_SEPERATOR + oppAction;
	if (oppAction != null && action.endsWith(ending)) {
	return action.substring(0, action.length() - ending.length());
	} else {
	return null;
	}
	}
	
	
	// POLL
	
	/**
	* @return OPP Global status request action which all available probes will respond to
	*/
	public static String getGlobalPollAction() {
	return OPP_NAMESPACE + ACTION_SEPERATOR + ACTION_POLL;
	}
	
	/**
	* @return OPP Status request action
	*/
	public static String getPollAction(String probeName) {
	// TODO: make this an OPP name
	return probeName + ACTION_SEPERATOR + ACTION_POLL;
	}
	
	/**
	* @return OPP Status request action
	*/
	public static String getPollAction(Class<?> probeClass) {
	// TODO: make this an OPP name
	return getPollAction(probeClass.getName());
	}
	
	public static boolean isPollAction(final String action) {
	return ACTION_POLL.equals(getOppAction(action));
	}
	
	// STATUS
	
	/**
	* @return OPP status action
	*/
	public static String getStatusAction(String probeName) {
	return getStatusAction();
	}
	
	/**
	* @return OPP status action
	*/
	public static String getStatusAction(Class<?> probeClass) {
	return getStatusAction();
	}
	
	public static String getStatusAction() {
	return OPP_NAMESPACE + ACTION_SEPERATOR + ACTION_STATUS;
	}
	
	public static boolean isStatusAction(final String action) {
	return getStatusAction().equals(action);
	}
	
	
	
	/**
	* @param probeClass
	* @return OPP Data action for probe with class
	*/
	public static String getDataAction(Class<?> probeClass) {
	return NameValueProbeDataListener.getDataAction(probeClass.getName());
	}
	
	public static String getDataAction(String probeName) {
	return probeName + ACTION_SEPERATOR + ACTION_DATA;
	}
	
	public static boolean isDataAction(final String action) {
	return ACTION_DATA.equals(getOppAction(action));
	}
	
	/**
	* @param probeClass
	* @return  OPP Data action for probe with class
	*/
	public static String getGetAction(String probeName) {
	return probeName + ACTION_SEPERATOR + ACTION_REQUEST;
	}
	
	public static String getGetAction(Class<?> probeClass) {
	return getGetAction(probeClass.getName());
	}
	
	public static boolean isGetAction(final String action) {
	return action != null && action.endsWith(ACTION_SEPERATOR + ACTION_REQUEST);
	}
}
