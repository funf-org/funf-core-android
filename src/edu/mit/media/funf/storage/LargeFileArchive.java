/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import android.util.Log;
import android.webkit.MimeTypeMap;

import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.NameGenerator;
import edu.mit.media.funf.util.NameGenerator.IdentityNameGenerator;

public class LargeFileArchive extends DefaultArchive implements FileArchive {

	private FileArchive largeFileArchive; // Cache
	protected FileArchive getLargeFileArchive() {
		if (largeFileArchive == null) {
			synchronized (this) {
				if (largeFileArchive == null) {
					SecretKey key = getSecretKey();
					String rootSdCardPath = getPathOnSDCard();
					FileArchive backupArchive = FileDirectoryArchive.getRollingFileArchive(new File(rootSdCardPath + "backupLarge"));
					
					NameGenerator nameGenerator = new IdentityNameGenerator();
					FileCopier copier = (key == null) ? new FileCopier.SimpleFileCopier() : new FileCopier.EncryptedFileCopier(key, "DES");
					DirectoryCleaner cleaner = new DirectoryCleaner.KeepAll();
					FileArchive mainArchive = new FileDirectoryArchive(new File(rootSdCardPath + "archiveLarge"), nameGenerator, copier, cleaner);
					
					largeFileArchive = new BackedUpArchive(mainArchive, backupArchive);
				}
			}
		}
		return largeFileArchive;
	}


	protected boolean isLargeFile(File item) {
	    String extension = MimeTypeMap.getFileExtensionFromUrl(item.getAbsolutePath());
	    if (extension == null)
	        return false;
	    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	    if (type == null || "".equals(type) || "null".equals(type))
	        return false;
	    else 
	        return true;
	}

	@Override
	public boolean add(File item) {
		if (isLargeFile(item)) {
		    Log.d(LogUtil.TAG, "adding to large archive");
		    return getLargeFileArchive().add(item);
		}			
		else
			return getDelegateArchive().add(item);
	}

	@Override
	public boolean contains(File item) {
		if (isLargeFile(item))
			return getLargeFileArchive().contains(item);
		else
			return getDelegateArchive().contains(item);
	}

	@Override
	public File[] getAll() {
		List<File> dbFiles = Arrays.asList(getDelegateArchive().getAll());
		List<File> largeFiles = Arrays.asList(getLargeFileArchive().getAll());
		List<File> allFiles = new ArrayList<File>();
		allFiles.addAll(dbFiles);
		allFiles.addAll(largeFiles);
		File[] allFilesArray = new File[allFiles.size()];
		return allFiles.toArray(allFilesArray);
	}

	@Override
	public boolean remove(File item) {
		if (isLargeFile(item))
			return getLargeFileArchive().remove(item);
		else
			return getDelegateArchive().remove(item);
	}
}
