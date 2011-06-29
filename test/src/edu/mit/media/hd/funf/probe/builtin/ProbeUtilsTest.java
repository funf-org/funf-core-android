/**
 *
 * This file is part of the FunF Software System
 * Copyright © 2011, Massachusetts Institute of Technology
 * Do not distribute or use without explicit permission.
 * Contact: funf.mit.edu
 *
 *
 */
package edu.mit.media.hd.funf.probe.builtin;

import junit.framework.TestCase;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

public class ProbeUtilsTest extends TestCase {

	
	public void testIntentUrls() {
		Intent i = new Intent("test.action");
		i.putExtra("TEST_PARAM1", true);
		i.putExtra("TEST_PARAM2", "(&U#ALAN");
		i.putExtra("TEST_PARAM6", new boolean[] {true, false, true});
		i.putExtra("TEST_PARAM7", new float[] {1, 0, 32.34f});
		i.putExtra("TEST_PARAM8", 2);
		i.putExtra("TEST_PARAM3", new Location("ASDF"));
		Bundle b = new Bundle();
		b.putFloat("TEST_PARAM4", 23423.34f);
		i.putExtra("TEST_PARAM5", b);
		System.out.println(i.toUri(Intent.URI_INTENT_SCHEME));
		
		// No arrays or parcelables show up
	}
}
