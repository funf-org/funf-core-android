package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Attempts to use the first archive. If that fails, continues down the list until a successful archive is reached.
 *
 */
public class CompositeFileArchive implements Archive<File> {

	private final Archive<File>[] archives;
	
	public CompositeFileArchive(Archive<File>... archives) {
		this.archives = archives;
	}
	
	@Override
	public boolean add(File item) {
		// Add to archive one by one, until one is successful or archives are exhausted.
		for (Archive<File> archive : archives) {
			if(archive.add(item)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public File[] getAll() {
		// Merge files from all archives, ignoring duplicates
		Set<File> fileSet = new HashSet<File>();
		for (Archive<File> archive : archives) {
			fileSet.addAll(Arrays.asList(archive.getAll()));
		}
		File[] files = new File[fileSet.size()];
		fileSet.toArray(files);
		return files;
	}

	@Override
	public boolean remove(File item) {
		// Remove on every archive, returning true if at least one was successful
		boolean success = false;
		for (Archive<File> archive : archives) {
			if(archive.remove(item)) {
				success = true;
			}
		}
		return success;
	}

	@Override
	public boolean contains(File item) {
		for (Archive<File> archive : archives) {
			if (archive.contains(item)) {
				return true;
			}
		}
		return false;
	}

}
