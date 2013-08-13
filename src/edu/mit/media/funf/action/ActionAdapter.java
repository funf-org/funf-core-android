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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

import edu.mit.media.funf.action.Action.DataAcceptingAction;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

import android.os.Handler;

public class ActionAdapter implements DataListener {
    
    private Handler actionHandler;
    
    private List<Action> actions;
    
    ActionAdapter() {
        actions = new ArrayList<Action>();
    }
    
    ActionAdapter(Handler handler) {
        this();
        this.actionHandler = handler;
    }
    
    public Handler getHandler() {
        return actionHandler;
    }
    
    public void setHandler(Handler handler) {
        this.actionHandler = handler;
    }
    
    public void registerAction(Action action) {
        actions.add(action);
    }

    public void unregisterAction(Action action) {
        if (actions.contains(action)) {
            actions.remove(action);
        }
    }
    
    protected void executeActions(IJsonObject dataSourceConfig, IJsonObject data) {
        for (Action action: actions) {
            if (action instanceof DataAcceptingAction) {
                ((DataAcceptingAction)action).setDataSource(dataSourceConfig);
                ((DataAcceptingAction)action).setData(data);   
            }
            getHandler().post(action);
        }
    }
    
    @Override
    public void onDataReceived(IJsonObject dataSourceConfig, IJsonObject data) {
        executeActions(dataSourceConfig, data);
    }

    @Override
    public void onDataCompleted(IJsonObject dataSourceConfig, JsonElement checkpoint) {
        // TODO
    }

}
