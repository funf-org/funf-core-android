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
    }
    
    public ProbabilisticFilter(DataListener listener) {
        this.listener = listener;
        long seed = System.currentTimeMillis();
        generator = new Random(seed);
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
