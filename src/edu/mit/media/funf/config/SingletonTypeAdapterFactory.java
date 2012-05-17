/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.config;

import static edu.mit.media.funf.json.JsonUtils.immutable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	private RuntimeTypeAdapterFactory delegate;
	private Map<String,Object> cache;
	
	public SingletonTypeAdapterFactory(RuntimeTypeAdapterFactory delegate) {
		this.delegate = delegate;
		this.cache = new HashMap<String,Object>();
	}
	
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		TypeAdapter<T> adapter = delegate.create(gson, type);
		return adapter == null ? null : new SingletonTypeAdapter<T>(adapter, type);
	}
	
	public Collection<Object> getCached() {
		return this.cache.values();
	}
	
	public void clearCache(Object o) {
		synchronized (cache) {
			Set<String> toRemove = new HashSet<String>();
			for (Map.Entry<String, Object> entry : cache.entrySet()) {
				if (o == entry.getValue()) {
					toRemove.add(entry.getKey());
				}
			}
			for (String key : toRemove) {
				cache.remove(key);
			}
		}
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
			Class<? extends E> runtimeType = delegate.getRuntimeType(el, type);
			String configString = runtimeType.toString() + immutable(el).toString();
			// TODO: surround this in a try catch class cast exception
			@SuppressWarnings("unchecked")
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
