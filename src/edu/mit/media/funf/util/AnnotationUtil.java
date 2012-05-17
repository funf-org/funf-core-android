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
