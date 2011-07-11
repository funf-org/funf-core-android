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
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class LocationProbeTest extends ProbeTestCase<LocationProbe> {

	private static final int FUDGE_FACTOR = 5;
	
	
	public LocationProbeTest() {
		super(LocationProbe.class);
	}

	public void testData() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 30L);
		startProbe(params);
		Bundle data = getData(30 + FUDGE_FACTOR);
		Location location = (Location)data.get("LOCATION");
		assertNotNull(location);
		System.out.println("Accuracy: " + String.valueOf(location.getAccuracy()));
	}
	
	public void testReturningCachedLocation() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 1L);
		startProbe(params);
		Bundle data = getData(1 + FUDGE_FACTOR);
		Location location = (Location)data.get("LOCATION");
		assertNotNull(location);
		System.out.println("Accuracy: " + String.valueOf(location.getAccuracy()));
	}
	
	public void testUsingBroadcasts() throws InterruptedException {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.DURATION.name, 30L);
		//params.putLong(Probe.SystemParameter.PERIOD.name, 10L);
		// TODO: come back to configuration parameters
		//params.putLong(LocationProbe.PARAM_MAX_WAIT_TIME, 10);
		//params.putLong(LocationProbe.PARAM_DESIRED_ACCURACY, 100);
		sendDataRequestBroadcast(params);
		
		Bundle data = getData(30 + FUDGE_FACTOR);
		Location location = (Location)data.get("LOCATION");
		assertNotNull(location);
		System.out.println("Accuracy: " + String.valueOf(location.getAccuracy()));
	}
	
}
