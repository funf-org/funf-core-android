package edu.mit.media.funf.storage;

import android.os.Bundle;

public interface BundleSerializer {
	/**
	 * Return a string version of the bundle.  Serializing should be as lossless as possible in terms of data,
	 * but it isn't necessary that the objects can be recreated from the string.
	 * @param bundle
	 * @return
	 */
	public String serialize(Bundle bundle);
}
