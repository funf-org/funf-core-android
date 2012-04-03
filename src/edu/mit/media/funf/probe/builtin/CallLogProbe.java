package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ContentProviderProbe.CursorCell.PhoneNumberCell;

@DefaultSchedule(period=36000)
@RequiredPermissions(android.Manifest.permission.READ_CONTACTS)
public class CallLogProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return CallLog.Calls.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Calls.DATE;
	}

	@Override
	protected Map<String,CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Calls._ID, intCell());
		projectionKeyToType.put(Calls.NUMBER, new SensitiveCell(new PhoneNumberCell()));
		projectionKeyToType.put(Calls.DATE, longCell());
		projectionKeyToType.put(Calls.TYPE, intCell());
		projectionKeyToType.put(Calls.DURATION, longCell());
		projectionKeyToType.put(Calls.CACHED_NAME, sensitiveStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_LABEL, sensitiveStringCell());
		projectionKeyToType.put(Calls.CACHED_NUMBER_TYPE, sensitiveStringCell());
		return projectionKeyToType;
	}

}
