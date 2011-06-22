package edu.mit.media.hd.funf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

public class AndroidUtils {

	public static void configureAlarm(Context context, Class<? extends Service> serviceClass, long period) {
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent archiveIntent = new Intent(context, serviceClass);
		PendingIntent pi = PendingIntent.getService(context, 0, archiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + period, period, pi);
	}

}
