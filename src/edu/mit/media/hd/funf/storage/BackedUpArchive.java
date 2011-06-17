package edu.mit.media.hd.funf.storage;

import java.io.File;

/**
 * Delegates all actions to archive.  Adds an item to the backup archive before removing from the archive.
 * It is up to the backup archive to determine when those items will remove themselves.
 *
 */
public class BackedUpArchive implements Archive<File> {

	private final Archive<File> archive, backupArchive;
	
	public BackedUpArchive(Archive<File> archive, Archive<File> backupArchive) {
		this.archive = archive;
		this.backupArchive = backupArchive;
	}
	
	@Override
	public boolean add(File item) {
		return archive.add(item);
	}

	@Override
	public File[] getAll() {
		return archive.getAll();
	}

	@Override
	public boolean contains(File item) {
		return archive.contains(item);
	}

	@Override
	public boolean remove(File item) {
		if (archive.contains(item)) {
			backupArchive.add(item);
		}
		return archive.remove(item);
	}


}
