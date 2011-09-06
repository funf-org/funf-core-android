package edu.mit.media.hd.funf.storage;

public class HttpUploadService extends UploadService {

	public static final String UPLOAD_URL = REMOTE_ARCHIVE_ID;
	
	@Override
	protected RemoteArchive getRemoteArchive(String name) {
		return new HttpArchive(name);
	}

}
