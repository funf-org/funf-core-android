package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.Browser;
import edu.mit.media.funf.probe.CursorCell;
import edu.mit.media.funf.probe.DatedContentProviderProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BrowserSearchesKeys;

public class BrowserSearchesProbe extends DatedContentProviderProbe {

	@Override
	protected String getDataName() {
		return BrowserSearchesKeys.SEARCHES;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Browser.SearchColumns._ID, intCell());
		projectionKeyToType.put(Browser.SearchColumns.SEARCH, hashedStringCell());
		projectionKeyToType.put(Browser.SearchColumns.DATE, longCell());
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
		return Browser.SEARCHES_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Browser.SearchColumns.DATE;
	}

	
}
