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

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class ProbabilisticFilter implements DataListener {

    @Configurable
    private Double probability = 0.5; // should be between 0.0 and 1.0
    
    @Configurable
    private Long seed = null;
    
    private final DataListener listener;
    
    private Random generator;
    
    ProbabilisticFilter(DataListener listener) {
        this.listener = listener;
        if (seed == null)
            seed = System.currentTimeMillis();
        generator = new Random(seed);
    }

    @Override
    public void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        Double random = generator.nextDouble();
        if (random < probability) {
            listener.onDataReceived(dataSourceConfig, data);
        }
    }

    @Override
    public void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        listener.onDataCompleted(dataSourceConfig, checkpoint);
    }

}
