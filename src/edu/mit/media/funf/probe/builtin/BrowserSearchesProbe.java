package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.Browser;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@DefaultSchedule(period=604800)
@RequiredPermissions(android.Manifest.permission.READ_HISTORY_BOOKMARKS)
public class BrowserSearchesProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Browser.SEARCHES_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Browser.SearchColumns.DATE;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Browser.SearchColumns._ID, intCell());
		projectionKeyToType.put(Browser.SearchColumns.SEARCH, sensitiveStringCell());
		projectionKeyToType.put(Browser.SearchColumns.DATE, longCell());
		return projectionKeyToType;
	}

}
