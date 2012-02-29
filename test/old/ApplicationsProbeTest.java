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

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.ProbeTestCase;
import android.os.Bundle;

public class ApplicationsProbeTest extends ProbeTestCase<ApplicationsProbe> {

	public ApplicationsProbeTest() {
		super(ApplicationsProbe.class);
	}

	public void testProbe() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(2);
		assertNotNull(data.get("INSTALLED_APPLICATIONS"));
		assertNotNull(data.get("UNINSTALLED_APPLICATIONS"));
	}
	
	public void testRunOnceProbe() {
		Bundle params = new Bundle();
		params.putLong(Probe.Parameter.Builtin.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(2);
		assertNotNull(data.get("INSTALLED_APPLICATIONS"));
		assertNotNull(data.get("UNINSTALLED_APPLICATIONS"));
		shouldNotReturnData(5);
	}
}
