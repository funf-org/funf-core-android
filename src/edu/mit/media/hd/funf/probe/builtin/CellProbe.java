package edu.mit.media.hd.funf.probe.builtin;

import android.content.Context;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import edu.mit.media.hd.funf.probe.Probe;

public class CellProbe extends Probe {

	public static final String TYPE = "type";
	private Bundle cellLocationInfo;
	private long timestamp;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(SystemParameter.PERIOD, 3600L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.ACCESS_COARSE_LOCATION
		};
	}

	@Override
	protected void onEnable() {
		// Nothing
	}
	
	@Override
	protected void onDisable() {
		// Nothing
	}

	@Override
	protected void onRun(Bundle params) {
		TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		CellLocation location = manager.getCellLocation();
		Bundle b = new Bundle();
		if (location instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) location;
			gsmLocation.fillInNotifierBundle(b);
			b.putInt(TYPE, TelephonyManager.PHONE_TYPE_GSM);
		} else if (location instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
			cdmaLocation.fillInNotifierBundle(b);
			b.putInt(TYPE, TelephonyManager.PHONE_TYPE_CDMA);
		} else {
			b.putInt(TYPE, TelephonyManager.PHONE_TYPE_NONE);
		}
		timestamp = System.currentTimeMillis();
		cellLocationInfo = b;
		sendProbeData();
	}

	@Override
	protected void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		if (cellLocationInfo != null) {
			Bundle data = new Bundle();
			data.putAll(cellLocationInfo);
			sendProbeData(timestamp, new Bundle(), data);
		}
	}

}
