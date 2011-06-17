package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A simple directory based archive for files.  
 * A file may be able to be archived more than once depending on the nameGenerator strategy that is used.
 *
 */
public class FileDirectoryArchive implements Archive<File> {
	public static final String TAG = FileDirectoryArchive.class.getName();
	
	private final File archiveDir;
	private final NameGenerator nameGenerator;
	private final FileCopier fileCopier;
	private final DirectoryCleaner cleaner;
	
	public FileDirectoryArchive(File archiveDir, NameGenerator nameGenerator, FileCopier fileCopier, DirectoryCleaner cleaner) {
		this.archiveDir = archiveDir;
		this.nameGenerator = nameGenerator;
		this.fileCopier = fileCopier;
		this.cleaner = cleaner;
		archiveDir.mkdirs();
	}
	
	public static FileDirectoryArchive getSimpleFileArchive(File archiveDir) {
		return new FileDirectoryArchive(archiveDir, new NameGenerator.IdentityNameGenerator(), new FileCopier.SimpleFileCopier(), new DirectoryCleaner.KeepAll());
	}
	
	public static FileDirectoryArchive getTimestampedFileArchive(File archiveDir) {
		return new FileDirectoryArchive(archiveDir, new NameGenerator.TimestampNameGenerator(), new FileCopier.SimpleFileCopier(), new DirectoryCleaner.KeepAll());
	}
	
	public static FileDirectoryArchive getEncryptedTimestampedFileArchive(File archiveDir) {
		return new FileDirectoryArchive(archiveDir, new NameGenerator.TimestampNameGenerator(), new FileCopier.EncryptedFileCopier(), new DirectoryCleaner.KeepAll());
	}
	
	public static FileDirectoryArchive getRollingFileArchive(File archiveDir) {
		return new FileDirectoryArchive(archiveDir, new NameGenerator.IdentityNameGenerator(), new FileCopier.SimpleFileCopier(), new DirectoryCleaner.KeepMostRecent(50));
	}
	
	
	@Override
	public boolean add(File item) {
		File archiveFile = new File(archiveDir, nameGenerator.generateName(item.getName()));
		boolean result = fileCopier.copy(item, archiveFile);
		cleaner.clean(archiveDir);
		return result;
	}
	
	@Override
	public File[] getAll() {
		cleaner.clean(archiveDir);
		return archiveDir.listFiles();
	}

	@Override
	public boolean remove(File item) {
		if(contains(item)) {
			return item.delete();
		}
		return false;
	}
	
	public boolean contains(final File item) {
		final String itemFilename = item.getName();
		return archiveDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return itemFilename.equals(filename);			
			}
		}).length > 0;
	}
	

}
