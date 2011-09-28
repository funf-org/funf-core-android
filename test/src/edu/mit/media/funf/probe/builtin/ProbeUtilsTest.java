/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe.builtin;

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
