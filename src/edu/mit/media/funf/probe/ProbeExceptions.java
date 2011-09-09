/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.funf.probe;

/**
 * Exceptions related to Funf ProbeKeys.
 * @author alangardner
 *
 */
public class ProbeExceptions {

	/**
	 * Used when attempting to put non-primitive or parcelable object into Bundle or SharedPreferences
	 * @author alangardner
	 *
	 */
	public static class UnstorableTypeException extends RuntimeException {
		private static final long serialVersionUID = 8360917114397550065L;

		public UnstorableTypeException(final Class<?> objectClass) {
			super("Can't store object of type: " + objectClass.getName());
		}
	}
}
