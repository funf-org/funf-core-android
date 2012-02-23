package edu.mit.media.funf.probe2;

import com.google.gson.JsonObject;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class ProbeManager extends Service implements ProbeFactory {
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		basicFactory = ProbeFactory.BasicProbeFactory.getInstance(this);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	public void requestData(Probe.DataListener listener, JsonObject schedule, JsonObject config) {
		
	}
	
	public void unrequestData(Probe.DataListener listener, JsonObject schedule, JsonObject config) {
		
	}
	
	public void unrequestAllData(Probe.DataListener listener) {
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/****************************************
	 * Caching Probe Factory
	 *****************************************/
	
	private ProbeFactory basicFactory;
	
	@Override
	public Probe getProbe(String name, JsonObject config) {
		// Strip scheduling params from conifg, add name parameter
		// Find bundle that equals this bundle if it exists, then return that probe
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Probe getProbe(Class<? extends Probe> probeClass, JsonObject config) {
		// TODO Auto-generated method stub
		return null;
	}

}
