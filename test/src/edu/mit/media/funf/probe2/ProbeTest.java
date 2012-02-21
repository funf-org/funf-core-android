package edu.mit.media.funf.probe2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.test.AndroidTestCase;

public class ProbeTest extends AndroidTestCase {

	private TestProbe testProbe;
	
	public class TestProbe extends Probe {
		public BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
		
		public static final String 
			ENABLED = "ENABLED",
			DISABLED = "DISABLED",
			STARTED = "STARTED",
			STOPPED = "STOPPED";
		
		@Override
		protected void onEnable() {
			messageQueue.offer(ENABLED);
		}

		@Override
		protected void onStart() {
			messageQueue.offer(STARTED);
		}

		@Override
		protected void onStop() {
			messageQueue.offer(STOPPED);
		}

		@Override
		protected void onDisable() {
			messageQueue.offer(DISABLED);
		}
		
		
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testProbe = new TestProbe();
	}

	@Override
	protected void tearDown() throws Exception {
		if (testProbe != null) {
			testProbe.disable();
		}
		super.tearDown();
	}

	
	public void testFullStateFlow() {
		assertEquals(Probe.State.DISABLED, testProbe.getState());
		testProbe.enable();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.ENABLED);
		testProbe.start();
		assertStateChange(testProbe, Probe.State.RUNNING, TestProbe.STARTED);
		testProbe.stop();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.STOPPED);
		testProbe.disable();
		assertStateChange(testProbe, Probe.State.DISABLED, TestProbe.DISABLED);
	}
	
	public void testIndempotence() {
		assertEquals(Probe.State.DISABLED, testProbe.getState());
		testProbe.disable();
		assertStateChange(testProbe, Probe.State.DISABLED, null);
		
		// Test multiple times to ensure it doesn't call onEnabled
		testProbe.enable();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.ENABLED);
		testProbe.enable();
		assertStateChange(testProbe, Probe.State.ENABLED, null);
		testProbe.enable();
		assertStateChange(testProbe, Probe.State.ENABLED, null);
		
		testProbe.disable();
		assertStateChange(testProbe, Probe.State.DISABLED, TestProbe.DISABLED);
	}
	
	private void assertStateChange(TestProbe theTestProbe, Probe.State correctState, String correctMessage) {
		try {
			String message = testProbe.messageQueue.poll(100, TimeUnit.MILLISECONDS);
			if (correctMessage == null) {
				assertNull(message);
			} else {
				assertNotNull(message);
				assertEquals(correctMessage, message);
			}
			assertEquals(correctState, testProbe.getState());
		} catch (InterruptedException e) {
			fail();
		}
	}
	
}
