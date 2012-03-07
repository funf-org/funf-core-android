package edu.mit.media.funf.probe;

import static edu.mit.media.funf.Utils.TAG;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonObject;

/**
 * Responsible for initializing a probe, including setting Context, configuration, and a factory
 * for creating more probes.
 *
 */
public interface ProbeFactory {

	/**
	 * Returns the probe specified by the given name and configuration.  Name should be the fully qualified java class
	 * name of the probe.  (e.g. "edu.mit.media.funf.builtin.LocationProbe")
	 * @param name
	 * @param config
	 * @return
	 */
	public Probe getProbe(String name, JsonObject config);
	
	/**
	 * Returns the probe specified by the class and configuration.
	 * 
	 * @param probeClass
	 * @param config
	 * @return
	 */
	public <T extends Probe> T getProbe(Class<? extends T> probeClass, JsonObject config);
	
	/**
	 * Returns the probe specified by the uri.  (e.g. "probe://edu.mit.media.funf.builtin.LocationProbe")
	 * 
	 * @param probeClass
	 * @param config
	 * @return
	 */
	public Probe getProbe(Uri probeUri);
	
	
	public class BasicProbeFactory implements ProbeFactory {

		private Context context;
		private BasicProbeFactory(Context context) {
			if (context == null) {
				throw new RuntimeException("Context is required for BasicProbeFactory");
			}
			this.context = context.getApplicationContext();
		}
		
		private static BasicProbeFactory instance;
		public static BasicProbeFactory getInstance(Context context) {
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
		public Probe getProbe(Uri probeUri) {
			return getProbe(Probe.Identifier.getProbeName(probeUri), Probe.Identifier.getConfig(probeUri));
		}
		
		@Override
		public Probe getProbe(String name, JsonObject config) {
			Class<? extends Probe> probeClass = Probe.Base.getProbeClass(name);
			return probeClass == null ? null : getProbe(probeClass, config);
		}

		@Override
		public <T extends Probe> T getProbe(Class<? extends T> probeClass, JsonObject config) {
			try {
				T probe = probeClass.newInstance();
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
	
	/****************************************
	 * Caching Probe Factory
	 *****************************************/
	public class CachingProbeFactory implements ProbeFactory {
		private BasicProbeFactory basicFactory;
		private Map<Class<? extends Probe>,Map<JsonObject,Probe>> cache;  // (ProbeClass,Config) -> Probe
		
		private CachingProbeFactory(Context context) {
			if (context == null) {
				throw new RuntimeException("Context is required for BasicProbeFactory");
			}
			this.basicFactory =BasicProbeFactory.getInstance(context);
			this.cache = new HashMap<Class<? extends Probe>, Map<JsonObject,Probe>>();
		}
		
		private static CachingProbeFactory instance;
		public static CachingProbeFactory getInstance(Context context) {
			if (instance == null) {
				synchronized (CachingProbeFactory.class) {
					if (instance == null) {
						instance = new CachingProbeFactory(context);
					}
				}
			}
			return instance;
		}
		
		/**
		 * Returns a cached probe of the same class and config, or null if it doesn't exist.
		 * @param probeClass
		 * @param config
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private <T extends Probe> T getCachedProbe(Class<? extends T> probeClass, JsonObject config) {
			Map<JsonObject,Probe> cacheByConfig = cache.get(probeClass);
			return (cacheByConfig == null) ? null : (T)cacheByConfig.get(config);
		}
		
		/**
		 * Cache the probe, overwriting any existing cached probe.
		 * @param probe
		 * @param config
		 */
		private void cacheProbe(Probe probe, JsonObject config) {
			synchronized (cache) {
				Class<? extends Probe> probeClass = probe.getClass();
				Map<JsonObject,Probe> cacheByConfig = cache.get(probeClass);
				if (cacheByConfig == null) {
					cacheByConfig = new WeakHashMap<JsonObject, Probe>();
					cache.put(probeClass, cacheByConfig);
				}
				cacheByConfig.put(config, probe);
			}
		}
		
		@Override
		public Probe getProbe(Uri probeUri) {
			return getProbe(Probe.Identifier.getProbeName(probeUri), Probe.Identifier.getConfig(probeUri));
		}
		
		@Override
		public Probe getProbe(String name, JsonObject config) {
			Class<? extends Probe> probeClass = Probe.Base.getProbeClass(name);
			return getProbe(probeClass, config);
		}
	
		@Override
		public <T extends Probe> T getProbe(Class<? extends T> probeClass, JsonObject config) {
			T probe = getCachedProbe(probeClass, config);
			if (probe == null) { // Avoid synchronized block on every call
				synchronized (cache) {
					probe = getCachedProbe(probeClass, config);
					if (probe == null) {
						probe = basicFactory.getProbe(probeClass, config);
						if (probe != null) {
							cacheProbe(probe, config);
						}
					}
				}
			}
			return probe;
		}
	}
}
