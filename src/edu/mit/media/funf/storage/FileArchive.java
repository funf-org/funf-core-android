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
 * Responsible for storing a representation of the object.
 */
public interface FileArchive {

	/**
	 * Adds the item to the archive
	 * @param item
	 * @return true if archive was successful
	 */
	public boolean add(File item);
	
	/**
	 * Removes the item from the archive if it exists
	 * @param item
	 * @return true if item exists in archive and was successfully removed
	 */
	public boolean remove(File item);
	
	/**
	 * @param item
	 * @return true if item exists in archive
	 */
	public boolean contains(File item);
	
	
	/**
	 * @return All items in the archive
	 */
	public File[] getAll();
	
	

}