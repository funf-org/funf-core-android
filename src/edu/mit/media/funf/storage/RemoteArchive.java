package edu.mit.media.funf.storage;

import java.io.File;

public interface RemoteArchive {

	public boolean add(File file);
	public String getId();
}
