/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
