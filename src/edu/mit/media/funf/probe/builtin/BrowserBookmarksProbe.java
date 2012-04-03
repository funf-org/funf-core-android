package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.Browser;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;

@DefaultSchedule(period=604800)
@RequiredPermissions(android.Manifest.permission.READ_HISTORY_BOOKMARKS)
public class BrowserBookmarksProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Browser.BOOKMARKS_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Browser.BookmarkColumns.DATE;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Browser.BookmarkColumns._ID, intCell());
		projectionKeyToType.put(Browser.BookmarkColumns.TITLE, sensitiveStringCell());
		projectionKeyToType.put(Browser.BookmarkColumns.URL, sensitiveStringCell());
		projectionKeyToType.put(Browser.BookmarkColumns.VISITS, intCell());
		projectionKeyToType.put(Browser.BookmarkColumns.DATE, longCell());
		projectionKeyToType.put(Browser.BookmarkColumns.CREATED, longCell());
		projectionKeyToType.put(Browser.BookmarkColumns.BOOKMARK, intCell());
		//projectionKeyToType.put(Browser.BookmarkColumns.DESCRIPTION, hashedStringCell());  // TODO: Description doesn't exist
		return projectionKeyToType;
	}

}
