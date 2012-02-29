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
package edu.mit.media.funf.probe;

import android.os.Bundle;
import edu.mit.media.funf.Utils;

public abstract class SynchronousProbe extends Probe {

	private long mostRecentTimestamp;
	private Bundle mostRecentData;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
			new Parameter(Parameter.Builtin.PERIOD, getDefaultPeriod()),
			new Parameter(Parameter.Builtin.START, 0L),
			new Parameter(Parameter.Builtin.END, 0L)
		};
	}
	
	protected long getDefaultPeriod() {
		return 3600L;
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	protected void onDisable() {
		// Nothing
	}

	@Override
	protected void onEnable() {
		// Nothing
	}

	@Override
	protected void onRun(Bundle params) {
		mostRecentData = null; // Prevent sending old data with new timestamp
		mostRecentTimestamp = getTimestamp();
		mostRecentData = getData();
		sendProbeData();
		disable();
	}

	@Override
	protected void onStop() {
		// Nothing
	}

	@Override
	public void sendProbeData() {
		if (mostRecentData != null) {
			sendProbeData(mostRecentTimestamp, mostRecentData);
		}
	}

	/**
	 * Build and return the data that will get sent out as a data broadcast.
	 * @return
	 */
	protected abstract Bundle getData();
	
	/**
	 * Return the timestamp that should be sent with data
	 * @return
	 */
	protected long getTimestamp() {
		return Utils.getTimestamp();
	}
	
}
