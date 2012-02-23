package edu.mit.media.funf.probe2;

import static edu.mit.media.funf.Utils.TAG;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

/**
 * Responsible for initializing a probe, including setting Context, configuration, and a factory
 * for creating more probes.
 *
 */
public interface ProbeFactory {

	/**
	 * Returns the probe specified by the given name.  Name should be the fully qualified java class
	 * name of the probe.  (e.g. "edu.mit.media.funf.builtin.LocationProbe")
	 * @param name
	 * @param config
	 * @return
	 */
	public Probe getProbe(String name, JsonObject config);
	
	/**
	 * Returns the probe specified by the given name.  Name should be the fully qualified java class
	 * name of the probe.  (e.g. "edu.mit.media.funf.builtin.LocationProbe")
	 * 
	 * @param probeClass
	 * @param config
	 * @return
	 */
	public Probe getProbe(Class<? extends Probe> probeClass, JsonObject config);
	
	
	public class BasicProbeFactory implements ProbeFactory {

		private Context context;
		private BasicProbeFactory(Context context) {
			if (context == null) {
				throw new RuntimeException("Context is required for BasicProbeFactory");
			}
			this.context = context.getApplicationContext();
		}
		
		private static ProbeFactory instance;
		public static ProbeFactory getInstance(Context context) {
			if (instance == null) {
				synchronized (BasicProbeFactory.class) {
					if (instance == null) {
						instance = new BasicProbeFactory(context);
					}
				}
			}
			return instance;
		}
		
		@Override
		public Probe getProbe(String name, JsonObject config) {
			// TODO: Implement uri signatures, and find best available class
			try {
				Class<?> theClass = Class.forName(name);
				if (Probe.class.isAssignableFrom(theClass)) {
					Class<? extends Probe> probeClass = (Class<? extends Probe>)theClass;
					return getProbe(probeClass, config);
				}
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "Probe does not exist: '" + name + "'", e);
			}
			return null;
		}

		@Override
		public Probe getProbe(Class<? extends Probe> probeClass, JsonObject config) {
			try {
				Probe probe = probeClass.newInstance();
				probe.setContext(context);
				probe.setProbeFactory(this);
				probe.setConfig(config);
				return probe;
			} catch (IllegalAccessException e) {
				Log.e(TAG, "Probe constructor not visible for '" + probeClass.getName() + "'", e);
			} catch (InstantiationException e) {
				Log.e(TAG, "Probe constructor not visible for '" + probeClass.getName() + "'", e);
			}
			return null;
		}
		
	}
}
