package edu.mit.media.funf.probe2;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import android.test.AndroidTestCase;
import android.text.Annotation;

public class AnnotationsTest extends AndroidTestCase {

	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface A {
		String value() default "a";
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface B {
		String value() default "b";
	}
	
	@A
	public class Parent {
		
	}
	
	@B
	public class Child1 extends Parent {
		
	}
	
	@A("test")
	@B
	public class Child2 extends Parent {
		
	}
	
	@A("interface")
	public interface TestInterface  {
		
	}
	
	public class Child3 extends Parent implements TestInterface {
		
	}
	
	public class Child4 extends Child2 implements TestInterface {
		
	}
	
	public class Child5 implements TestInterface {
		
	}
	
	public void testAnnotationInheritance() {
		System.out.println(Parent.class.getAnnotations().length);
		System.out.println(Child1.class.getAnnotations().length);
		System.out.println(Child2.class.getAnnotations().length);
		System.out.println(Child2.class.getAnnotation(A.class).value());
		System.out.println(Child3.class.getAnnotation(A.class).value());
		System.out.println(Child4.class.getAnnotation(A.class).value());
		System.out.println(Child5.class.getAnnotation(A.class).value());
	}
	
	public void testReflectionAccessToDefaults() throws IllegalAccessException, InstantiationException {

		Annotation test = (Annotation) Probe.DefaultSchedule.class.newInstance();
	}
}
