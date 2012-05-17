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

/**
 * Delegates all actions to archive.  Adds an item to the backup archive before removing from the archive.
 * It is up to the backup archive to determine when those items will remove themselves.
 *
 */
public class BackedUpArchive implements FileArchive {

	private final FileArchive archive, backupArchive;
	
	public BackedUpArchive(FileArchive archive, FileArchive backupArchive) {
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
