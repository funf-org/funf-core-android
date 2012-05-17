package edu.mit.media.funf;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.AndroidTestCase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LazilyParsedNumber;

import edu.mit.media.funf.json.IJsonArray;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.AccelerometerFeaturesProbe;

public class FunfManagerTest extends AndroidTestCase {

	private JsonParser parser = new JsonParser();
	private FunfManager manager;
	private ServiceConnection conn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			manager = ((FunfManager.LocalBinder)service).getManager();
		}
	};
	
	public class SampleDataListener implements DataListener {
		
		public BlockingQueue<IJsonObject> dataEvents = new LinkedBlockingQueue<IJsonObject>();
		public BlockingQueue<IJsonObject> completeEvents = new LinkedBlockingQueue<IJsonObject>();
		
		@Override
		public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
			dataEvents.add(data);
		}
		
		@Override
		public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
			completeEvents.add(probeConfig);
		}
	};
	

	
	public SampleDataListener listener = new SampleDataListener();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		listener = new SampleDataListener();
		getContext().bindService(new Intent(getContext(), FunfManager.class), conn, Context.BIND_AUTO_CREATE);
		waitForServiceConnection(3000);
	}
	
	public void waitForServiceConnection(long millisToWait) {
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + millisToWait) {
			if (manager != null) {
                break;
            } else {
	            try {
	                Thread.sleep(100);
	            }
	            catch (InterruptedException e) {
	            }
            }
        }
	}

	private final String probeConfig = "{\"@type\": \"" + AccelerometerFeaturesProbe.class.getName() + "\" " +
				
			"}";
	
	public void testDataRequest() {
		manager.requestData(listener, parser.parse(probeConfig));
		IJsonObject data = null;
		try {
			data = listener.dataEvents.poll(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		if (data == null) {
			fail("Should have returned data within time alloted");
		}
		
		manager.unrequestData(listener, parser.parse(probeConfig));
		IJsonObject complete = null;
		try {
			complete = listener.completeEvents.poll(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		if (complete == null) {
			fail("Should have completed within time alloted");
		}
	}
	
	public void testIJsonObjectEqualsAndHash() {
		JsonElement p1 = JsonUtils.immutable(new JsonPrimitive(new LazilyParsedNumber("1")));
		JsonElement p2 = JsonUtils.immutable(new JsonPrimitive(1));
		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
		
		Gson gson = manager.getGson();
		Probe probe = gson.fromJson(probeConfig, Probe.class);
		IJsonObject o1 = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));
		IJsonArray a1 = o1.getAsJsonArray("freqBandEdges");
		IJsonArray a2 = (IJsonArray)JsonUtils.immutable(new JsonParser().parse(gson.toJson(a1)));
		
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
		IJsonObject o2 = (IJsonObject)JsonUtils.immutable(new JsonParser().parse(gson.toJson(o1)));
		assertEquals(o1, o2);
		assertEquals(o1.hashCode(), o2.hashCode());
	}
	
	public void testRegisterPipeline() {
		
	}
}
