/**
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
