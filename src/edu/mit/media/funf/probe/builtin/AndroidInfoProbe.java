package edu.mit.media.funf.probe.builtin;

import android.os.Build;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInfoKeys;

public class AndroidInfoProbe extends Base implements AndroidInfoKeys {

	@Override
	protected void onStart() {
		super.onStart();
		JsonObject data = new JsonObject();
		data.addProperty(FIRMWARE_VERSION, Build.VERSION.RELEASE);
		data.addProperty(BUILD_NUMBER,  
				Build.PRODUCT + "-" + Build.TYPE
				+ " " + Build.VERSION.RELEASE
				+ " " + Build.ID
				+ " " + Build.VERSION.INCREMENTAL
				+ " " + Build.TAGS);
		data.addProperty(SDK, Integer.parseInt(Build.VERSION.SDK, 10));
		sendData(data);
	}
	
}
