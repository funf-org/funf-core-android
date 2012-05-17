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
		public void onStateChanged(Probe probe, State previousState) {
			if (previousState == State.RUNNING && getState() != State.RUNNING) {
				Set<DataListener> listeners = getDataListeners();
				if (!listeners.isEmpty()) {
					DataListener[] listenerArray = new DataListener[listeners.size()];
					listeners.toArray(listenerArray);
					unregisterListener(listenerArray);
				}
				synchronized (queuedListeners) {
					DataListener[] queuedListenerArray = new DataListener[queuedListeners.size()];
					queuedListeners.toArray(queuedListenerArray);
					registerListener(queuedListenerArray);
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
		if (listeners != null) {
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
	}

	@Override
	public void unregisterListener(DataListener... listeners) {
		if (listeners != null) {
			synchronized (queuedListeners) {
				for (DataListener listener : listeners) {
					queuedListeners.remove(listener);
				}
			}
		}
		super.unregisterListener(listeners);
	}
	
}
