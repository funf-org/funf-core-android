package edu.mit.media.funf.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeFactory;
import edu.mit.media.funf.util.AnnotationUtil;
import edu.mit.media.funf.util.LogUtil;

public interface Configurable {

	/**
	 * Changes the configuration for this probe.  Setting the configuration will disable the probe.
	 * @param config
	 */
	public void setConfig(JsonObject config);
	
	/**
	 * @return A copy of the current specified configuration for this probe.
	 */
	public JsonObject getConfig();
	
	/**
	 * @return A copy of all configuration parameters, including default ones.
	 */
	public JsonObject getCompleteConfig();
	
	/**
	 * @return A uri that represents this configurable object as configuration was specified
	 */
	public Uri getUri();
	
	/**
	 * @return  A uri that represents this configurable object with default values included
	 */
	public Uri getCompleteUri();
	
	
	
	
	/**
	 * Used to indicate that a particular field is configurable.
	 * The name of the field will be used as the configuration name, and the 
	 * default value will be calculated by creating an instance with no config
	 * and inspecting what value gets created.
	 *
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(FIELD)
	@Inherited
	public @interface ConfigurableField {
		/**
		 * @return Overrides the field name, if specified.
		 */
		String name() default "";
	}

	public static class ConfigurableUri<T extends Configurable> {
		
		private final String scheme;
		
		
		public ConfigurableUri(String scheme) {
			assert scheme != null;
			this.scheme = scheme;
		}
		
		/**
		 * A consistent Uri for the probe class and config pair.  This can be used as an identifier for a probe with a specific config.
		 * Need to ensure that your config string is consistently sorted.
		 * @return
		 */
		public Uri getUri(String probeName, String config) {
			String path = config == null ? "" : "/" + config;
			return new Uri.Builder()
					.scheme(scheme)
					.authority(probeName)
					.path(path)
					.build();
		}
		
		/**
		 * A consistent Uri for the probe class and config pair.  This can be used as an identifier for a probe with a specific config.
		 * Will sort the JsonObject before serializing to the Uri, to ensure a consistent Uri.
		 * @param probeName
		 * @param config
		 * @return
		 */
		public Uri getUri(String probeName, JsonObject config) {
			return getUri(probeName, 
					(config == null) ? null : JsonUtils.deepSort(config).toString());
		}

		/**
		 * A consistent Uri for the probe class and config pair.  This can be used as an identifier for a probe with a specific config.
		 * Will sort the JsonObject before serializing to the Uri, to ensure a consistent Uri.
		 * @param probeClass
		 * @param config
		 * @return
		 */
		public Uri getUri(Class<? extends T> probeClass, JsonObject config) {
			return getUri(probeClass.getName(), config);
		}
		
		/**
		 * A consistent Uri for the probe class and config pair.  This can be used as an identifier for a probe with a specific config.
		 * @param configurableClass
		 * @param config
		 * @return
		 */
		public Uri getUri(Class<? extends T> configurableClass, String config) {
			return getUri(configurableClass.getName(), config);
		}
		
		/**
		 * A consistent Uri for the probe class and config pair.  This can be used as an identifier for the probe instance.
		 * Will sort the JsonObject before serializing to the Uri, to ensure a consistent Uri.
		 * @param configurable
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public Uri getUri(T configurable) {
			return getUri((Class<? extends T>)configurable.getClass(), configurable.getConfig());
		}
		
		/**
		 * Returns a URI that represents a configurable object with all of its 
		 * defaults specified.
		 * @param configurable
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public Uri getCompleteUri(T configurable) {
			return getUri((Class<? extends T>)configurable.getClass(), configurable.getCompleteConfig());
		}
		
		/**
		 * Returns true if Uri is a probe Uri.
		 * @param configurableUri
		 * @return
		 */
		public boolean isConfigurableUri(Uri configurableUri) {
			return scheme.equalsIgnoreCase(configurableUri.getScheme());
		}
		
		private void verifyConfigurableUri(Uri configurableUri) {
			if (!isConfigurableUri(configurableUri)) {
				throw new RuntimeException("Uri is not a configurable Uri: " + configurableUri.toString());
			}
		}
		
		/**
		 * Returns the name of the probe in the Uri.
		 * @param probeUri
		 * @return
		 */
		public String getName(Uri probeUri) {
			verifyConfigurableUri(probeUri);
			return probeUri.getAuthority();
		}
		
		/**
		 * Returns the config in the probe Uri as a String.
		 * @param probeUri
		 * @return
		 */
		public String getConfigString(Uri probeUri) {
			verifyConfigurableUri(probeUri);
			String path = probeUri.getPath();
			return path == null || !path.startsWith("/") ? null : path.substring(1);
		}
		
		/**
		 * Returns the config in the probe Uri as a JsonObject.
		 * @param probeUri
		 * @return
		 */
		public JsonObject getConfig(Uri probeUri) {
			String configString = getConfigString(probeUri);
			if (configString == null || configString.length() == 0) {
				return null;
			} else {
				return (JsonObject)new JsonParser().parse(configString);
			}
		}
	}
	

	public static final String FUNF_SCHEME = "funf";
	public static final ConfigurableUri<Configurable> CONFIG_URI = new ConfigurableUri<Configurable>(FUNF_SCHEME);
	
	public static class ConfigurableBase implements Configurable {
		private JsonObject specifiedConfig;
		private JsonObject completeConfig;
		private Uri uri;
		private Uri completeUri;

		private List<Field> configurableFields;
		private List<Field> getConfigurableFields() {
			if (configurableFields == null) {
				List<Field> newConfigurableFields = new ArrayList<Field>();
				AnnotationUtil.getAllFieldsWithAnnotation(newConfigurableFields, getClass(), ConfigurableField.class);
				configurableFields = newConfigurableFields;
			}
			return configurableFields;
		}

		@Override
		public synchronized void setConfig(JsonObject config) {
			this.specifiedConfig = (config == null) ? null : new JsonObject();
			this.completeConfig = null;
			this.uri = null;
			this.completeUri = null;
			if (this.specifiedConfig != null) {
				for (Field field : getConfigurableFields()) {
					// Only allow config that was declared
					ConfigurableField configParamAnnotation = field.getAnnotation(ConfigurableField.class);
					Class<?> type = field.getType();
					String name = configParamAnnotation.name();
					if (name.length() == 0) {
						name = field.getName();
					}
					boolean currentAccessibility = field.isAccessible();
					field.setAccessible(true);
					try {
						if (config.has(name)) {
							JsonElement el = config.get(name);
							specifiedConfig.add(name, el);
							Object value = getGson().fromJson(el, type);
							field.set(this, value);
						}
					} catch (IllegalArgumentException e) {
						Log.e(LogUtil.TAG, "Bad access of probe fields!!", e);
					} catch (IllegalAccessException e) {
						Log.e(LogUtil.TAG, "Bad access of probe fields!!", e);
					}
					field.setAccessible(currentAccessibility);
				}
			}
		}
		
		public JsonObject getConfig() {
			if (specifiedConfig == null) {
				synchronized (this) {
					if (specifiedConfig == null) {
						this.specifiedConfig = new JsonObject();
					}
				}
			}
			return JsonUtils.deepCopy(specifiedConfig);
		}
		
		
		/**
		 * Returns a copy of the default configuration that is used by the probe if no 
		 * configuration is specified.  This is also used to enumerate the 
		 * configuration options that are available.
		 * 
		 * The object returned by this function is a copy and can be modified.
		 * @return
		 */
		public static JsonObject getDefaultConfig(Class<? extends Probe> probeClass, Context context) {
			Probe defaultProbe = ProbeFactory.BasicProbeFactory.getInstance(context).getProbe(probeClass, null);
			return defaultProbe.getCompleteConfig();
		}
		

		
		/**
		 * Returns a copy of the current configuration for this probe in json format.
		 * @return
		 */
		public JsonObject getCompleteConfig() {
			if (completeConfig == null) {
				synchronized (this) {
					if (completeConfig == null) {
						completeConfig = new JsonObject();
						for (Field field : getConfigurableFields()) {
							ConfigurableField configParamAnnotation = field.getAnnotation(ConfigurableField.class);
							Class<?> type = field.getType();
							String name = configParamAnnotation.name();
							if (name.length() == 0) {
								name = field.getName();
							}
							boolean currentAccessibility = field.isAccessible();
							field.setAccessible(true);
							try {
								JsonElement value = getGson().toJsonTree(field.get(this), type);
								completeConfig.add(name, value);
							} catch (IllegalArgumentException e) {
								Log.e(LogUtil.TAG, "Bad access of probe fields!!", e);
							} catch (IllegalAccessException e) {
								Log.e(LogUtil.TAG, "Bad access of probe fields!!", e);
							}
							field.setAccessible(currentAccessibility);
						}
					}
				}
			}
			return JsonUtils.deepCopy(completeConfig);
		}
		
		public Uri getUri() {
			if (uri == null) {
				uri = CONFIG_URI.getUri(this);
			}
			return uri;
		}
		
		public Uri getCompleteUri() {
			if (completeUri == null) {
				completeUri = CONFIG_URI.getCompleteUri(this);
			}
			return completeUri;
		}
		
		private Gson gson;
		protected Gson getGson() {
			if (gson == null) {
				gson = getGsonBuilder().create(); 
			}
			return gson;
		}
		

		protected GsonBuilder getGsonBuilder() {
			return new GsonBuilder();
		}
	}
	
}
