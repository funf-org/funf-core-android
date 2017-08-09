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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Attempts to use the first archive. If that fails, continues down the list until a successful archive is reached.
 *
 */
public class CompositeFileArchive implements FileArchive {

	private final FileArchive[] archives;
	
	public CompositeFileArchive(FileArchive... archives) {
		this.archives = archives;
	}
	
	@Override
	public boolean add(File item) {
		// Add to archive one by one, until one is successful or archives are exhausted.
		for (FileArchive archive : archives) {
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
		for (FileArchive archive : archives) {
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
		for (FileArchive archive : archives) {
			if(archive.remove(item)) {
				success = true;
			}
		}
		return success;
	}

	@Override
	public boolean contains(File item) {
		for (FileArchive archive : archives) {
			if (archive.contains(item)) {
				return true;
			}
		}
		return false;
	}

}
