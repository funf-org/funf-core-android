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

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import edu.mit.media.funf.probe.ProbeTestCase;
import edu.mit.media.funf.probe.Probe.Parameter;

public class ImagesProbeTest extends ProbeTestCase<ImagesProbe> {

	public ImagesProbeTest() {
		super(ImagesProbe.class);
	}
	
	public void testData() throws InterruptedException {
		Log.i("edu.mit.media.funf.probe.builtin.ImagesProbe", "___________________START_________________");
		Bundle params = new Bundle();
		params.putLong(Parameter.Builtin.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(20);
		ArrayList<Parcelable> images = data.getParcelableArrayList(ImagesProbe.IMAGES);
		assertNotNull(images);
		assertTrue(images.size() > 0);
		
		Thread.sleep(5000L);
		clearData();
		
		// Running again should return an empty result
		startProbe(params);
		data = getData(10);
		images = data.getParcelableArrayList(ImagesProbe.IMAGES);
		assertNotNull(images);
		Log.i(TAG, "Number of images: " + images.size());
		assertTrue(images.isEmpty());
	}

}
