package edu.mit.media.funf.util;

public class ArrayUtil {

	public ArrayUtil() {
		
	}
	

	
	/**
	 * Convenience function for concatenating two arrays
	 * @param <T>
	 * @param first
	 * @param second
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = (T[])new Object[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
}
