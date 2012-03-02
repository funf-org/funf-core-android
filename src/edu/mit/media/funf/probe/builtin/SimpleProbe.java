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
