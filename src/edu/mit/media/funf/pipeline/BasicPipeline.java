package edu.mit.media.funf.pipeline;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class BasicPipeline implements Pipeline, DataListener {

	private Map<String,Schedule> schedulers = new HashMap<String, Schedule>();
	private FunfManager manager;
	private Gson gson;
	
	/*
	
	private Map<String,Schedule> schedules;
	private Map<String,Trigger> triggers;

	@Configurable
	private List<JsonElement> data;
	
	@Configurable
	private Storage storage;
	
	@Configurable
	private Uploader upload;
	
	@Configurable
	private Updater update;
*/
	
	// Build schedules and probe requests
	// Send to scheduler
	// Listen for data
	// Record data 
	// Archive data (optional)
	// Upload data (optional)
	
	// Update pipeline  (file, resource, or online)
	// This could simply put a json file to the proper location
	// FunfManager could be responsible for destroying this, and recreating
	
	// Needs to potentially be able to handle other scheduling of events
	

	@Override
	public void onCreate(FunfManager manager) {
		this.manager = manager;
		this.gson = manager.getGsonBuilder().create();
		
		/*
		Scheduler scheduler = manager.getScheduler();
		for (JsonElement dataRequest : data) {
			scheduler.set(this, dataRequest);
		}
		*/
		// For each data request, schedule
	}
	
	@Override
	public void onDestroy() {
		
	}

	@Override
	public void onRun(String action, JsonElement config) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
		// TODO Auto-generated method stub
		
	}
}
