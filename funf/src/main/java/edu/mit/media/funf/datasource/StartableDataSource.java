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
