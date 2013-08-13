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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class Filter implements DataListener {

    private Set<DataListener> dataListeners = Collections.synchronizedSet(new HashSet<DataListener>());

    /**
     * Returns the set of data listeners. Make sure to synchronize on this
     * object, if you plan to modify it or iterate over it.
     */
    protected Set<DataListener> getDataListeners() {
        return dataListeners;
    }

    public void registerListener(DataListener... listeners) {
        if (listeners != null) {
            for (DataListener listener : listeners) {
                dataListeners.add(listener);
            }
        }
    }

    public void unregisterListener(DataListener... listeners) {
        if (listeners != null) {
            for (DataListener listener : listeners) {
                dataListeners.remove(listener);
            }
        }
    }

    protected void unregisterAllListeners() {
        DataListener[] listeners = null;
        synchronized (dataListeners) {
            listeners = new DataListener[dataListeners.size()];
            dataListeners.toArray(listeners);
        }
        unregisterListener(listeners);
    }

    protected void sendData(IJsonObject dataSourceConfig, IJsonObject data) {
        if (data == null) {
            return;
        } else {
            synchronized (dataListeners) {
                for (DataListener listener : dataListeners) {
                    listener.onDataReceived(dataSourceConfig, data);
                }
            }
        }
    }
    
    /**
     * Add filtering code in this function. Call sendData in this function to
     * pass data to the registered listeners of this filter.
     * 
     * @param dataSourceConfig
     * @param data
     */
    protected void filterData(IJsonObject dataSourceConfig, IJsonObject data) {
        
    }

    @Override
    public final void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        filterData(dataSourceConfig, data);
    }

    @Override
    public final void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        // TODO Auto-generated method stub
    }

}
