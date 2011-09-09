package edu.mit.media.funf.storage;

import android.test.AndroidTestCase;

public class DefaultArchiveTest extends AndroidTestCase {

	
	public void testEncryptionKeyGeneration() {
		DefaultArchive testDbArchive = DefaultArchive.getArchive(getContext(), "testDb");
		testDbArchive.setEncryptionPassword("changeme".toCharArray());
		testDbArchive.setEncryptionPassword("test1234".toCharArray());
	}
}
