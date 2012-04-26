package edu.mit.media.funf.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Used to indicate that a particular field is configurable.
 * The name of the field will be used as the configuration name, and the 
 * default value will be calculated by creating an instance with no config
 * and inspecting what value gets created.
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
public @interface Configurable {
	/**
	 * @return Overrides the field name, if specified.
	 */
	String name() default "";
}
