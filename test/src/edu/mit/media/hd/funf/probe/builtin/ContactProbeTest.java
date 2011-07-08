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
		startProbe(params);
		Bundle data = getData(30);
		assertNotNull(data.get(Probe.TIMESTAMP));
		ArrayList<Parcelable> contactData = data.getParcelableArrayList(ContactProbe.CONTACT_DATA);
		assertNotNull(contactData);
		int count = 1;
		while(data != null) {
			try {
				data = getData(1);
				count ++;
			} catch (AssertionFailedError e) {
				assertTrue(contactData.size() > 0);
				for (Parcelable dataRow : contactData) {
					Bundle b = (Bundle)dataRow;
					Log.i("ContactProbeTest", "Data keys: " + b.getString(Data.MIMETYPE) + ":" + String.valueOf(b.get(Data.DATA1)) + " - Others: " + Utils.join(Utils.getValues(b).keySet(), ", "));
				}
				data = null;
			}
		}
		Log.i("ContactProbeTest", "Count: " + String.valueOf(count));
	}
	
}
