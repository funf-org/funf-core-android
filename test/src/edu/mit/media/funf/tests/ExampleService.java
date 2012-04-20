package edu.mit.media.funf.tests;

import java.util.Map;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import edu.mit.media.funf.util.BundleUtil;

public class ExampleService extends IntentService {

	public ExampleService() {
		super("ExampleService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Map<String,Object> values = BundleUtil.getValues(intent.getExtras());
		String entryString = "{";
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			entryString += "" + entry.getKey() + ": " + String.valueOf(entry.getValue());
		}
		entryString += "}";
		Log.i("ExampleService", "Intent: " + intent.getAction() + " -> " + entryString);
	}

}
