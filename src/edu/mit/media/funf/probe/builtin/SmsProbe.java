package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInternal.Sms;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SmsKeys;

/**
 * @author alangardner
 *
 */
@DefaultSchedule(period=36000)
@DisplayName("SMS Log Probe")
@RequiredPermissions(android.Manifest.permission.READ_SMS)
public class SmsProbe extends DatedContentProviderProbe implements SmsKeys {

	@Override
	protected Uri getContentProviderUri() {
		return Sms.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Sms.DATE;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Sms.TYPE, intCell());
		projectionMap.put(Sms.THREAD_ID, intCell());
		projectionMap.put(Sms.ADDRESS, sensitiveStringCell()); // TODO: figure out if we have to normalize this first (maybe phone number)
		projectionMap.put(Sms.PERSON_ID, longCell());
		projectionMap.put(Sms.DATE, longCell());
		projectionMap.put(Sms.READ, booleanCell());
		//projectionMap.put(Sms.SEEN, booleanCell()); //Not Supported on all devices
		projectionMap.put(Sms.STATUS, intCell());
		projectionMap.put(Sms.SUBJECT, sensitiveStringCell());
		projectionMap.put(Sms.BODY, sensitiveStringCell());
		projectionMap.put(Sms.PERSON, sensitiveStringCell());
		projectionMap.put(Sms.PROTOCOL, intCell());
		projectionMap.put(Sms.REPLY_PATH_PRESENT, booleanCell());
		projectionMap.put(Sms.SERVICE_CENTER, stringCell());
		projectionMap.put(Sms.LOCKED, booleanCell());
		//projectionMap.put(Sms.ERROR_CODE, intCell());  //Not Supported on all devices
		//projectionMap.put(Sms.META_DATA, hashedStringCell());  Doesn't exist for some reason
		return projectionMap;
	}

}
