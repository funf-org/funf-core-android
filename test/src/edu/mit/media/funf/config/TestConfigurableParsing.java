package edu.mit.media.funf.config;

import android.test.AndroidTestCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;

public class TestConfigurableParsing extends AndroidTestCase {

	private JsonObject getConfig() {
		JsonObject config = new JsonObject();
		
		return config;
	}
	
	private Gson getGson() {
		return new GsonBuilder().registerTypeAdapterFactory(
				new ConfigurableTypeAdapterFactory<TestConfigurable>(getContext(), TestConfigurable.class, Test1.class))
				.create();
	}
	
	public void testConfigurables() {
		Gson gson = getGson();
		Test1 test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
		assertEquals("Default was not set in configurable", 1, test1.overridden);
		assertEquals("Default private field was not set in configurable", 2, test1.getPrivateField());
		JsonObject expectedConfig = new JsonObject();
		expectedConfig.addProperty("@type", Test1.class.getName());
		expectedConfig.addProperty("overridden", 1);
		expectedConfig.addProperty("privateField", 2);
		assertEquals("Configurable not serialized correctly", expectedConfig, gson.toJsonTree(test1));
		
		test1 = gson.fromJson("{\"overridden\": 5, \"privateField\": 3}", Test1.class);
		assertEquals("Specified config was not set in configurable", 5, test1.overridden);
		assertEquals("Specified config on private field was not set in configurable", 3, test1.getPrivateField());
		
		// Test runtime config
		test1 = gson.fromJson("{\"@type\":\"" + Test2.class.getName() + "\", \"overridden\": 5, \"privateField\": 3}", Test1.class);
		assertTrue("Runtime type not created from config", test1 instanceof Test2);
		assertEquals("Specified config was not set in configurable", 5, test1.overridden);
		assertEquals("Specified config was not set in configurable", "5", ((Test2)test1).overridden);
		assertEquals("Specified config on private field was not set in configurable", 3, test1.getPrivateField());
		

		test1 = gson.fromJson("{\"notConfigurable\": \"yes\"}", Test1.class);
		assertEquals("Specified config was not set in configurable", "no", test1.notConfigurable);
		
		// Test default is used if not specified
		TestConfigurable test = gson.fromJson("{\"overridden\": 5, \"privateField\": 3}", TestConfigurable.class);
		assertTrue("Runtime type not created from config", test instanceof Test1);
		
		// Test nested types
		String nestedJson = "{\"@type\":\"" + Test2.class.getName() + "\", \"overridden\": 5, \"privateField\": 3, \"nested\": {\"@type\":\"" + Test2.class.getName() + "\", \"privateField\": 5}}";
		test = gson.fromJson(nestedJson, TestConfigurable.class);
		assertTrue("Runtime type not created from config", test instanceof Test2);
		Test2 test2 = (Test2)test;
		assertTrue("Runtime type not created from nested config", test2.nested instanceof Test2);
		assertEquals("Specified config was not set in nested configurable", 5, test2.nested.getPrivateField());
		String expectedSerialized = "{\"@type\":\"edu.mit.media.funf.config.TestConfigurableParsing$Test2\",\"nested\":{\"@type\":\"edu.mit.media.funf.config.TestConfigurableParsing$Test2\",\"nested\":{\"@type\":\"edu.mit.media.funf.config.TestConfigurableParsing$Test1\",\"overridden\":1,\"privateField\":2},\"overridden\":1,\"privateField\":5},\"overridden\":5,\"privateField\":3}";
		assertEquals("Configurable not serialized to json correctly", new JsonParser().parse(expectedSerialized), gson.toJsonTree(test));
		
	}
	
	public void testSingleton() {
		
	}
	
	public interface TestConfigurable {
		
	}
	
	public static class Test1 implements TestConfigurable {
		@Configurable
		public int overridden = 1;
		@Configurable
		private int privateField = 2;
		
		public String notConfigurable = "no";
		
		public int getPrivateField() {
			return privateField;
		}
	}
	
	public static class Test2 extends Test1 {
		@Configurable
		public String overridden = "test";
		@Configurable
		public Test1 nested = new Test1();
	}
}
