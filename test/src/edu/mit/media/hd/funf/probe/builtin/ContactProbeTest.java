package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import junit.framework.AssertionFailedError;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Data;
import android.util.Log;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.probe.Probe;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class ContactProbeTest extends ProbeTestCase<ContactProbe> {

	public ContactProbeTest() {
		super(ContactProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		params.putBoolean(ContactProbe.FULL_PARAM.getName(), true);
		startProbe(params);
		Bundle data = getData(30);
		assertNotNull(data.get(Probe.TIMESTAMP));
		ArrayList<Parcelable> contacts = data.getParcelableArrayList(ContactProbe.CONTACT_DATA);
		assertNotNull(contacts);
		int count = 1;
		while(data != null) {
			try {
				data = getData(1);
				count ++;
			} catch (AssertionFailedError e) {
				assertTrue(contacts.size() > 0);
				Log.i("ContactProbeTest", "Contact keys: " + Utils.join(Utils.getValues(data).keySet(), ", "));
				for (Parcelable dataRow : contacts) {
					Bundle b = (Bundle)dataRow;
					Log.i("ContactProbeTest", "Data keys: " + b.getString(Data.MIMETYPE) + ":" + String.valueOf(b.get(Data.DATA1)) + " - Others: " + Utils.join(Utils.getValues(b).keySet(), ", "));
				}
				data = null;
			}
		}
		Log.i("ContactProbeTest", "Count: " + String.valueOf(count));
	}
	
	public void testFullParameter() {
		// Run a full scan
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		params.putBoolean(ContactProbe.FULL_PARAM.getName(), true);
		startProbe(params);
		Bundle data = getData(30);
		boolean hasAtLeastTwo = false;
		while (data != null) {
			// Get the rest of the data
			try {
				data = getData(5);
				hasAtLeastTwo = true;
			} catch (AssertionFailedError e) {
				data = null;
			}
		}
		assertTrue(hasAtLeastTwo);
		
		// Run a non-full scan
		params.putBoolean(ContactProbe.FULL_PARAM.getName(), false);
		startProbe(params);
		try {
			getData(30);  // Unfortunately we have to wait, since full scans take a while to return anything
			fail("Should not get any contacts for a non-full scan after a full scan");
		} catch (AssertionFailedError e) {
			// Success
		}
	}
	
}
