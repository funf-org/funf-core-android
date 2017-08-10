/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
