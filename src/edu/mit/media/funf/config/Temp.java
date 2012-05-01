package edu.mit.media.funf.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Temp {

	public static final String 
		TYPE = "@type",
		SCHEDULE = "@schedule";
	
	private Gson gson;
	
	public <T> T get(JsonElement el, Class<T> baseClass) throws InvalidTypeException, UnconfigurableObjectException {
		return get(el, baseClass, null);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(JsonElement el, Class<T> baseClass, Class<? extends T> defaultClass) throws InvalidTypeException, UnconfigurableObjectException {

		Class<? extends T> type = defaultClass;
		if (defaultClass == null) {
			type = baseClass;
		}
		
		// Create type from string or string in @type 
		String typeString = null;
		if (el.isJsonPrimitive()) {
			try {
				typeString = el.getAsString();
			} catch (ClassCastException e) {
				// TODO Auto-generated catch block
			}
		} else if (el.isJsonObject()) {
			typeString = el.getAsJsonObject().get(TYPE).getAsString();
		}
		
		
		if (typeString != null) {
			try {
				type = (Class<? extends T>)Class.forName(el.getAsString());
				if (!baseClass.isAssignableFrom(type)) {
					// TODO: should we just use the default class?
					throw new InvalidTypeException();
				}
			} catch (ClassNotFoundException e) {
				throw new InvalidTypeException();
			}
		}
		
		if (el.isJsonObject()) {
			return gson.fromJson(el, type);
		} else {
			try {
				return type.newInstance();
			} catch (IllegalAccessException e) {
				throw new UnconfigurableObjectException("Configurable object must have a public default constructor.");
			} catch (InstantiationException e) {
				throw new UnconfigurableObjectException("Configurable object must have a public default constructor.");
			}
		}
	}
	
	public class InvalidTypeException extends Exception {

		private static final long serialVersionUID = -6278837859329611119L;

		public InvalidTypeException() {
			super();
		}

		public InvalidTypeException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		public InvalidTypeException(String detailMessage) {
			super(detailMessage);
		}

		public InvalidTypeException(Throwable throwable) {
			super(throwable);
		}
		
	}
	
	public class UnconfigurableObjectException extends Exception {

		private static final long serialVersionUID = 1881015793087599681L;

		public UnconfigurableObjectException() {
			super();
		}

		public UnconfigurableObjectException(String detailMessage,
				Throwable throwable) {
			super(detailMessage, throwable);
		}

		public UnconfigurableObjectException(String detailMessage) {
			super(detailMessage);
		}

		public UnconfigurableObjectException(Throwable throwable) {
			super(throwable);
		}
	}
}
