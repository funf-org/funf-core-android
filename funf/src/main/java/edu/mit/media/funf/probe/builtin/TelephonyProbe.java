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

import android.content.Context;
import android.telephony.TelephonyManager;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.data.DataNormalizer.PhoneNumberNormalizer;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TelephonyKeys;

@RequiredPermissions(android.Manifest.permission.READ_PHONE_STATE)
@RequiredFeatures("android.hardware.telephony")
@Schedule.DefaultSchedule(interval=604800)
@DisplayName("Mobile Network Info Probe")
public class TelephonyProbe extends ImpulseProbe implements TelephonyKeys {

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
