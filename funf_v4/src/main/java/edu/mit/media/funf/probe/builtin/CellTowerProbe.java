/**
 * 
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
 * 
 */
package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CellKeys;

@DisplayName("Nearby Cellular Towers Probe")
@RequiredFeatures("android.hardware.telephony")
@RequiredPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)
public class CellTowerProbe extends Base implements CellKeys {

	@Override
	protected void onStart() {
		super.onStart();
		sendData(getGson().toJsonTree(getData()).getAsJsonObject());
		stop();
	}
	
	private Bundle getData() {
		TelephonyManager manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		CellLocation location = manager.getCellLocation();
		Bundle data = new Bundle();
		if (location instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) location;
			gsmLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_GSM);
			String networkOperator = manager.getNetworkOperator();
			int mcc = 0;
			int mnc = 0;
			if (networkOperator != null && !networkOperator.isEmpty()) {
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
				mnc = Integer.parseInt(networkOperator.substring(3));
			}
			data.putInt("mcc", mcc);
			data.putInt("mnc", mnc);
		} else if (location instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
			cdmaLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_CDMA);
			String networkOperator = manager.getNetworkOperator();
			int mcc = 0;
			int mnc = 0;
			if (networkOperator != null && !networkOperator.isEmpty()) {
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
				mnc = Integer.parseInt(networkOperator.substring(3));
			}
			data.putInt("mcc", mcc);
			data.putInt("mnc", mnc);
		} else {
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_NONE);
		}
		return data;
	}
	
}
