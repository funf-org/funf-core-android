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
