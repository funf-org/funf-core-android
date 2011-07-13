package edu.mit.media.hd.funf.probe.builtin;

import android.os.Build;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.SynchronousProbe;

public class AndroidInfoProbe extends SynchronousProbe {

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

	@Override
	public void sendProbeData() {
		Bundle data = new Bundle();
		data.putString("FIRMWARE_VERSION", Build.VERSION.RELEASE);
		data.putString("BUILD_NUMBER",  
				Build.PRODUCT + "-" + Build.TYPE
				+ " " + Build.VERSION.RELEASE
				+ " " + Build.ID
				+ " " + Build.VERSION.INCREMENTAL
				+ " " + Build.TAGS);
		data.putInt("SDK", Integer.parseInt(Build.VERSION.SDK, 10));
		sendProbeData(System.currentTimeMillis(), new Bundle(), data);
	}

}
