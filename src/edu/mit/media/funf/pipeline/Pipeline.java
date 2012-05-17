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
	
}
