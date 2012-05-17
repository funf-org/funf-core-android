/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.storage;

import java.io.File;
import java.io.FilenameFilter;

import edu.mit.media.funf.util.NameGenerator;

/**
 * A simple directory based archive for files.  
 * A file may be able to be archived more than once depending on the nameGenerator strategy that is used.
 *
 */
public class FileDirectoryArchive implements FileArchive {
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
	
	public static FileDirectoryArchive getRollingFileArchive(File archiveDir) {
		return new FileDirectoryArchive(archiveDir, new NameGenerator.IdentityNameGenerator(), new FileCopier.SimpleFileCopier(), new DirectoryCleaner.KeepUnderPercentageOfDiskFree(0.5, 10000000));
	}
	
	
	@Override
	public boolean add(File item) {
		this.archiveDir.mkdirs();
		File archiveFile = new File(archiveDir, nameGenerator.generateName(item.getName()));
		boolean result = fileCopier.copy(item, archiveFile);
		cleaner.clean(archiveDir);
		return result;
	}
	
	@Override
	public File[] getAll() {
		cleaner.clean(archiveDir);
		File[] files = archiveDir.listFiles();
		return (files == null) ? new File[0] : files;
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
		String[] files = archiveDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return itemFilename.equals(filename);			
			}
		});
		return files != null && files.length > 0;
	}
	

}
