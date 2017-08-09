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
