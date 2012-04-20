package edu.mit.media.funf.util;

import static edu.mit.media.funf.util.AsyncSharedPrefs.async;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;

public class UuidUtil {

	public static String getInstallationId(Context context) {
		if (UuidUtil.uuid == null) {
			SharedPreferences prefs = async(context.getSharedPreferences(UuidUtil.FUNF_UTILS_PREFS, Context.MODE_PRIVATE));
			UuidUtil.uuid = prefs.getString(UuidUtil.INSTALLATION_UUID_KEY, null);
			if (UuidUtil.uuid == null) {
				UuidUtil.uuid = UUID.randomUUID().toString();
				prefs.edit().putString(UuidUtil.INSTALLATION_UUID_KEY, UuidUtil.uuid).commit();
			}
		}
		return UuidUtil.uuid;
	}

	public static String uuid = null;
	public static final String INSTALLATION_UUID_KEY = "INSTALLATION_UUID";
	public static final String FUNF_UTILS_PREFS = "edu.mit.media.funf.Utils";

}
