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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeTestCase;
import edu.mit.media.funf.probe.Probe.Parameter;

public class AccelerometerProbeTest extends ProbeTestCase<AccelerometerSensorProbe> {

	public AccelerometerProbeTest() {
		super(AccelerometerSensorProbe.class);
	}
	
	public void testEmptyBroadcast() throws CanceledException {
		Intent empty = new Intent();
		empty.putExtra("TEST", "Test");
		getContext().sendBroadcast(empty);
		PendingIntent emptyPendingIntent = PendingIntent.getBroadcast(getContext(), 0, empty, PendingIntent.FLAG_CANCEL_CURRENT);
		emptyPendingIntent.send();
		
		emptyPendingIntent.send(getContext(), 0, new Intent(Probe.ACTION_DATA));

	}
	
	
	public void testAccelerometerData() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(Parameter.Builtin.DURATION.name, 3L);
		params.putLong(Parameter.Builtin.PERIOD.name, 10L);
		startProbe(params);
		for (int i=0; i<3; i++) {
			Bundle data = getData(10);
			assertTrue(data.containsKey("SENSOR"));
			assertTrue(data.containsKey("EVENT_TIMESTAMP"));
			assertTrue(data.containsKey("ACCURACY"));
			assertTrue(data.containsKey("X"));
			assertTrue(data.containsKey("Y"));
			assertTrue(data.containsKey("Z"));
			assertTrue(data.containsKey("TIMESTAMP"));
			long[] eventTimestamp = data.getLongArray("EVENT_TIMESTAMP");
			int[] accuracy = data.getIntArray("ACCURACY");
			float[] x = data.getFloatArray("X");
			float[] y = data.getFloatArray("Y");
			float[] z = data.getFloatArray("Z");
			int numEvents = eventTimestamp.length;
			assertEquals(numEvents, accuracy.length);
			assertEquals(numEvents, x.length);
			assertEquals(numEvents, y.length);
			assertEquals(numEvents, z.length);
//			System.out.println("@" + data.getLong("TIMESTAMP") + " - " +
//					"X:" + data.getFloat("X") +
//					"Y:" + data.getFloat("Y") +
//					"Z:" + data.getFloat("Z")
//			);
		}
		stopProbe();
	}
	

}
