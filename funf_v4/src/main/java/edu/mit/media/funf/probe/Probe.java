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

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.data.DataNormalizer;
import edu.mit.media.funf.json.BundleTypeAdapter;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.security.HashUtil;
import edu.mit.media.funf.security.HashUtil.HashingType;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LockUtil;

public interface Probe {

	public static final String DEFAULT_CONFIG = "{}";
	public static final boolean DEFAULT_OPPORTUNISTIC = true;
	public static final boolean DEFAULT_STRICT = false;
	public static final double DEFAULT_PERIOD = 3600;
	
	/**
	 * Listeners added to this probe will receive data callbacks from this
	 * probe, until this listener is unregistered. The probe should continue to
	 * be active and send data to this listener until it is unregistered. This
	 * method is indempotent and thread safe.
	 * 
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

	

	public interface PassiveProbe extends Probe {

		/**
		 * Listeners removed from this probe will no longer receive data
		 * callbacks from this probe. This method is indempotent and thread
		 * safe.
		 * 
		 * @param listener
		 */
		public void registerPassiveListener(DataListener... listener);

		/**
		 * Listeners removed from this probe will no longer receive data
		 * callbacks from this probe. This method is indempotent and thread
		 * safe.
		 * 
		 * @param listener
		 */
		public void unregisterPassiveListener(DataListener... listener);
	}

	public interface ContinuousProbe extends Probe {
		public static final double DEFAULT_DURATION = 60; // One minute

		/**
		 * Listeners removed from this probe will no longer receive data
		 * callbacks from this probe. This method is indempotent and thread
		 * safe.
		 * 
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
		 * @return The checkpoint that represents the state of the data stream
		 *         at the point this is called.
		 */
		public JsonElement getCheckpoint();

		/**
		 * Sets the checkpoint the probe should start the sending data from.
		 * Like setConfig, calling this will disable the probe.
		 * 
		 * @param checkpoint
		 */
		public void setCheckpoint(JsonElement checkpoint);
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
		 * Called when the probe emits data. Data emitted from probes that
		 * extend the Probe class are guaranteed to have the PROBE and TIMESTAMP
		 * parameters.
		 * 
		 * @param data
		 */
		public void onDataReceived(IJsonObject probeConfig, IJsonObject data);

		/**
		 * Called when the probe is finished sending a stream of data. This can
		 * be used to know when the probe was run, even if it didn't send data.
		 * It can also be used to get a checkpoint of far through the data
		 * stream the probe ran. Continuable probes can use this checkpoint to
		 * start the data stream where it previously left off.
		 * 
		 * @param completeProbeUri
		 * @param checkpoint
		 */
		public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint);
	}

	/**
	 * Interface implemented by Probe status observers.
	 */
	public interface StateListener {

		/**
		 * Called when the probe emits a status message, which can happen when
		 * the probe changes state.
		 * 
		 * @param status
		 */
		public void onStateChanged(Probe probe, State previousState);
	}

	/**
	 * Types to represent the current state of the probe. Provides the
	 * implementation of the ProbeRunnable state machine.
	 * 
	 */
	public static enum State {

		// TODO: should we try catch, to prevent one probe from killing all
		// probes?
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
						JsonElement el = JsonUtils.immutable(probe.getGson().toJsonTree(probe));
						probe.lock = LockUtil.getWakeLock(probe.getContext(), el.toString());
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
	public abstract class Base implements Probe, BaseProbeKeys {

		private Context context;

		/**
		 * No argument constructor requires that setContext be called manually.
		 */
		public Base() {
			state = State.DISABLED;
		}

		public Base(Context context) {
			this();
			this.context = context;
		}

		private Gson gson;

		protected Gson getGson() {
			if (gson == null) {
				gson = getGsonBuilder().create();
			}
			return gson;
		}

		protected GsonBuilder getGsonBuilder() {
			GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapterFactory(FunfManager.getProbeFactory(getContext()));
			TypeAdapterFactory adapterFactory = getSerializationFactory();
			if (adapterFactory != null) {
				builder.registerTypeAdapterFactory(adapterFactory);
			}
			builder.registerTypeAdapterFactory(BundleTypeAdapter.FACTORY);
			return builder;
		}

		// TODO: figure out how to get scheduler to use source data requests to
		// schedule appropriately
		// Probably will need to prototype with ActivityProbe
		@Deprecated
		public Map<String, JsonObject> getSourceDataRequests() {
			return null;
		}

		private IJsonObject config;
		public IJsonObject getConfig() {
			if (config == null) {
				config = new IJsonObject(getGson().toJsonTree(this).getAsJsonObject());
			}
			return config;
		}

		protected Context getContext() {
			return context;
		}

		/*****************************************
		 * Probe Data Listeners
		 *****************************************/
		private Set<DataListener> dataListeners = Collections.synchronizedSet(new HashSet<DataListener>());
		private Set<DataListener> passiveDataListeners = Collections.synchronizedSet(new HashSet<DataListener>());

		/**
		 * Returns the set of data listeners. Make sure to synchronize on this
		 * object, if you plan to modify it or iterate over it.
		 */
		protected Set<DataListener> getDataListeners() {
			return dataListeners;
		}

		/**
		 * Returns the set of passive data listeners. Make sure to synchronize
		 * on this object, if you plan to modify it or iterate over it.
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
				checkpoint = ((ContinuableProbe) this).getCheckpoint();
			}
			return checkpoint;
		}

		public void unregisterListener(DataListener... listeners) {
			if (listeners != null) {
				JsonElement checkpoint = getCheckpointIfContinuable();
				for (DataListener listener : listeners) {
					dataListeners.remove(listener);
					listener.onDataCompleted(getConfig(), checkpoint);
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
			DataListener[] listeners = null;
			synchronized (dataListeners) {
				listeners = new DataListener[dataListeners.size()];
				dataListeners.toArray(listeners);
			}
			unregisterListener(listeners);
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
					listener.onDataCompleted(getConfig(), checkpoint);
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
				if (handler != null) {
					Message dataMessage = handler.obtainMessage(SEND_DATA_MESSAGE, data);
					handler.sendMessage(dataMessage);
				}
			} else {
				if (!data.has(TIMESTAMP)) {
					data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
				}
				IJsonObject immutableData = new IJsonObject(data);
				synchronized (dataListeners) {
					for (DataListener listener : dataListeners) {
						listener.onDataReceived(getConfig(), immutableData);
					}
				}
				synchronized (passiveDataListeners) {
					for (DataListener listener : passiveDataListeners) {
						if (!dataListeners.contains(listener)) { // Don't send
																	// data
																	// twice to
																	// passive
																	// listeners
							listener.onDataReceived(getConfig(), immutableData);
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

		private void ensureLooperThreadExists() {
			if (looper == null) {
				synchronized (this) {
					if (looper == null) {
						HandlerThread thread = new HandlerThread("Probe[" + getClass().getName() + "]");
						thread.start();
						looper = thread.getLooper();
						handler = new Handler(looper, new ProbeHandlerCallback());
					}
				}
			}
		}

		protected final void enable() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(ENABLE_MESSAGE));
		}

		protected final void start() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(START_MESSAGE));
		}

		protected final void stop() {
			ensureLooperThreadExists();
			handler.sendMessage(handler.obtainMessage(STOP_MESSAGE));
		}

		protected final void disable() {
			if (handler != null) {
				handler.sendMessage(handler.obtainMessage(DISABLE_MESSAGE));
			}
		}

		public void destroy() {
			disable();
		}

		/**
		 * Called when the probe switches from the disabled to the enabled
		 * state. This is where any passive or opportunistic listeners should be
		 * configured. An enabled probe should not keep a wake lock. If you need
		 * the device to stay awake consider implementing a StartableProbe, and
		 * using the onStart method.
		 */
		protected void onEnable() {

		}

		/**
		 * Called when the probe switches from the enabled state to active
		 * running state. This should be used to send any data broadcasts, but
		 * must return quickly. If you have any long running processes they
		 * should be started on a separate thread created by this method, or
		 * should be divided into short runnables that are posted to this
		 * threads looper one at a time, to allow for the probe to change state.
		 */
		protected void onStart() {

		}

		/**
		 * Called with the probe switches from the running state to the enabled
		 * state. This method should be used to stop any running threads
		 * emitting data, or remove a runnable that has been posted to this
		 * thread's looper. Any passive listeners should continue running.
		 */
		protected void onStop() {

		}

		/**
		 * Called with the probe switches from the enabled state to the disabled
		 * state. This method should be used to stop any passive listeners
		 * created in the onEnable method. This is the time to cleanup and
		 * release any resources before the probe is destroyed.
		 */
		protected void onDisable() {

		}

		private volatile Looper looper;
		private volatile Handler handler;

		/**
		 * Access to the probe thread's handler.
		 * 
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

		protected static final int ENABLE_MESSAGE = 1, START_MESSAGE = 2, STOP_MESSAGE = 3, DISABLE_MESSAGE = 4,
				SEND_DATA_MESSAGE = 5, SEND_DATA_COMPLETE_MESSAGE = 6;

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
						sendData((JsonObject) msg.obj);
					}
					break;
				case SEND_DATA_COMPLETE_MESSAGE:
					if (msg.obj instanceof JsonObject) {
						sendData((JsonObject) msg.obj);
					}
					break;
				default:
					return Base.this.handleMessage(msg);
				}
				return true; // Message was handled
			}

		}

		/*****************************************
		 * Probe State Listeners
		 *****************************************/

		private Set<StateListener> stateListeners = Collections.synchronizedSet(new HashSet<StateListener>());

		/**
		 * Returns the set of status listeners. Make sure to synchronize on this
		 * object, if you plan to modify it or iterate over it.
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

		/** 
		 * Sensitive data is hidden by default
		 * This can not be changed by configuration alone
		 * If you uncomment this line please submit the change
		 * to funf@media.mit.edu in accordance with the LGPL license. *
		 */
		//@Configurable
		private boolean hideSensitiveData = true;

		protected final String sensitiveData(String data) {
			return sensitiveData(data, null);
		}

		protected final String sensitiveData(String data, DataNormalizer<String> normalizer) {
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
		 * Used to override the serialiazation technique for multiple types.
		 * You can override this method to have getGson() return a Gson object that includes your TypeAdapterFactory.
		 * 
		 * @return
		 */
		protected TypeAdapterFactory getSerializationFactory() {
			return null;
		}

		/**
		 * Return true if this should be wake locked while in running.  
		 * For most probes this will return true, but some may need to allow the device to sleep.
		 * @return
		 */
		protected boolean isWakeLockedWhileRunning() {
			return true;
		}
	}
}
