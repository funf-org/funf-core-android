package edu.mit.media.funf.probe.builtin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.Base;

/**
 * A probe that runs once returns data and then automatically unregisters its probes when the data stream is done.
 * While this probe is running, other DataListeners that register will be queued up for the next run, which will be when the current run stops.
 * 
 * Extending this class is useful when your probe returns a data stream which has an end, like with ContentProvider probes.
 */
public abstract class ImpulseProbe extends Base {

	private Set<DataListener> queuedListeners = Collections.synchronizedSet(new HashSet<DataListener>());
	protected Set<DataListener> getQueuedDataListeners() {
		return queuedListeners;
	}
	
	private StateListener listenerQueueProcessor = new StateListener() {
		@Override
		public void onStateChanged(Probe probe) {
			if (getState() != State.RUNNING) {
				unregisterListener((DataListener[])getDataListeners().toArray());
				synchronized (queuedListeners) {
					registerListener((DataListener[])queuedListeners.toArray());
					queuedListeners.clear();
				}
			}
		}
	};
	
	public ImpulseProbe() {
		addStateListener(listenerQueueProcessor);
	}
	
	
	
	@Override
	public void registerListener(DataListener... listeners) {
		// TODO: may want to introduce a small delay in running the probe, in case register listener is called multiple times in a row
		if (getState() ==  State.RUNNING) {
			synchronized (queuedListeners) {
				Set<DataListener> currentListeners = getDataListeners();
				for (DataListener listener : listeners) {
					if (!currentListeners.contains(listener)) {
						queuedListeners.add(listener);
					}
				}
			}
		} else {
			super.registerListener(listeners);
		}
	}

	@Override
	public void unregisterListener(DataListener... listeners) {
		synchronized (queuedListeners) {
			for (DataListener listener : listeners) {
				queuedListeners.remove(listener);
			}
		}
		super.unregisterListener(listeners);
	}
	
}
