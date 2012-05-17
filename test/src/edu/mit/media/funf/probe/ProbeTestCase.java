package edu.mit.media.funf.probe;

import android.test.AndroidTestCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;


public class ProbeTestCase<T extends Probe> extends AndroidTestCase {

	private Gson factory;
	private Class<T> probeClass;
	
	public ProbeTestCase(Class<T> probeClass) {
		this.probeClass = probeClass;
	}
	
	public Gson getFactory() {
		if (factory == null) {
			factory = new GsonBuilder().registerTypeAdapterFactory(FunfManager.getProbeFactory(getContext())).create();
		}
		return factory;
	}
	
	public T getProbe(JsonObject config) {
		return getFactory().fromJson(config, probeClass);
	}
}
