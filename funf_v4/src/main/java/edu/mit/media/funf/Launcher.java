package edu.mit.media.funf;

import static edu.mit.media.funf.util.LogUtil.TAG;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Launcher extends BroadcastReceiver {

	private static boolean launched = false;
	
	public static void launch(Context context) {
		Log.v(TAG, "Launched!");
		Intent i = new Intent(context.getApplicationContext(), FunfManager.class);
		context.getApplicationContext().startService(i);
		launched = true;
	}
	
	public static boolean isLaunched() {
		return launched;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		launch(context);
	}

}
