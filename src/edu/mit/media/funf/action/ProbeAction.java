/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
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
package edu.mit.media.funf.action;

import java.util.Map;
import java.util.Map.Entry;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.trigger.Trigger;

public class ProbeAction extends Action {

    @Configurable
    private String action;
    
    @Configurable
    private Map<String, String> listeners;
    
    @Override
    protected void execute() {
        if (!("start".equals(action) || "stop".equals(action)))
            return;
        for (Entry<String, String> entry: listeners.entrySet()) {
            Probe probe = getPipeline().getProbeByLabel(entry.getKey());
            Trigger trigger = getPipeline().getTriggerByLabel(entry.getValue());
            if (probe != null && trigger != null && trigger instanceof DataListener) {
                if ("start".equals(action)) {
                    probe.registerListener((DataListener)trigger);
                } else if (probe instanceof ContinuousProbe) {
                    ((ContinuousProbe) probe).unregisterListener((DataListener)trigger);
                }
            }
        }
    }
}
