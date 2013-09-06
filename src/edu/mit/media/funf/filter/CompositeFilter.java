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

import java.util.List;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

/**
 * Provides functionality of nested filter objects.
 * 
 * The "filters" member must point to a list of filter classes
 * which implement both DataListener and DataSource interfaces.
 * 
 * This filter will create a chain of filters in their order of
 * appearance in the "filters" list.
 *
 */
public class CompositeFilter implements DataListener, DataSource {
    
    @Configurable
    private DataListener listener = null;
    
    @Configurable
    private List<DataListener> filters = null;
    
    private DataListener headFilter = null;
    
    CompositeFilter() {
    }
    
    private void ensureEnabled() {
        if (headFilter == null) {
            if (filters == null) {
                headFilter = listener;
                return;
            }
            
            boolean isFirst = true;
            DataListener nextFilter = null;
            for (DataListener filter: filters) {
                if (isFirst) {
                    headFilter = filter;
                    nextFilter = headFilter;
                    isFirst = false;
                } else {
                    if (nextFilter instanceof DataSource) {
                        ((DataSource)nextFilter).setListener(filter);
                        nextFilter = filter;
                    }
                }
            }
            if (nextFilter instanceof DataSource) {
                ((DataSource)nextFilter).setListener(listener);
            }
        }
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        ensureEnabled();
        headFilter.onDataReceived(probeConfig, data);
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        ensureEnabled();
        headFilter.onDataCompleted(probeConfig, checkpoint);
    }

    @Override
    public void setListener(DataListener listener) {
        this.listener = listener;
        
    }

}
