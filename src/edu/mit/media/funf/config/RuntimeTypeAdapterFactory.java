package edu.mit.media.funf.config;

import com.google.gson.JsonElement;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public interface RuntimeTypeAdapterFactory extends TypeAdapterFactory {
	
	public static final String TYPE = "@type";

	public <T> Class<? extends T> getRuntimeType(final JsonElement el, final TypeToken<T> type);
}
