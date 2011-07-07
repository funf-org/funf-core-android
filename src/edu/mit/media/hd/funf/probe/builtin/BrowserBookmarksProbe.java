package edu.mit.media.hd.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.Browser;
import edu.mit.media.hd.funf.probe.CursorCell;
import edu.mit.media.hd.funf.probe.DatedContentProviderProbe;

public class BrowserBookmarksProbe extends DatedContentProviderProbe {

	public static final String BOOKMARKS = "BOOKMARKS";

	@Override
	protected String getDataName() {
		return BOOKMARKS;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Browser.BookmarkColumns._ID, intCell());
		projectionKeyToType.put(Browser.BookmarkColumns.TITLE, hashedStringCell());
		projectionKeyToType.put(Browser.BookmarkColumns.URL, hashedStringCell());
		projectionKeyToType.put(Browser.BookmarkColumns.VISITS, intCell());
		projectionKeyToType.put(Browser.BookmarkColumns.DATE, longCell());
		projectionKeyToType.put(Browser.BookmarkColumns.CREATED, longCell());
		projectionKeyToType.put(Browser.BookmarkColumns.BOOKMARK, intCell());
		//projectionKeyToType.put(Browser.BookmarkColumns.DESCRIPTION, hashedStringCell());  // TODO: Description doesn't exist
		return projectionKeyToType;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.READ_HISTORY_BOOKMARKS
		};
	}

	@Override
	protected Uri getContentProviderUri() {
		return Browser.BOOKMARKS_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Browser.BookmarkColumns.DATE;
	}
	
	

}
