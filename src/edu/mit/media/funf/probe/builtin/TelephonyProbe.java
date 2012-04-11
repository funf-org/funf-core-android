package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.google.gson.JsonObject;

import edu.mit.media.funf.DataNormalizer.PhoneNumberNormalizer;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TelephonyKeys;

@RequiredPermissions(android.Manifest.permission.READ_PHONE_STATE)
@RequiredFeatures("android.hardware.telephony")
@DefaultSchedule(period=604800)
@DisplayName("Mobile Network Info Probe")
public class TelephonyProbe extends Base implements TelephonyKeys {

	@Override
	protected void onStart() {
		super.onStart();
		TelephonyManager telephony = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		JsonObject data = new JsonObject();
		data.addProperty(CALL_STATE, telephony.getCallState());
		data.addProperty(DEVICE_ID, telephony.getDeviceId());
		data.addProperty(DEVICE_SOFTWARE_VERSION, telephony.getDeviceSoftwareVersion());
		data.addProperty(LINE_1_NUMBER, sensitiveData(telephony.getLine1Number(), new PhoneNumberNormalizer()));
		data.addProperty(NETWORK_COUNTRY_ISO, telephony.getNetworkCountryIso());
		data.addProperty(NETWORK_OPERATOR, telephony.getNetworkOperator());
		data.addProperty(NETWORK_OPERATOR_NAME, telephony.getNetworkOperatorName());
		data.addProperty(NETWORK_TYPE, telephony.getNetworkType());
		data.addProperty(PHONE_TYPE, telephony.getPhoneType());
		data.addProperty(SIM_COUNTRY_ISO, telephony.getSimCountryIso());
		data.addProperty(SIM_OPERATOR, telephony.getSimOperator());
		data.addProperty(SIM_OPERATOR_NAME, telephony.getSimOperatorName());
		data.addProperty(SIM_SERIAL_NUMBER, telephony.getSimSerialNumber()); // TODO: Should this be hashed?
		data.addProperty(SIM_STATE, telephony.getSimState());
		data.addProperty(SUBSCRIBER_ID, telephony.getSubscriberId());  // TODO: Should this be hashed?
		data.addProperty(VOICEMAIL_ALPHA_TAG, telephony.getVoiceMailAlphaTag());
		data.addProperty(VOICEMAIL_NUMBER, telephony.getVoiceMailNumber());
		data.addProperty(HAS_ICC_CARD, telephony.hasIccCard());
		sendData(data);
		stop();
	}

	
	
}
