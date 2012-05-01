package edu.mit.media.funf.config;

import static edu.mit.media.funf.json.JsonUtils.immutable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Same configuration should return the same object.  
 * A cache of each object created is kept to ensure this.
 * 
 * TODO: should probably have weak refs, so that we can garbage collect unused items.
 * 
 * @author alangardner
 *
 */
public class SingletonTypeAdapterFactory implements TypeAdapterFactory {

	private TypeAdapterFactory delegate;
	private Map<String,Object> cache;
	
	public SingletonTypeAdapterFactory(TypeAdapterFactory delegate) {
		this.delegate = delegate;
		this.cache = new HashMap<String,Object>();
	}
	
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		return new SingletonTypeAdapter<T>(delegate.create(gson, type), type);
	}
	
	public void clearCache() {
		synchronized (cache) {
			cache.clear();
		}
	}

	public class SingletonTypeAdapter<E> extends TypeAdapter<E> {
		
		private TypeAdapter<E> typeAdapter;
		private TypeToken<E> type;
		
		public SingletonTypeAdapter(TypeAdapter<E> typeAdapter, TypeToken<E> type) {
			this.typeAdapter = typeAdapter;
			this.type = type;
		}
		
		@Override
		public void write(JsonWriter out, E value) throws IOException {
			typeAdapter.write(out, value);
		}

		@Override
		public E read(JsonReader in) throws IOException {
			JsonElement el = Streams.parse(in);
			Class<? extends E> runtimeType = ConfigurableTypeAdapterFactory.getRuntimeType(el, (Class<E>)type.getRawType(), (Class<E>)type.getRawType());
			String configString = runtimeType.toString() + immutable(el).toString();
			// TODO: surround this in a try catch class cast exception
			E object = (E)cache.get(configString);
			if (object == null) {
				object = typeAdapter.read(new JsonTreeReader(el));
				cache.put(configString, object);
			}
			return object;
		}
		
		public void clearCache() {
			cache.clear();
		}
	}
}
