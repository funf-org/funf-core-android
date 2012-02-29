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

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import edu.mit.media.funf.Utils;

public class ProbeUtils {

	/**
	 * Scans the manifest for registered services, returning the set that are instances of Probe
	 * @param context
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<Class<? extends Probe>> getAvailableProbeClasses(Context context) {
		Set<Class<? extends Probe>> probes = new HashSet<Class<? extends Probe>>();
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SERVICES);
			if (info.services != null) {
				for (ServiceInfo serviceInfo : info.services) {
					try {
						Class<?> probeServiceClass = Class.forName(serviceInfo.name);
						if (Probe.class.isAssignableFrom(probeServiceClass)) {
							probes.add((Class<? extends Probe>) probeServiceClass);
						}
					} catch (ClassNotFoundException e) {
						Log.e(Utils.TAG, e.getLocalizedMessage());
					}
				}
			}
		} catch (NameNotFoundException e) {
			Log.e(Utils.TAG, e.getLocalizedMessage());
		}
		return probes;
	}

	/**
	 * Get the class instance for the probe specified by the action, from the set of probe classes passed in.
	 * @param probeClasses
	 * @param action
	 * @return probe class that matches action, or null if it doesn't exist
	 */
	public static Class<? extends Probe> getProbeClass(final Set<Class<? extends Probe>> probeClasses, final String action) {
		// TODO: deal with registration of more than just class name for action
		// work with interfaces as well, e.g. the common OPP interfaces
		for (Class<? extends Probe> probeClass : probeClasses) {
			if (action != null && action.startsWith(probeClass.getName())) {
				return probeClass;
			}
		}
		return null;
	}

}
