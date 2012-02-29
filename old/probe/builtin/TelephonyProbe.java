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

import android.os.Bundle;
import android.telephony.TelephonyManager;
import edu.mit.media.funf.HashUtil;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TelephonyKeys;

public class TelephonyProbe extends SynchronousProbe implements TelephonyKeys {

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.READ_PHONE_STATE,
			};
	}
	
	@Override
	public String[] getRequiredFeatures() {
		return new String[] {
			"android.hardware.telephony"
		};
	}
	
	@Override
	protected long getDefaultPeriod() {
		return 604800L;
	}
	
	@Override
	protected String getDisplayName() {
		return "Mobile Network Info Probe";
	}

	@Override
	public Bundle getData() {
		Bundle data = new Bundle();
		TelephonyManager telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		// TODO: Belongs with CellProbe, or separate probe, these are volatile values
		//data.putInt("CELL_LOCATION", telephony.getCellLocation().fill);  
		//data.putInt("DATA_ACTIVITY", telephony.getDataActivity()); 
		//data.putInt("DATA_STATE", telephony.getDataState()); 
		//data.putParcelableArray("NEIGHBORING_CELL_INFO", telephony.getNeighboringCellInfo());
		//data.putBoolean("IS_NETWORK_ROAMING", telephony.isNetworkRoaming());
		data.putInt(CALL_STATE, telephony.getCallState());
		data.putString(DEVICE_ID, telephony.getDeviceId());
		data.putString(DEVICE_SOFTWARE_VERSION, telephony.getDeviceSoftwareVersion());
		data.putString(LINE_1_NUMBER, HashUtil.hashString(this, telephony.getLine1Number()));
		data.putString(NETWORK_COUNTRY_ISO, telephony.getNetworkCountryIso());
		data.putString(NETWORK_OPERATOR, telephony.getNetworkOperator());
		data.putString(NETWORK_OPERATOR_NAME, telephony.getNetworkOperatorName());
		data.putInt(NETWORK_TYPE, telephony.getNetworkType());
		data.putInt(PHONE_TYPE, telephony.getPhoneType());
		data.putString(SIM_COUNTRY_ISO, telephony.getSimCountryIso());
		data.putString(SIM_OPERATOR, telephony.getSimOperator());
		data.putString(SIM_OPERATOR_NAME, telephony.getSimOperatorName());
		data.putString(SIM_SERIAL_NUMBER, telephony.getSimSerialNumber()); // TODO: Should this be hashed?
		data.putInt(SIM_STATE, telephony.getSimState());
		data.putString(SUBSCRIBER_ID, telephony.getSubscriberId());  // TODO: Should this be hashed?
		data.putString(VOICEMAIL_ALPHA_TAG, telephony.getVoiceMailAlphaTag());
		data.putString(VOICEMAIL_NUMBER, telephony.getVoiceMailNumber());
		data.putBoolean(HAS_ICC_CARD, telephony.hasIccCard());
		return data;
	}

}
