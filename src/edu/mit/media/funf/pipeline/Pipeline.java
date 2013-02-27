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
package edu.mit.media.funf.pipeline;

import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;



public interface Pipeline {
	
	/**
	 * Called once when the pipeline is created.  This method can be used
	 * to register any scheduled operations.
	 * 
	 * @param manager
	 */
	public void onCreate(FunfManager manager);
	
	/**
	 * Instructs pipeline to perform an operation.
	 * @param action The action to perform.
	 * @param config The object to perform the action on.
	 */
	public void onRun(String action, JsonElement config); // maybe intent, IJsonObject?
	
	/**
	 * The teardown method called once when the pipeline should shut down.
	 */
	public void onDestroy();
	
	/**
	 * Returns true if this pipeline is enabled, meaning onCreate has been called 
	 * and onDestroy has not yet been called.
	 */
	public boolean isEnabled();
	
}
