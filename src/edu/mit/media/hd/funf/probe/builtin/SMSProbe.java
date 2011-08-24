package edu.mit.media.hd.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import edu.mit.media.hd.funf.probe.CursorCell;
import edu.mit.media.hd.funf.probe.DatedContentProviderProbe;
import edu.mit.media.hd.funf.probe.builtin.ProbeKeys.SMSKeys;
import edu.mit.media.hd.funf.probe.builtin.ProbeKeys.AndroidInternal.Sms;

// TODO: May need to send text messages individually because bundle size may get too big for full scan
public class SMSProbe extends DatedContentProviderProbe implements SMSKeys {
	
	@Override
	protected Uri getContentProviderUri() {
		return Sms.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Sms.DATE;
	}

	@Override
	protected String getDataName() {
		return MESSAGES;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Sms.TYPE, intCell());
		projectionMap.put(Sms.THREAD_ID, intCell());
		projectionMap.put(Sms.ADDRESS, hashedStringCell()); // TODO: figure out if we have to normalize this first (maybe phone number)
		projectionMap.put(Sms.PERSON_ID, longCell());
		projectionMap.put(Sms.DATE, longCell());
		projectionMap.put(Sms.READ, booleanCell());
		//projectionMap.put(Sms.SEEN, booleanCell()); //Not Supported on all devices
		projectionMap.put(Sms.STATUS, intCell());
		projectionMap.put(Sms.SUBJECT, hashedStringCell());
		projectionMap.put(Sms.BODY, hashedStringCell());
		projectionMap.put(Sms.PERSON, hashedStringCell());
		projectionMap.put(Sms.PROTOCOL, intCell());
		projectionMap.put(Sms.REPLY_PATH_PRESENT, booleanCell());
		projectionMap.put(Sms.SERVICE_CENTER, stringCell());
		projectionMap.put(Sms.LOCKED, booleanCell());
		//projectionMap.put(Sms.ERROR_CODE, intCell());  //Not Supported on all devices
		//projectionMap.put(Sms.META_DATA, hashedStringCell());  Doesn't exist for some reason
		return projectionMap;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {android.Manifest.permission.READ_SMS};
	}

	
}
