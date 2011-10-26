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

import android.os.Bundle;
import edu.mit.media.funf.probe.ProbeTestCase;
import edu.mit.media.funf.probe.Probe.Parameter;

public class LightSensorProbeTest extends ProbeTestCase<LightSensorProbe> {

	public LightSensorProbeTest() {
		super(LightSensorProbe.class);
	}
	
	
	public void testData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(Parameter.Builtin.DURATION.name, 10L);
		params.putLong(Parameter.Builtin.PERIOD.name, 10L);
		startProbe(params);
		
		Bundle data = getData(10);
		assertTrue(data.containsKey("SENSOR"));
		assertTrue(data.containsKey("EVENT_TIMESTAMP"));
		assertTrue(data.containsKey("ACCURACY"));
		assertTrue(data.containsKey("LUX"));
		assertTrue(data.containsKey("TIMESTAMP"));
		long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
		int[] accuracy = data.getIntArray("ACCURACY");
		float[] lux = data.getFloatArray("LUX");
		int numEvents = eventTimestamp.length;
		assertEquals(numEvents, accuracy.length);
		assertEquals(numEvents, lux.length);
		System.out.println("@" + data.getLong("TIMESTAMP") + " - " +
				"LUX Count:" + lux.length
		);
	}
	

}
