package edu.mit.media.hd.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.MediaStore.Images;
import edu.mit.media.hd.funf.probe.CursorCell;
import edu.mit.media.hd.funf.probe.DatedContentProviderProbe;

public class ImagesProbe extends DatedContentProviderProbe {

	public static final String IMAGES = "IMAGES";
	
	@Override
	protected Uri getContentProviderUri() {
		return Images.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return  Images.Media.DATE_MODIFIED;
	}

	@Override
	protected String getDataName() {
		return IMAGES;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Images.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Images.Media.DATE_ADDED, longCell());
		projectionMap.put(Images.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Images.Media.DISPLAY_NAME, hashedStringCell());
		projectionMap.put(Images.Media.MIME_TYPE, stringCell());
		projectionMap.put(Images.Media.SIZE, longCell());
		projectionMap.put(Images.Media.TITLE, hashedStringCell());
		projectionMap.put(Images.Media.BUCKET_DISPLAY_NAME, hashedStringCell());
		projectionMap.put(Images.Media.BUCKET_ID, stringCell());
		projectionMap.put(Images.Media.DATE_TAKEN, longCell());
		projectionMap.put(Images.Media.DESCRIPTION, hashedStringCell());
		projectionMap.put(Images.Media.IS_PRIVATE, intCell());
		projectionMap.put(Images.Media.LATITUDE, doubleCell());
		projectionMap.put(Images.Media.LONGITUDE, doubleCell());
		projectionMap.put(Images.Media.MINI_THUMB_MAGIC, intCell());
		projectionMap.put(Images.Media.ORIENTATION, intCell());
		projectionMap.put(Images.Media.PICASA_ID, stringCell());
		return projectionMap;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

}
