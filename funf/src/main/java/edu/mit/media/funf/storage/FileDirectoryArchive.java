/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
