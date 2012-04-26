package edu.mit.media.funf.config;



import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.util.LogUtil;

/**
 * Responsible for initializing a probe, including setting Context, configuration, and a factory
 * for creating more probes.
 *
 */
public interface ConfigurableObjectFactory {

	/**
	 * Returns the probe specified by the given name and configuration.  Name should be the fully qualified java class
	 * name of the probe.  (e.g. "edu.mit.media.funf.builtin.LocationProbe")
	 * @param name
	 * @param config
	 * @return
	 */
	public Configurable get(String name, JsonObject config);
	
	/**
	 * Returns the probe specified by the uri.  (e.g. "funf://edu.mit.media.funf.builtin.LocationProbe")
	 * 
	 * @param probeClass
	 * @param config
	 * @return
	 */
	public Configurable get(Uri probeUri);
	
	/**
	 * Returns the probe specified by the class and configuration.
	 * 
	 * @param probeClass
	 * @param config
	 * @return
	 */
	public <T extends Configurable> T get(Class<T> probeClass, JsonObject config);
	
	
	public static class FactoryUtils {

		public static <T extends Configurable> Class<? extends T> getClass(String className, Class<T> parentClass) {
			try {
				return (Class<? extends T>)Class.forName(className);
			} catch (ClassNotFoundException e) {
				Log.e(LogUtil.TAG, "Configurable class does not exist: '" + className + "'", e);
			} catch (ClassCastException e) {
				Log.e(LogUtil.TAG, "Class does not extend configurable: '" + className + "'", e);
			}
			return null;
		}
		
		public static Class<? extends Probe> getProbeClass(String className) {
			return getClass(className, Probe.class);
		}
	}
	
	public class BasicProbeFactory implements ConfigurableObjectFactory {

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
		public Configurable get(Uri probeUri) {
			return get(Probe.PROBE_URI.getName(probeUri), Probe.PROBE_URI.getConfig(probeUri));
		}
		
		@Override
		public Configurable get(String name, JsonObject config) {
			Class<? extends Configurable> probeClass = getClass(name);
			return probeClass == null ? null : get(probeClass, config);
		}

		@Override
		public <T extends Configurable> T get(Class<T> probeClass, JsonObject config) {
			try {
				T probe = probeClass.newInstance();
				probe.setContext(context);
				probe.setFactory(this);
				probe.setConfig(config);
				return probe;
			} catch (IllegalAccessException e) {
				Log.e(LogUtil.TAG, "Probe constructor not visible for '" + probeClass.getName() + "'", e);
			} catch (InstantiationException e) {
				Log.e(LogUtil.TAG, "Probe constructor not visible for '" + probeClass.getName() + "'", e);
			}
			return null;
		}

		public Class<? extends Configurable> getClass(String className) {
			return FactoryUtils.getClass(className, Configurable.class);
		}


	}
	
	/****************************************
	 * Caching Probe Factory
	 *****************************************/
	public class CachingProbeFactory implements ConfigurableObjectFactory {
		private BasicProbeFactory basicFactory;
		private Map<Class<? extends Configurable>,Map<JsonObject,Configurable>> cache;  // (ProbeClass,Config) -> Probe
		
		private CachingProbeFactory(Context context) {
			if (context == null) {
				throw new RuntimeException("Context is required for BasicProbeFactory");
			}
			this.basicFactory = new BasicProbeFactory(context);
			this.cache = new HashMap<Class<? extends Configurable>, Map<JsonObject,Configurable>>();
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
		private <T extends Configurable> T getCachedProbe(Class<T> probeClass, JsonObject config) {
			Map<JsonObject, Configurable> cacheByConfig = cache.get(probeClass);
			return (cacheByConfig == null) ? null : (T)cacheByConfig.get(config);
		}
		
		/**
		 * Cache the probe, overwriting any existing cached probe.
		 * @param probe
		 * @param config
		 */
		private void cacheProbe(Configurable probe, JsonObject config) {
			synchronized (cache) {
				Class<? extends Configurable> probeClass = (Class<? extends Configurable>)probe.getClass();
				Map<JsonObject, Configurable> cacheByConfig = cache.get(probeClass);
				if (cacheByConfig == null) {
					cacheByConfig = new WeakHashMap<JsonObject, Configurable>();
					cache.put(probeClass, cacheByConfig);
				}
				cacheByConfig.put(config, probe);
			}
		}
		
		@Override
		public Configurable get(Uri probeUri) {
			return get(Probe.PROBE_URI.getName(probeUri), Probe.PROBE_URI.getConfig(probeUri));
		}
		
		@Override
		public Configurable get(String name, JsonObject config) {
			Class<? extends Configurable> probeClass = basicFactory.getClass(name);
			return get(probeClass, config);
		}
	
		@Override
		public <T extends Configurable> T get(Class<T> probeClass, JsonObject config) {
			T probe = getCachedProbe(probeClass, config);
			if (probe == null) { // Avoid synchronized block on every call
				synchronized (cache) {
					probe = getCachedProbe(probeClass, config);
					if (probe == null) {
						probe = basicFactory.get(probeClass, config);
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
