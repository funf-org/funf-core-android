package edu.mit.media.funf.probe.builtin;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import edu.mit.media.funf.HashUtil;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TelephonyKeys;

public class TelephonyProbe extends SynchronousProbe implements TelephonyKeys {

	@Override
	public String[] getRequiredPermissions() {
		return null;
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
