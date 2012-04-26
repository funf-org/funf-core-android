package edu.mit.media.funf.probe;


import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapterFactory;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.config.ConfigurableObjectFactory;
import edu.mit.media.funf.data.DataNormalizer;
import edu.mit.media.funf.json.BundleTypeAdapter;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.security.HashUtil;
import edu.mit.media.funf.security.HashUtil.HashingType;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LockUtil;

public interface Probe extends Configurable {
	
	public static final boolean DEFAULT_OPPORTUNISTIC = true;
	public static final boolean DEFAULT_STRICT = false;
	

	/**
	 * Listeners added to this probe will receive data callbacks from this probe,
	 * until this listener is unregistered.  The probe should continue to be active
	 * and send data to this listener until it is unregistered.
	 * This method is indempotent and thread safe.
	 * @param listener
	 */
	public void registerListener(DataListener... listener);
	
	/**
	 * Remove all listeners and disable;
	 */
	public void destroy();

	/**
	 * @return the current state of the probe.
	 */
	public State getState();
	public void addStateListener(StateListener listener);
	public void removeStateListener(StateListener listener);
	

	public static final double DEFAULT_PERIOD = 3600; 
	
	public interface PassiveProbe extends Probe {


		/**
		 * Listeners removed from this probe will no longer receive data callbacks from this probe.
		 * This method is indempotent and thread safe.
		 * @param listener
		 */
		public void registerPassiveListener(DataListener... listener);
		
		/**
		 * Listeners removed from this probe will no longer receive data callbacks from this probe.
		 * This method is indempotent and thread safe.
		 * 
		 * @param listener
		 */
		public void unregisterPassiveListener(DataListener... listener);
	}
	
	public interface ContinuousProbe extends Probe {
		public static final double DEFAULT_DURATION = 60; // One minute
		
		/**
		 * Listeners removed from this probe will no longer receive data callbacks from this probe.
		 * This method is indempotent and thread safe.
		 * @param listener
		 */
		public void unregisterListener(DataListener... listener);
		
	}
	
	/**
	 * A probe that can continue where it left off, using a checkpoint.
	 *
	 */
	public interface ContinuableProbe extends Probe {
		

		/**
		 * @return The checkpoint that represents the state of the data stream at the point this is called.
		 */
		public JsonElement getCheckpoint();
		
		/**
		 * Sets the checkpoint the probe should start the sending data from.  Like setConfig, 
		 * calling this will disable the probe.
		 * @param checkpoint
		 */
		public void setCheckpoint(JsonElement checkpoint);
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface DefaultSchedule {
		String value() default "";
		double period() default Probe.DEFAULT_PERIOD;
		double duration() default ContinuousProbe.DEFAULT_DURATION;
		boolean opportunistic() default DEFAULT_OPPORTUNISTIC;
		boolean strict() default DEFAULT_STRICT;
	}

	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface DisplayName {
		String value();
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface Description {
		String value();
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface RequiredPermissions {
		String[] value();
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface RequiredFeatures {
		String[] value();
	}
	
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@Inherited
	public @interface RequiredProbes {
		Class<? extends Probe>[] value();
	}
	
	
	/**
	 * Interface implemented by Probe data observers.
	 */
	public interface DataListener {

		/**
		 * Called when the probe emits data.  Data emitted from probes that extend the Probe class
		 * are guaranteed to have the PROBE and TIMESTAMP parameters.
		 * @param data
		 */
		public void onDataReceived(Uri completeProbeUri, JsonObject data);
		

		/**
		 * Called when the probe is finished sending a stream of data.  This can be used to know when the probe
		 * was run, even if it didn't send data.  It can also be used to get a checkpoint of far through the
		 * data stream the probe ran.  Continuable probes can use this checkpoint to start the data stream 
		 * where it previously left off.
		 * @param completeProbeUri
		 * @param checkpoint
		 */
		public void onDataCompleted(Uri completeProbeUri, JsonElement checkpoint);
	}
	

	/**
	 * Interface implemented by Probe status observers.
	 */
	public interface StateListener {

		/**
		 * Called when the probe emits a status message, which can happen when the probe changes state.
		 * @param status
		 */
		public void onStateChanged(Probe probe, State previousState);
	}
	
	public static final String PROBE_SCHEME = "probe";
	public static final ConfigurableUri<Probe> PROBE_URI = new ConfigurableUri<Probe>(PROBE_SCHEME);
	
	/**
	 * Types to represent the current state of the probe.
	 * Provides the implementation of the ProbeRunnable state machine.
	 *
	 */
	public static enum State {

		// TODO: should we try catch, to prevent one probe from killing all probes?
		DISABLED {

			@Override
			protected void enable(Base probe) {
				synchronized (probe) {
					probe.state = ENABLED;
					probe.onEnable();
					probe.notifyStateChange(this);
				}
			}
	
			@Override
			protected void start(Base probe) {
				synchronized (probe) {
					enable(probe);
					if (probe.state == ENABLED) {
						ENABLED.start(probe);
					}
				}
			}
	
			@Override
			protected void stop(Base probe) {
				// Nothing
			}
	
			@Override
			protected void disable(Base probe) {
				// Nothing
			}
		},
		ENABLED {

			@Override
			protected void enable(Base probe) {
				// Nothing
			}
	
			@Override
			protected void start(Base probe) {
				synchronized (probe) {
					if (probe.isWakeLockedWhileRunning()) {
						Uri probeUri = probe.getUri();
						probe.lock = LockUtil.getWakeLock(probe.getContext(), probeUri.toString());
					}
					probe.state = RUNNING;
					probe.onStart();
					probe.notifyStateChange(this);
				}
			}
	
			@Override
			protected void stop(Base probe) {
				// Nothing
			}
	
			@Override
			protected void disable(Base probe) {
				synchronized (probe) {
					probe.state = DISABLED;
					probe.onDisable();
					probe.notifyStateChange(this);
					probe.passiveDataListeners.clear();
					probe.dataListeners.clear();
					// Shutdown handler thread
					probe.looper.quit();
					probe.looper = null;
					probe.handler = null;
				}
			}
		},
		RUNNING {
 
			@Override
			protected void enable(Base probe) {
				// Nothing
			}
	
			@Override
			protected void start(Base probe) {
				// Nothing
			}
	
			@Override
			protected void stop(Base probe) {
				synchronized (probe) {
					probe.state = ENABLED;
					probe.onStop();
					probe.notifyStateChange(this);
					probe.unregisterAllListeners();
					if (probe.lock != null && probe.lock.isHeld()) {
						probe.lock.release();
						probe.lock = null;
					}
				}
			}
	
			@Override
			protected void disable(Base probe) {
				synchronized (probe) {
					stop(probe);
					if (probe.state == ENABLED) {
						ENABLED.disable(probe);
					}
				}
			}
		};
		
		protected abstract void enable(Base probe);
		protected abstract void disable(Base probe);
		protected abstract void start(Base probe);
		protected abstract void stop(Base probe);
		
	}

	@DefaultSchedule
	public abstract class Base extends ConfigurableBase implements Probe, BaseProbeKeys {
		
		/**
		 * No argument constructor requires that setContext be called manually.
		 */
		public Base() {
			state = State.DISABLED;
		}
		
		public Base(Context context) {
			this();
			setContext(context);
		}
		
		public Base(Context context, ConfigurableObjectFactory factory) {
			this(context);
			setFactory(factory);
		}
		
		private Gson gson;
		protected Gson getGson() {
			if (gson == null) {
				gson = getGsonBuilder().create(); 
			}
			return gson;
		}
		
		protected GsonBuilder getGsonBuilder() {
			GsonBuilder builder = super.getGsonBuilder();
			TypeAdapterFactory adapterFactory = getSerializationFactory();
			if (adapterFactory != null) {
				builder.registerTypeAdapterFactory(adapterFactory);
			}
			builder.registerTypeAdapterFactory(BundleTypeAdapter.FACTORY);
			return builder;
		}
		
		// TODO: figure out how to get scheduler to use source data requests to schedule appropriately
		// Probably will need to prototype with ActivityProbe
		public Map<String,JsonObject> getSourceDataRequests() {
			return null;
		}
		
		public JsonObject getDiffConfig(Bundle existingConfig, JsonObject... data) {
			return null;
		}


		@Override
		public synchronized void setConfig(JsonObject config) {
			disable();
			super.setConfig(config);
		}
		
		@Override
		protected Context getContext() {
			// Overridden to give the state machine access, without making public
			return super.getContext();
		}
	
		/*****************************************
		 * Probe Data Listeners
		 *****************************************/
		private Set<DataListener> dataListeners = Collections.synchronizedSet(new HashSet<DataListener>());
		private Set<DataListener> passiveDataListeners = Collections.synchronizedSet(new HashSet<DataListener>());
		
		/**
		 * Returns the set of data listeners.  Make sure to synchronize on this object, 
		 * if you plan to modify it or iterate over it.
		 */
		protected Set<DataListener> getDataListeners() {
			return dataListeners;
		}
		
		/**
		 * Returns the set of passive data listeners.  Make sure to synchronize on this object, 
		 * if you plan to modify it or iterate over it.
		 */
		protected Set<DataListener> getPassiveDataListeners() {
			return dataListeners;
		}
		
		@Override
		public void registerListener(DataListener... listeners) {
			if (listeners != null) {
				for (DataListener listener : listeners) {
					dataListeners.add(listener);
				}
				start();
			}
		}
		
		private JsonElement getCheckpointIfContinuable() {
			JsonElement checkpoint = null;
			if (this instanceof ContinuableProbe) {
				checkpoint = ((ContinuableProbe)this).getCheckpoint();
			}
			return checkpoint;
		}
	
		public void unregisterListener(DataListener... listeners) {
			if (listeners != null) {
				JsonElement checkpoint = getCheckpointIfContinuable();
				for (DataListener listener : listeners) {
					dataListeners.remove(listener);
					listener.onDataCompleted(getCompleteUri(), checkpoint);
				}
				// If no one is listening, stop using device resources
				if (dataListeners.isEmpty()) {
					stop();
				}
				if (passiveDataListeners.isEmpty()) {
					disable();
				}
			}
		}
		
		protected void unregisterAllListeners() {
			synchronized (dataListeners) {
				DataListener[] listeners = new DataListener[dataListeners.size()];
				dataListeners.toArray(listeners);
				unregisterListener(listeners);
			}
		}
		
		public void registerPassiveListener(DataListener... listeners) {
			if (listeners != null) {
				for (DataListener listener : listeners) {
					dataListeners.add(listener);
				}
				enable();
			}
		}
	
		public void unregisterPassiveListener(DataListener... listeners) {
			if (listeners != null) {
				JsonElement checkpoint = getCheckpointIfContinuable();
				for (DataListener listener : listeners) {
					dataListeners.remove(listener);
					listener.onDataCompleted(getCompleteUri(), checkpoint);
				}
				// If no one is listening, stop using device resources
				if (dataListeners.isEmpty() && passiveDataListeners.isEmpty()) {
					disable();
				}
			}
		}
		
		protected void unregisterAllPassiveListeners() {
			synchronized (passiveDataListeners) {
				DataListener[] listeners = new DataListener[passiveDataListeners.size()];
				passiveDataListeners.toArray(listeners);
				unregisterPassiveListener(listeners);
			}
		}
		
		protected void notifyStateChange(State previousState) {
			synchronized (stateListeners) {
				for (StateListener listener : stateListeners) {
					listener.onStateChanged(this, previousState);
				}
			}
		}
		
		protected void sendData(final JsonObject data) {
			if (data == null || looper == null) {
				return;
			} else if (Thread.currentThread() != looper.getThread()) {
				// Ensure the data send runs on the probe's thread
				Message dataMessage = handler.obtainMessage(SEND_DATA_MESSAGE, data);
				handler.sendMessage(dataMessage);
			} else {
				if (!data.has(TIMESTAMP)) {
					data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
				}
				synchronized (dataListeners) {
					for (DataListener listener : dataListeners) {
						listener.onDataReceived(getCompleteUri(), JsonUtils.deepCopy(data));
					}
				}
				synchronized (passiveDataListeners) {
					for (DataListener listener : passiveDataListeners) {
						if (!dataListeners.contains(listener)) { // Don't send data twice to passive listeners
							listener.onDataReceived(getCompleteUri(), JsonUtils.deepCopy(data));
						}
					}
				}
			}
		}
		
		
		/*****************************************
		 * Probe State Machine
		 *****************************************/
		
		private State state;
		private PowerManager.WakeLock lock;
		
		@Override
		public State getState() {
			return state;
		}
		
		private synchronized void ensureLooperThreadExists() {
			if (looper == null) {
				HandlerThread thread = new HandlerThread("Probe[" + getClass().getName() + "]");
		        thread.start();
		        looper = thread.getLooper();
		        handler = new Handler(looper, new ProbeHandlerCallback());
			}
		}
		
		protected final synchronized void enable() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(ENABLE_MESSAGE));
		}
	
		protected final synchronized void start() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(START_MESSAGE));
		}
	
		protected final synchronized void stop() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(STOP_MESSAGE));
		}
	
		protected final synchronized void disable() {
			if (handler != null) {
				handler.sendMessage(handler.obtainMessage(DISABLE_MESSAGE));
			}
		}
		

		public void destroy() {
			disable();
		}
		
	
		/**
		 * Called when the probe switches from the disabled to the enabled state.  
		 * This is where any passive or opportunistic listeners should be configured.
		 * An enabled probe should not keep a wake lock.  If you need 
		 * the device to stay awake consider implementing a StartableProbe, and
		 * using the onStart method.
		 */
		protected void onEnable() {
			
		}
		
		/**
		 * Called when the probe switches from the enabled state to active running state.  
		 * This should be used to send any data broadcasts, but must return quickly.
		 * If you have any long running processes they should be started on a separate thread 
		 * created by this method, or should be divided into short runnables that are posted to 
		 * this threads looper one at a time, to allow for the probe to change state.
		 */
		protected void onStart() {
			
		}
		
		/**
		 * Called with the probe switches from the running state to the enabled state.  
		 * This method should be used to stop any running threads emitting data, or remove
		 * a runnable that has been posted to this thread's looper.
		 * Any passive listeners should continue running.
		 */
		protected void onStop() {
			
		}
		
		/**
		 * Called with the probe switches from the enabled state to the disabled state.  
		 * This method should be used to stop any passive listeners created in the onEnable method.
		 * This is the time to cleanup and release any resources before the probe is destroyed.
		 */
		protected void onDisable() {
			
		}
		
		
	
		private volatile Looper looper;
		private volatile Handler handler;
		
		/**
		 * Access to the probe thread's handler.
		 * @return
		 */
		protected Handler getHandler() {
			return handler;
		}
		
		/**
		 * @param msg
		 * @return
		 */
		protected boolean handleMessage(Message msg) {
			// For right now don't handle any messages, only runnables
			return false;
		}
		
		protected static final int 
			ENABLE_MESSAGE = 1,
			START_MESSAGE = 2,
			STOP_MESSAGE = 3,
			DISABLE_MESSAGE = 4,
			SEND_DATA_MESSAGE = 5,
			SEND_DATA_COMPLETE_MESSAGE = 6;
		
		private class ProbeHandlerCallback implements Handler.Callback {
	
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case ENABLE_MESSAGE:
					state.enable(Base.this);
					break;
				case START_MESSAGE:
					state.start(Base.this);
					break;
				case STOP_MESSAGE:
					state.stop(Base.this);
					break;
				case DISABLE_MESSAGE:
					state.disable(Base.this);
					break;
				case SEND_DATA_MESSAGE:
					if (msg.obj instanceof JsonObject) {
						sendData((JsonObject)msg.obj);
					}
					break;
				case SEND_DATA_COMPLETE_MESSAGE:
					if (msg.obj instanceof JsonObject) {
						sendData((JsonObject)msg.obj);
					}
					break;
				default:
					return Base.this.handleMessage(msg);
				}
				return true;  // Message was handled
			}
			
		}
		
		
	
		
		/*****************************************
		 * Probe State Listeners
		 *****************************************/
		
		
		private Set<StateListener> stateListeners = Collections.synchronizedSet(new HashSet<StateListener>());
		
		/**
		 * Returns the set of status listeners.  Make sure to synchronize on this object, 
		 * if you plan to modify it or iterate over it.
		 */
		protected Set<StateListener> getStateListeners() {
			return stateListeners;
		}
		
		@Override
		public void addStateListener(StateListener listener) {
			stateListeners.add(listener);
		}
	
		@Override
		public void removeStateListener(StateListener listener) {
			stateListeners.remove(listener);
		}
		
		
		
		
		/**********************************
		 * Sensitive Data
		 ********************************/
		
		@ConfigurableField
		private boolean hideSensitiveData = true;
		
		protected String sensitiveData(String data) {
			return sensitiveData(data, null);
		}
		
		protected String sensitiveData(String data, DataNormalizer<String> normalizer) {
			if (hideSensitiveData) {
				if (normalizer != null) {
					data = normalizer.normalize(data);
				}
				return HashUtil.hashString(getContext(), data, HashingType.ONE_WAY_HASH);
			} else {
				return data;
			}
		}
		
		/**********************************
		 * Custom serialization
		 ********************************/
		
		/**
		 * Used to override the serialiazation technique for multiple types
		 * @return
		 */
		protected TypeAdapterFactory getSerializationFactory() {
			return null;
		}
		
		protected boolean isWakeLockedWhileRunning() {
			return true;
		}
	}
}
