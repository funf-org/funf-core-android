package edu.mit.media.funf.util;

import android.content.Context;
import android.os.PowerManager;

public class LockUtil {

	public static PowerManager.WakeLock getWakeLock(Context context) {
		return LockUtil.getWakeLock(context, context.getClass().getName());
	}

	public static PowerManager.WakeLock getWakeLock(Context context, String tag) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
		lock.acquire();
		return lock;
	}

}
