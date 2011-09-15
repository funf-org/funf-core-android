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
Case<LocationProbe> {

	private static final int FUDGE_FACTOR = 10;
	
	
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
