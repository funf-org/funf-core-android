package edu.mit.media.funf.probe.builtin;

import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

import com.google.gson.Gson;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ServicesKeys;
import edu.mit.media.funf.util.Configurable;

public class ServicesProbe extends Base implements ServicesKeys {

	/**
	 * The array of packages from which service info will be emitted. 
	 * If this parameter is null, will return every service.
	 */
	@Configurable
	private String[] packages = null;  
	
	@Override
	protected void onStart() {
		super.onStart();
		Gson gson = getGson();
		ActivityManager am = (ActivityManager)getContext().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<String> packageList = packages == null ? null : Arrays.asList(packages);
		for (RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
			String packageName = info.service.getPackageName();
			if (packageList == null || packageList.contains(packageName)) {
				sendData(gson.toJsonTree(info).getAsJsonObject());
			}
		}
		stop();
	}
	
}
