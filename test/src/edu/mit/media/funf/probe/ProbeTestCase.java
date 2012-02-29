package edu.mit.media.funf.probe;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;


public class ProbeTestCase<T extends Probe> extends AndroidTestCase {

	private ProbeFactory factory;
	private Class<? extends Probe> probeClass;
	
	public ProbeTestCase(Class<? extends Probe> probeClass) {
		this.probeClass = probeClass;
	}
	
	public ProbeFactory getFactory() {
		if (factory == null) {
			factory = ProbeFactory.BasicProbeFactory.getInstance(getContext());
		}
		return factory;
	}
	
	@SuppressWarnings("unchecked")
	public T getProbe(JsonObject config) {
		return (T)getFactory().getProbe(probeClass, config);
	}
}
