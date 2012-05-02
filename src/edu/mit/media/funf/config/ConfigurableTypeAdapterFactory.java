package edu.mit.media.funf.config;

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.ParseException;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.mit.media.funf.util.AnnotationUtil;
import edu.mit.media.funf.util.LogUtil;

public class ConfigurableTypeAdapterFactory<E> implements TypeAdapterFactory {
	public static final String TYPE = "@type";
	
	private final Context context;
	private final Class<E> baseClass;
	private final Class<? extends E> defaultClass;
	
	/**
	 * Use the base class as the default class.
	 * @param context
	 * @param baseClass
	 */
	public ConfigurableTypeAdapterFactory(Context context, Class<E> baseClass) {
		this(context, baseClass, baseClass);
	}
	
	/**
	 * @param context
	 * @param baseClass  
	 * @param defaultClass  Setting this to null will cause a ParseException if the runtime type information is incorrect or unavailable.
	 */
	public ConfigurableTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
		assert context != null && baseClass != null;
		if (defaultClass != null && !isInstantiable(defaultClass)) {
			throw new RuntimeException("Default class does not have a default contructor.");
		}
		this.context = context;
		this.baseClass = baseClass;
		this.defaultClass = defaultClass;
	}
	
	private static boolean isInstantiable(Class<?> type) {
		try {
			type.newInstance();
			return true;
		} catch (IllegalAccessException e) {
		} catch (InstantiationException e) {
		}
		return false;
	}
	


	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		final boolean canUseDefaultClass = defaultClass != null && defaultClass.isAssignableFrom(type.getRawType());
		if (baseClass.isAssignableFrom(type.getRawType())) {
			// TODO: create caching data structures
			return new TypeAdapter<T>() {
				@SuppressWarnings("unchecked")
				@Override
				public void write(JsonWriter out, T value) throws IOException {
					// TODO: need to handle null
					JsonObject jsonObject = new JsonObject();
					List<Field> configurableFields = new ArrayList<Field>();
					AnnotationUtil.getAllFieldsWithAnnotation(configurableFields, value.getClass(), Configurable.class);
					for (Field field : configurableFields) {
						String fieldJsonName = field.getAnnotation(Configurable.class).name();
						if ("".equals(fieldJsonName)) {
							fieldJsonName = field.getName();
						}
						boolean currentAccessibility = field.isAccessible();
						try {
							field.setAccessible(true);
							jsonObject.add(fieldJsonName, gson.toJsonTree(field.get(value)));
						} catch (IllegalArgumentException e) {
							Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
						} catch (IllegalAccessException e) {
							Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
						} finally {
							field.setAccessible(currentAccessibility);
						}
					}
					Streams.write(jsonObject, out);
				}

				@Override
				public T read(JsonReader in) throws IOException {
					// TODO: need to handle null
					JsonElement el = Streams.parse(in);
					Class<? extends T> runtimeType = getRuntimeType(el);
					if (runtimeType == null) {
						throw new ParseException("RuntimeTypeAdapter: Unable to parse runtime type.");
					}

					T object = null;
					try {
						object = runtimeType.newInstance();
					} catch (IllegalAccessException e) {
						throw new RuntimeException("RuntimeTypeAdapter: Runtime class '" + runtimeType.getName() + "' does not have a visible default contructor.");
					} catch (InstantiationException e) {
						throw new RuntimeException("RuntimeTypeAdapter: Runtime class '" + runtimeType.getName() + "' does not have a default contructor.");
					}
					
					// Inject Configuration
					if (el.isJsonObject()) {
						JsonObject jsonObject = el.getAsJsonObject();
						// Loop over configurable fields for customization
						List<Field> configurableFields = new ArrayList<Field>();
						AnnotationUtil.getAllFieldsWithAnnotation(configurableFields, runtimeType, Configurable.class);
						for (Field field : configurableFields) {
							// TODO: check that object doesn't have field of it's own type (to prevent infinite recursion)
							String fieldJsonName = field.getAnnotation(Configurable.class).name();
							if ("".equals(fieldJsonName)) {
								fieldJsonName = field.getName();
							}
							boolean currentAccessibility = field.isAccessible();
							try {
								field.setAccessible(true);
								if (jsonObject.has(fieldJsonName)) {
									field.set(object, gson.fromJson(jsonObject.get(fieldJsonName), field.getGenericType()));
								}
							} catch (IllegalArgumentException e) {
								Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
							} catch (IllegalAccessException e) {
								Log.e(LogUtil.TAG, "Bad access of configurable fields!!", e);
							} finally {
								field.setAccessible(currentAccessibility);
							}
						}
						return gson.fromJson(el, runtimeType);
					} 
					
					// Inject Context
					List<Field> contextFields = new ArrayList<Field>();
					AnnotationUtil.getAllFieldsOfType(contextFields, runtimeType, Context.class);
					for (Field field : contextFields) {
						boolean currentAccessibility = field.isAccessible();
						field.setAccessible(true);
						try {
							field.set(object, context);
						} catch (IllegalArgumentException e) {
							Log.e(LogUtil.TAG, "Bad access of Context fields!!", e);
						} catch (IllegalAccessException e) {
							Log.e(LogUtil.TAG, "Bad access of Context fields!!", e);
						}
						field.setAccessible(currentAccessibility);
					}
					
					return object;
				}
				

				@SuppressWarnings("unchecked")
				private Class<? extends T> getRuntimeType(JsonElement el) {
					return ConfigurableTypeAdapterFactory.getRuntimeType(el, (Class<T>)type.getRawType(), canUseDefaultClass ? (Class<? extends T>)defaultClass : null);
				}
			};
		}
		return null;
	}
	

	public static <T> Class<? extends T> getRuntimeType(JsonElement el, Class<T> baseClass, Class<? extends T> defaultClass) {
		Class<? extends T> type = defaultClass;
		String typeString = null;
		try {
			if (el.isJsonObject()) {
				typeString = el.getAsJsonObject().remove(TYPE).getAsString();
			} else if (el.isJsonPrimitive()){
				typeString = el.getAsString();
			}
		} catch (ClassCastException e) {
		}
		// TODO: expand string to allow for builtin to be specified as ".SampleProbe"
		if (typeString != null) {
			try {
				Class<?> runtimeClass = Class.forName(typeString);
				if (baseClass.isAssignableFrom(runtimeClass)) {
					type = (Class<? extends T>)runtimeClass;
				} else {
					Log.w(TAG, "RuntimeTypeAdapter: Runtime class '" + typeString + "' is not assignable from default class '" + defaultClass.getName() + "'.");
				}
			} catch (ClassNotFoundException e) {
				Log.w(TAG, "RuntimeTypeAdapter: Runtime class '" + typeString + "' not found.");
			}
		}
		return type;
	}
}
