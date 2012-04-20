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
