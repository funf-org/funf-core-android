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
