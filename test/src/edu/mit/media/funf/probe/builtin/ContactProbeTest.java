package edu.mit.media.funf.probe.builtin;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.ProbeTestCase;

public class ContactProbeTest extends ProbeTestCase<ContactProbe> {
  
  public ContactProbeTest() {
    super(ContactProbe.class);
  }

  public static final String TAG = "Probes";
  
  private DataListener listener = new DataListener() {
    @Override
    public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
        Log.i(TAG, "DATA: " + completeProbeUri.toString() + " " + data.toString());
    }

    @Override
    public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
        Log.i(TAG, "COMPLETE: " + completeProbeUri.toString());
    }
};
  
  public void testProbe() throws InterruptedException {
    ContactProbe probe = getProbe(new JsonObject());
    probe.registerListener(listener);
    Thread.sleep(100L);
  }
}
