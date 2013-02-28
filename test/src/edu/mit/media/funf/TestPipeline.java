package edu.mit.media.funf;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import android.test.AndroidTestCase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;

public class TestPipeline extends AndroidTestCase {
	
	private Gson gson;
	private Queue<String> actions;
	
	public static final String 
	CREATED = "created",
	RAN = "ran",
	DESTROYED = "destroyed";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gson = FunfManager.getGsonBuilder(getContext()).create();
		actions = new LinkedList<String>();
	}

	public static class SamplePipeline implements Pipeline {
		
		public SamplePipeline() {
			
		}
		
		@Configurable
		private Map<String,Schedule> schedules;
		
		@Configurable
		private Object testComponent;
		
		private Object nonConfigurable;

		@Override
		public void onCreate(FunfManager manager) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRun(String action, JsonElement config) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDestroy() {
			// TODO Auto-generated method stub
			
		}

		public Map<String,Schedule> getSchedules() {
			return schedules;
		}

        @Override
        public boolean isEnabled() {
          // TODO Auto-generated method stub
          return false;
        }
	}
	
	public String SAMPLE_PIPELINE_CONFIG = "{" +
			"\"@type\": \"" + SamplePipeline.class.getName() + "\"," +
			"\"" + PipelineFactory.SCHEDULES_FIELD_NAME + "\": {" +
					"\"archive\": {" + 
						"\"interval\": 1," + 
						"\"opportunistic\": false" + 
					"}" +
			"}," +
			"\"testComponent\": {" +
				"\"@schedule\": {" +
					"\"duration\": 2" + 
				"}" +
			"}," +
			"\"nonConfigurable\": {" +
				"\"@schedule\": {" +
					"\"duration\": 2" + 
				"}" +
			"}" +
		"}";
	
	
	public void testPipelineLifecycle() {
	
	}
	
	public void testLoadPipeline() {
		Pipeline pipeline = gson.fromJson(SAMPLE_PIPELINE_CONFIG, Pipeline.class);
		assertTrue("Should respect runtime type.", pipeline instanceof SamplePipeline);
		SamplePipeline samplePipeline = (SamplePipeline)pipeline;
		Map<String,Schedule> schedules = samplePipeline.getSchedules();
		assertNotNull("Schedules should exists.", schedules);
		assertTrue("Schedule should contain explicit action", schedules.containsKey("archive"));
		Schedule archive = schedules.get("archive");
		assertEquals("Interval should have been set in schedule.", BigDecimal.ONE, archive.getInterval());
		assertEquals("Opportunistic should have been set in schedule.", false, archive.isOpportunistic());
		assertTrue("Schedule should contain annotation based action", schedules.containsKey("testComponent"));
		Schedule testComponent = schedules.get("testComponent");
		assertEquals("Interval should have been set in schedule.", new BigDecimal(2), testComponent.getDuration());
		assertFalse("Schedule should not contain schedule for non configurable item.", schedules.containsKey("nonConfigurable"));
	}
}
