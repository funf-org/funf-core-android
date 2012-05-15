package edu.mit.media.funf.pipeline;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.JsonUtils;



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
	
	
	public static class Builder {
		
		public static final String SCHEDULES_FIELD_NAME = "schedules";
		public static final String SCHEDULE = "@schedule";
		
		private Gson gson;
		
		public Builder() {
			this.gson = new Gson();
		}
		
		public Builder(Gson gson) {
			this.gson = gson;
		}
		
		public Pipeline load(String jsonConfig) {
			return load(new JsonParser().parse(jsonConfig).getAsJsonObject());
		}
		
		public Pipeline load(JsonObject el) {
			el = JsonUtils.deepCopy(el); // Work on copy, since we will be modifying
			
			// Load annotated schedules
			JsonObject annotatedSchedules = new JsonObject();
			// TODO: make this recursive to have nested schedules with dot notation
			for (Map.Entry<String,JsonElement> entry : el.entrySet()) {
				JsonElement entryEl = entry.getValue();
				if (entryEl.isJsonObject()) {
					JsonObject subConfig = entryEl.getAsJsonObject();
					if (subConfig.has(SCHEDULE)) {
						JsonElement scheduleConfig = subConfig.get(SCHEDULE);
						annotatedSchedules.add(entry.getKey(), scheduleConfig);
					}
				}
			}
			
			JsonObject existingSchedules = el.has(SCHEDULES_FIELD_NAME) ? el.getAsJsonObject(SCHEDULES_FIELD_NAME) : new JsonObject();
			JsonUtils.deepCopyOnto(annotatedSchedules, existingSchedules, true); // Not sure which should override, default to annotation
			
			// TODO: load annotated triggers
			
			
			Pipeline pipeline = gson.fromJson(el, Pipeline.class);
			return pipeline;
		}
	}
}
