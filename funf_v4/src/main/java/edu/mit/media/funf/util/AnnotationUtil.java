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
package edu.mit.media.funf.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;


public class AnnotationUtil {

	/**
	 * @param fields
	 * @param type
	 * @return
	 */
	public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
	    return AnnotationUtil.getAllFieldsWithAnnotation(fields, type, null);
	}
	
	/**
	 * Returns the field with the given name on the type lowest in the class hierarchy.
	 * 
	 * @param name The name of the field to get
	 * @param type The class on which to search for the field.
	 * @return The field with this name lowest in the hierarchy, otherwise null if the field does not exist in the class hierarchy.
	 */
	public static Field getField(String name, Class<?> type) {
		for (Field field: type.getDeclaredFields()) {
			if (field.getName().equals(name)) {
				return field;
			}
	    }
	
	    if (type.getSuperclass() != null) {
	        return getField(name, type.getSuperclass());
	    }
		
		return null;
	}
	
	public static List<Field> getAllFieldsOfType(List<Field> fields, Class<?> type, Class<?> fieldType) {
		for (Field field: type.getDeclaredFields()) {
			if (fieldType == null || fieldType.isAssignableFrom(field.getType())) {
				fields.add(field);
			}
	    }
	
	    if (type.getSuperclass() != null) {
	        fields = getAllFieldsOfType(fields, type.getSuperclass(), fieldType);
	    }
	
	    return fields;
	}

	public static List<Field> getAllFieldsWithAnnotation(List<Field> fields, Class<?> type, Class<? extends Annotation> annotationType) {
		for (Field field: type.getDeclaredFields()) {
			if (annotationType == null || field.getAnnotation(annotationType) != null) {
				fields.add(field);
			}
	    }
	
	    if (type.getSuperclass() != null) {
	        fields = getAllFieldsWithAnnotation(fields, type.getSuperclass(), annotationType);
	    }
	
	    return fields;
	}

}
