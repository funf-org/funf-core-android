package edu.mit.media.funf.util;

import java.util.Collection;
import java.util.Iterator;

public class StringUtil {
	
	private StringUtil() {
		
	}


	/**
	 * Convenience function for joining strings using a delimeter
	 * @param strings
	 * @param delimeter
	 * @return
	 */
	public static String join(final Collection<?> strings, String delimeter) {
		if (delimeter == null) {
			delimeter = ",";
		}
		if (strings.isEmpty()) {
			return "";
		}
		StringBuffer joined = new StringBuffer();
		Iterator<?> stringIter = strings.iterator();
		joined.append(stringIter.next().toString());
		while (stringIter.hasNext()) {
			joined.append(delimeter);
			joined.append(stringIter.next().toString());
		}
		return joined.toString();
	}
}
