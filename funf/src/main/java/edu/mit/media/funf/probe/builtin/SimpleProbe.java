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
package edu.mit.media.funf.probe.builtin;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import edu.mit.media.funf.probe.Probe.Base;

public abstract class SimpleProbe<T> extends Base {
	
	// TODO: possibly integrate this into base, so that both impulse and continuous probes can make use of it
	
	@Override
	protected GsonBuilder getGsonBuilder() {
		GsonBuilder builder = super.getGsonBuilder();
		JsonSerializer<T> serializer = getSerializer();
		if (serializer != null) {
			builder.registerTypeAdapter(getClass().getGenericInterfaces()[0], serializer);
		}
		return builder;
	}
	
	protected void sendData(T data) {
		sendData(getGson().toJsonTree(data).getAsJsonObject());
	}

	/**
	 * Used to override the default serialization technique for the object
	 * @return
	 */
	protected JsonSerializer<T> getSerializer() {
		return null;
	}

	
}
