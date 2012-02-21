package edu.mit.media.funf.probe2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;



public abstract class Probe implements ProbeRunnable {
	
	
	/**
	 * No argument constructor requires that setContext be called manually.
	 */
	public Probe() {
		state = State.DISABLED;
	}
	
	public Probe(Context context) {
		this();
		setContext(context);
	}
	


	private Context context;
	public void setContext(Context context) {
		if (context == null) {
			throw new RuntimeException("Attempted to set a null context in probe '" + getClass().getName() + "'");
		}
		this.context = context.getApplicationContext();
	}
	protected Context getContext() {
		if (context == null) {
			throw new RuntimeException("Context was never set for probe '" + getClass().getName() + "'");
		}
		return context;
	}

	/*****************************************
	 * Probe Data Listeners
	 *****************************************/
	
	/**
	 * Interface implemented by Probe data observers.
	 */
	public interface DataListener {

		/**
		 * Called when the probe emits data.  Data emitted from probes that extend the Probe class
		 * are guaranteed to have the PROBE and TIMESTAMP parameters.
		 * @param data
		 */
		public void onDataReceived(Bundle data);
	}
	
	private Set<DataListener> dataListeners = Collections.synchronizedSet(new HashSet<DataListener>());
	
	/**
	 * Returns the set of data listeners.  Make sure to synchronize on this object, 
	 * if you plan to modify it or iterate over it.
	 */
	protected Set<DataListener> getDataListeners() {
		return dataListeners;
	}
	
	@Override
	public void addDataListener(DataListener listener) {
		dataListeners.add(listener);
	}

	@Override
	public void removeDataListener(DataListener listener) {
		dataListeners.remove(listener);
	}
	
	protected void sendData(Bundle data) {
		synchronized (dataListeners) {
			for (DataListener listener : dataListeners) {
				listener.onDataReceived(data);
			}
		}
	}
	
	
	
	/*****************************************
	 * Probe Status Listeners
	 *****************************************/
	// TODO: Figure out if we need status listeners, maybe reading state directly is enough
	// May need a State listener to know when probe stops itself?
	
	/**
	 * Interface implemented by Probe status observers.
	 */
	public interface StatusListener {

		/**
		 * Called when the probe emits a status message, which can happen when the probe changes state.
		 * @param status
		 */
		public void onStatusReceived(Bundle status);
	}
	
	private Set<StatusListener> statusListeners = Collections.synchronizedSet(new HashSet<StatusListener>());
	
	/**
	 * Returns the set of status listeners.  Make sure to synchronize on this object, 
	 * if you plan to modify it or iterate over it.
	 */
	protected Set<StatusListener> getStatusListeners() {
		return statusListeners;
	}
	@Override
	public void addStatusListener(StatusListener listener) {
		statusListeners.add(listener);
	}

	@Override
	public void removeStatusListener(StatusListener listener) {
		statusListeners.remove(listener);
	}

	
	
	
	/*****************************************
	 * Probe State Machine
	 *****************************************/
	
	private State state;
	
	/**
	 * Returns the current state of the probe.
	 */
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
	
	@Override
	public synchronized void enable() {
		ensureLooperThreadExists();
		handler.post(new Runnable() {
			@Override
			public void run() {
				state.enable(Probe.this);
			}
		});
	}

	@Override
	public synchronized void start() {
		ensureLooperThreadExists();
		handler.post(new Runnable() {
			@Override
			public void run() {
				state.start(Probe.this);
			}
		});
	}

	@Override
	public synchronized void stop() {
		ensureLooperThreadExists();
		handler.post(new Runnable() {
			@Override
			public void run() {
				state.stop(Probe.this);
			}
		});
	}

	@Override
	public synchronized void disable() {
		if (handler != null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					state.disable(Probe.this);
				}
			});
		}
	}
	

	/**
	 * Called when the probe switches from the disabled to the enabled state.  
	 * This is where any passive or opportunistic listeners should be configured.
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
	
	private class ProbeHandlerCallback implements Handler.Callback {

		@Override
		public boolean handleMessage(Message msg) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	/**
	 * Types to represent the current state of the probe.
	 * Provides the implementation of the ProbeRunnable state machine.
	 * 
	 * @author alangardner
	 *
	 */
	public enum State {

		// TODO: should we try catch, to prevent one probe from killing all probes?
		DISABLED {

			@Override
			protected void enable(Probe probe) {
				synchronized (probe) {
					probe.state = ENABLED;

			        
					probe.onEnable();
				}
			}
	
			@Override
			protected void start(Probe probe) {
				synchronized (probe) {
					enable(probe);
					if (probe.state == ENABLED) {
						ENABLED.start(probe);
					}
				}
			}
	
			@Override
			protected void stop(Probe probe) {
				// Nothing
			}
	
			@Override
			protected void disable(Probe probe) {
				// Nothing
			}
		},
		ENABLED {

			@Override
			protected void enable(Probe probe) {
				// Nothing
			}
	
			@Override
			protected void start(Probe probe) {
				synchronized (probe) {
					probe.state = RUNNING;
					probe.onStart();
				}
			}
	
			@Override
			protected void stop(Probe probe) {
				// Nothing
			}
	
			@Override
			protected void disable(Probe probe) {
				synchronized (probe) {
					probe.state = DISABLED;
					probe.onDisable();
					// Shutdown handler thread
					probe.looper.quit();
					probe.looper = null;
					probe.handler = null;
				}
			}
		},
		RUNNING {

			@Override
			protected void enable(Probe probe) {
				// Nothing
			}
	
			@Override
			protected void start(Probe probe) {
				// Nothing
			}
	
			@Override
			protected void stop(Probe probe) {
				synchronized (probe) {
					probe.state = ENABLED;
					probe.onStop();
				}
			}
	
			@Override
			protected void disable(Probe probe) {
				synchronized (probe) {
					stop(probe);
					if (probe.state == ENABLED) {
						ENABLED.disable(probe);
					}
				}
			}
		};
		
		protected abstract void enable(Probe probe);
		protected abstract void disable(Probe probe);
		protected abstract void start(Probe probe);
		protected abstract void stop(Probe probe);
		
	}
}
