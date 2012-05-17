package edu.mit.media.funf.config;

import java.io.IOException;
import java.lang.reflect.Field;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.mit.media.funf.util.AnnotationUtil;

import static edu.mit.media.funf.util.LogUtil.TAG;

public class ContextInjectorTypeAdapaterFactory implements TypeAdapterFactory {

	public static final String CONTEXT_FIELD = "context";
	
	private Context context;
	private TypeAdapterFactory delegate;
	
	public ContextInjectorTypeAdapaterFactory(Context context, TypeAdapterFactory delegate) {
		if (context == null) {
			throw new RuntimeException("Context cannot be null.");
		}
		this.context = context;
		this.delegate = delegate;
	}
	
	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		final TypeAdapter<T> delegateAdapter = delegate.create(gson, type);
		if (delegateAdapter == null) {
			return null;
		} else {
			return new TypeAdapter<T>() {
				
				@Override
				public void write(JsonWriter out, T value) throws IOException {
					delegateAdapter.write(out, value);
				}
	
				@Override
				public T read(JsonReader in) throws IOException {
					T value = delegateAdapter.read(in);
					if (value != null) {
						try {
							Field contextField = AnnotationUtil.getField(CONTEXT_FIELD, value.getClass());
							if (contextField != null && Context.class.isAssignableFrom(contextField.getType())) {
								boolean isAccessible = contextField.isAccessible();
								contextField.setAccessible(true);
								contextField.set(value, context);
								contextField.setAccessible(isAccessible);
							}
						} catch (SecurityException e) {
							// Swallow
							Log.v(TAG, e.getMessage());
						} catch (IllegalArgumentException e) {
							// Swallow
							Log.v(TAG, e.getMessage());
						} catch (IllegalAccessException e) {
							// Swallow
							Log.v(TAG, e.getMessage());
						}
					}
					return value;
				}
			};
		}
	}

}
