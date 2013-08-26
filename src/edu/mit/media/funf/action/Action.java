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

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.time.TimeUtil;

import android.os.Handler;
import android.os.Looper;

public class Action implements Runnable {
        
    /**
     *  Delay in seconds after which this action should be run.
     *  
     *  Use this for short delays, for timing longer delays, consider
     *  using AlarmProbe.
     */
    @Configurable
    private int delay = 0; 
    
    private ActionGraph graph;
    private Handler handler;
    
    Action() {
    }
    
    public Action(ActionGraph graph) {
        this();
        this.graph = graph;
        this.handler = graph.getHandler();
    }
    
    protected ActionGraph getGraph() {
        return graph;
    }
    
    public Handler getHandler() {
        return handler;
    }
    
    public void setHandler(Handler handler) {
        this.handler = handler;
    }
    
    public int getDelay() {
        return delay;
    }
    
    public void setDelay(int delay) {
        this.delay = delay;
    }
    
    @Override
    public final void run() {
        if (Looper.myLooper() != getHandler().getLooper()) {
            getHandler().post(this);
            return;
        }
        execute();
    }
    
    public void queueInHandler() {
        if (delay > 0) {
            getHandler().postDelayed(this, TimeUtil.secondsToMillis(delay));   
        } else {
            getHandler().post(this);
        }
    }
    
    /**
     * Override this function to include the action-specific code.
     */
    protected void execute() {
        // Perform action here
    }
    
}
