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

import java.lang.Runnable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.pipeline.TriggerActionPipeline;
import edu.mit.media.funf.trigger.Trigger;

public class Action implements Runnable {

    @Configurable
    private String label;
    
    @Configurable
    private Set<String> triggers;
    
    private TriggerActionPipeline pipeline;
    
    Action() {
    }
    
    public void setPipeline(TriggerActionPipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    protected TriggerActionPipeline getPipeline() {
        return pipeline;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Set<String> getTriggerLabels() {
        return triggers;
    }
    
    @Override
    public final void run() {
        execute();
        activateTriggers();
    }
    
    /**
     * Override this function to include the action-specific code.
     */
    protected void execute() {
     // Perform action here
    }
    
    /*****************************************
     * Registered Triggers
     *****************************************/
    private Set<Trigger> registeredTriggers = Collections.synchronizedSet(new HashSet<Trigger>());

    /**
     * Returns the set of registered actions. Make sure to synchronize on this
     * object, if you plan to modify it or iterate over it.
     */
    public Set<Trigger> getRegisteredTriggers() {
        return registeredTriggers;
    }

    public void registerTrigger(Trigger... triggers) {
        if (triggers != null) {
            for (Trigger trigger : triggers) {
                registeredTriggers.add(trigger);
            }
        }
    }

    public void unregisterTrigger(Trigger... triggers) {
        if (triggers != null) {
            for (Trigger trigger : triggers) {
                registeredTriggers.remove(trigger);
            }
        }
    }

    public void unregisterAllTriggers() {
        Trigger[] triggers = null;
        synchronized (registeredTriggers) {
            triggers = new Trigger[registeredTriggers.size()];
            registeredTriggers.toArray(triggers);
        }
        unregisterTrigger(triggers);
    }

    protected void activateTriggers() {
        for (Trigger trigger : registeredTriggers) {
            trigger.start();
        }
    }
}
