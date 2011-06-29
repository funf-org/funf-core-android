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

import android.location.Location;
import android.os.Bundle;
import edu.mit.media.hd.funf.OppProbe;
import edu.mit.media.hd.funf.probe.Probe;
import edu.mit.media.hd.funf.probe.builtin.LocationProbe;

public class LocationProbeTest extends ProbeTestCase<LocationProbe> {

	private static final int FUDGE_FACTOR = 3;
	
	
	public LocationProbeTest() {
		super(LocationProbe.class);
	}

	public void testUsingBroadcasts() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(Probe.SystemParameter.START.name, System.currentTimeMillis()/1000);
		params.putLong(Probe.SystemParameter.PERIOD.name, 10L);
		//params.putLong(LocationProbe.PARAM_MAX_WAIT_TIME, 10);
		//params.putLong(LocationProbe.PARAM_DESIRED_ACCURACY, 100);
		sendDataRequestBroadcast(params);
		
		Bundle data = getData(60 + FUDGE_FACTOR);
		Location location = (Location)data.get("LOCATION");
		System.out.println("Location: " + location.getLatitude() + "," + location.getLongitude() + " within " + location.getAccuracy() + "m");
		assertTrue(location.getAccuracy() <= 100.0f);
	}
	
}
