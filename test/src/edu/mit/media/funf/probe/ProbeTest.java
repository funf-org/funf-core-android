package edu.mit.media.funf.probe;

import java.util.concurrent.TimeUnit;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.util.Log;
import edu.mit.media.funf.tests.ExampleService;

public class ProbeTest extends AndroidTestCase {

	
	public void testPendingIntentDataTransfer() throws InterruptedException {
		//Intent i = new Intent("edu.mit.media.funf.REQUEST");
		//i.setType("edu.mit.media.funf.DATA/Location");
		//i.putExtra("PARAMETERS", new Bundle[]{ new Bundle() });
		Intent callbackIntent = new Intent(getContext(), ExampleService.class);
		PendingIntent pi = PendingIntent.getService(getContext(), 0, callbackIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//i.putExtra("CALLBACK", pi);
		
		Log.i("FunfTest", pi.getTargetPackage());
		int resultCode = 0;
		Intent dataIntent = new Intent();
		dataIntent.putExtra("DATA1", 0);
		try {
			pi.send(getContext(), resultCode, dataIntent);
		} catch (CanceledException e) {
			// remove pi from send list
			Log.i("FunfTest", "Canceled pending intent");
			fail("Canceled pending intent");
		}
		
		pi.cancel();
		
		try {
			pi.send(getContext(), resultCode, dataIntent);
		} catch (CanceledException e) {
			// remove pi from send list
			Log.i("FunfTest", "Canceled pending intent");
		}
		
		Thread.sleep(5000L);
	}
	
	
	public void testStartServiceIntentOrder() {
		Intent intent = new Intent(getContext(), ExampleService.class);
		for (int i=0; i<50; i++) {
			intent.putExtra("ORDER", i);
			getContext().startService(intent);
		}
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testTimeUnit() {
		assertEquals(1311937252000L, TimeUnit.SECONDS.convert(1311937252000L, TimeUnit.SECONDS));
		assertEquals(1311937252, TimeUnit.SECONDS.convert(1311937252000L, TimeUnit.MILLISECONDS));
	}
}
