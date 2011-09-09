package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CellKeys;

public class CellProbe extends SynchronousProbe implements CellKeys {
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.ACCESS_COARSE_LOCATION
		};
	}
	
	@Override
	protected Bundle getData() {
		TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		CellLocation location = manager.getCellLocation();
		Bundle data = new Bundle();
		if (location instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) location;
			gsmLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_GSM);
		} else if (location instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
			cdmaLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_CDMA);
		} else {
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_NONE);
		}
		return data;
	}

}
