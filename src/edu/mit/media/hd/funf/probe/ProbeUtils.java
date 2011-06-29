package edu.mit.media.hd.funf.probe;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import edu.mit.media.hd.funf.Utils;

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
