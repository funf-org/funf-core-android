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
package edu.mit.media.funf.filter;

import java.util.Random;

import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.util.LogUtil;

/**
 * Passes on data randomly, with a uniform probability equal to
 * the configured "probability" variable.
 * 
 * Filters can be registered as a DataListener to any data source,
 * for eg. probes or other filters. Filters listen for data from
 * the source, and if certain specified conditions are met, 
 * they forward the data to their own Data Listener.
 * 
 * For the ProbabilisticFilter, the "probability" variable
 * must be set to the required probability, in the range [0,1].
 * 
 * Values less than 0 will be considered as 0, and greater than 1
 * will be considered as 1.
 *
 */
public class ProbabilisticFilter implements DataListener, DataSource {

    /**
     * Probability with which the filter should forward data.
     * Values less than 0 will be considered as 0, and greater than 1
     * will be considered as 1.
     */
    @Configurable
    private double probability = 0.5; 
    
    @Configurable
    private DataListener listener;
        
    private Random generator;
    
    ProbabilisticFilter() {
        long seed = System.currentTimeMillis();
        generator = new Random(seed);
    }
    
    public ProbabilisticFilter(DataListener listener) {
        this();
        this.listener = listener;
    }

    @Override
    public void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        double random = generator.nextDouble();
        Log.d(LogUtil.TAG, "generated probability: " + random);
        if (random < probability) {
            listener.onDataReceived(dataSourceConfig, data);
        }
    }

    @Override
    public void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        listener.onDataCompleted(dataSourceConfig, checkpoint);
    }

    @Override
    public void setListener(DataListener listener) {
        this.listener = listener;
    }

}
