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
package edu.mit.media.funf.datasource;

import com.google.gson.JsonElement;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class StartableDataSource implements Startable, DataSource {
    
    public static enum State {
        OFF,
        ON
    };

    protected State currentState = State.OFF;

    protected DataListener outputListener;

    protected DataListener delegator = new DataListener() {

        @Override
        public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
            if (outputListener != null) {
                outputListener.onDataReceived(probeConfig, data);
            }
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig,
                JsonElement checkpoint) {
            if (outputListener != null) {
                outputListener.onDataCompleted(probeConfig, checkpoint);
            }
        }
    };
    
    public DataListener getDelegator() {
        return delegator;
    }

    @Override
    public final void start() {
        if (currentState == State.ON) 
            return;
        currentState = State.ON;
        onStart();
    }

    @Override
    public final void stop() {
        if (currentState == State.OFF) 
            return;
        currentState = State.OFF;
        onStop();
    }

    @Override
    public void setListener(DataListener listener) {
        this.outputListener = listener;
    }

    protected void onStart() {

    }

    protected void onStop() {

    }
}
