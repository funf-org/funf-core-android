package edu.mit.media.funf.probe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.test.AndroidTestCase;

public class ProbeTest extends AndroidTestCase {

	private TestProbe testProbe;
	
	public class TestProbe extends Probe.Base implements Probe {
		public BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
		
		public static final String 
			ENABLED = "ENABLED",
			DISABLED = "DISABLED",
			STARTED = "STARTED",
			STOPPED = "STOPPED";
		
		
		@Override
		public synchronized void disablePassive() {
			// TODO Auto-generated method stub
			super.disablePassive();
		}

		@Override
		public synchronized void enablePassive() {
			// TODO Auto-generated method stub
			super.enablePassive();
		}

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			super.start();
		}

		@Override
		public synchronized void stop() {
			// TODO Auto-generated method stub
			super.stop();
		}

		@Override
		protected void onEnablePassive() {
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
		protected void onDisablePassive() {
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
			testProbe.disablePassive();
		}
		super.tearDown();
	}

	
	public void testFullStateFlow() {
		assertEquals(Probe.State.DISABLED, testProbe.getState());
		testProbe.enablePassive();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.ENABLED);
		testProbe.start();
		assertStateChange(testProbe, Probe.State.RUNNING, TestProbe.STARTED);
		testProbe.stop();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.STOPPED);
		testProbe.disablePassive();
		assertStateChange(testProbe, Probe.State.DISABLED, TestProbe.DISABLED);
	}
	
	public void testIndempotence() {
		assertEquals(Probe.State.DISABLED, testProbe.getState());
		testProbe.disablePassive();
		assertStateChange(testProbe, Probe.State.DISABLED);
		
		// Test multiple times to ensure it doesn't call onEnabled
		testProbe.enablePassive();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.ENABLED);
		testProbe.enablePassive();
		assertStateChange(testProbe, Probe.State.ENABLED);
		testProbe.enablePassive();
		assertStateChange(testProbe, Probe.State.ENABLED);
		
		testProbe.disablePassive();
		assertStateChange(testProbe, Probe.State.DISABLED, TestProbe.DISABLED);
	}
	
	public void testConfigChange() {
		testProbe.enablePassive();
		assertStateChange(testProbe, Probe.State.ENABLED, TestProbe.ENABLED);
		testProbe.start();
		assertStateChange(testProbe, Probe.State.RUNNING, TestProbe.STARTED);
		testProbe.setConfig(null);
		assertStateChange(testProbe, Probe.State.DISABLED, TestProbe.STOPPED, TestProbe.DISABLED);
	}
	
	/**
	 * Asserts that the current state is as defined at the end of the list of state changes.
	 * If no state changes are provided, it is assumed that there should NOT be a state change.
	 * @param theTestProbe
	 * @param correctState
	 * @param correctMessages
	 */
	private void assertStateChange(TestProbe theTestProbe, Probe.State correctState, String... correctMessages) {
		try {
			if (correctMessages == null) {
				correctMessages = new String[] { null };
			}
			for (String correctMessage : correctMessages) {
				String message = testProbe.messageQueue.poll(100, TimeUnit.MILLISECONDS);
				if (correctMessage == null) {
					assertNull(message);
				} else {
					assertNotNull(message);
					assertEquals(correctMessage, message);
				}
			}
			assertEquals(correctState, testProbe.getState());
		} catch (InterruptedException e) {
			fail();
		}
	}
	
}
