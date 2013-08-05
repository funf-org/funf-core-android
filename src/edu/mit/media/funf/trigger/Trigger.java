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
package edu.mit.media.funf.trigger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.BundleTypeAdapter;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.TriggerActionPipeline;

public class Trigger {

    @Configurable
    protected String label = "base";
    
    @Configurable
    protected Set<String> actions;
    
    private TriggerActionPipeline pipeline;
    
    private Handler actionHandler;

    /**
     * No argument constructor requires that setPipeline be called manually.
     */
    public Trigger() {
        state = TriggerState.DISABLED;
    }

    public Trigger(TriggerActionPipeline pipeline, Handler handler) {
        this();
        this.pipeline = pipeline;
        this.actionHandler = handler;
    }
    
    private Gson gson;

    protected Gson getGson() {
        if (gson == null) {
            gson = getGsonBuilder().create();
        }
        return gson;
    }

    protected GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(getPipeline().getFunfManager().getTriggerFactory());
        TypeAdapterFactory adapterFactory = getSerializationFactory();
        if (adapterFactory != null) {
            builder.registerTypeAdapterFactory(adapterFactory);
        }
        builder.registerTypeAdapterFactory(BundleTypeAdapter.FACTORY);
        return builder;
    }

    private IJsonObject config;
    public IJsonObject getConfig() {
        if (config == null) {
            config = new IJsonObject(getGson().toJsonTree(this).getAsJsonObject());
        }
        return config;
    }

    protected TriggerActionPipeline getPipeline() {
        return pipeline;
    }
    
    public void setPipeline(TriggerActionPipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    protected Handler getActionHandler() {
        return actionHandler;
    }
    
    public void setActionHandler(Handler handler) {
        this.actionHandler = handler;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Set<String> getActionLabels() {
        return actions;
    }

    /*****************************************
     * Registered Actions
     *****************************************/
    private Set<Action> registeredActions = Collections.synchronizedSet(new HashSet<Action>());

    /**
     * Returns the set of registered actions. Make sure to synchronize on this
     * object, if you plan to modify it or iterate over it.
     */
    public Set<Action> getRegisteredActions() {
        return registeredActions;
    }

    public void registerAction(Action... actions) {
        if (actions != null) {
            for (Action action : actions) {
                registeredActions.add(action);
            }
        }
    }

    public void unregisterAction(Action... actions) {
        if (actions != null) {
            for (Action action : actions) {
                registeredActions.remove(action);
            }
            // If no action is registered, stop using device resources
            if (registeredActions.isEmpty()) {
                disable();
            }
        }
    }

    public void unregisterAllActions() {
        Action[] actions = null;
        synchronized (registeredActions) {
            actions = new Action[registeredActions.size()];
            registeredActions.toArray(actions);
        }
        unregisterAction(actions);
    }

    protected void runActions() {
        // execute actions
        for (Action action : registeredActions) {
            actionHandler.post(action);
        }
    }

    /*****************************************
     * Trigger State Machine
     *****************************************/

    public static enum TriggerState {

        DISABLED {

            @Override
            protected void enable(Trigger trigger) {
                synchronized (trigger) {
                    trigger.state = ENABLED;
                    trigger.onEnable();
                }
            }

            @Override
            protected void trigger(Trigger trigger) {
                // Nothing
            }

            @Override
            protected void cancel(Trigger trigger) {
                // Nothing
            }

            @Override
            protected void disable(Trigger trigger) {
                // Nothing
            }
        },
        ENABLED {

            @Override
            protected void enable(Trigger trigger) {
                // Nothing
            }

            @Override
            protected void trigger(Trigger trigger) {
                synchronized (trigger) {
                    trigger.state = TRIGGERED;
                    trigger.runActions();
                    trigger.onTrigger();
                    trigger.state = ENABLED;
                }
            }

            @Override
            protected void cancel(Trigger trigger) {
                // Nothing
            }

            @Override
            protected void disable(Trigger trigger) {
                synchronized (trigger) {
                    trigger.state = DISABLED;
                    trigger.onDisable();
                    trigger.registeredActions.clear();
                    // Shutdown handler thread
                    trigger.looper.quit();
                    trigger.looper = null;
                    trigger.handler = null;
                }
            }
        },
        TRIGGERED {

            @Override
            protected void enable(Trigger trigger) {
                // Nothing
            }

            @Override
            protected void trigger(Trigger trigger) {
                // Nothing - Ignore cascading triggers
            }

            @Override
            protected void cancel(Trigger trigger) {
                trigger.state = ENABLED;
                trigger.onCancel();
            }

            @Override
            protected void disable(Trigger trigger) {
                synchronized (trigger) {
                    cancel(trigger);
                    if (trigger.state == ENABLED) {
                        ENABLED.disable(trigger);
                    }
                }
            }
        };

        protected abstract void enable(Trigger trigger);

        protected abstract void trigger(Trigger trigger);

        protected abstract void cancel(Trigger trigger);

        protected abstract void disable(Trigger trigger);

    }

    private TriggerState state;

    public TriggerState getState() {
        return state;
    }

    private void ensureLooperThreadExists() {
        if (looper == null) {
            synchronized (this) {
                if (looper == null) {
                    HandlerThread thread = new HandlerThread("Trigger[" + getClass().getName() + "]");
                    thread.start();
                    looper = thread.getLooper();
                    handler = new Handler(looper, new TriggerHandlerCallback());
                }
            }
        }
    }

    protected final void enable() {
        ensureLooperThreadExists();
        handler.sendMessage(handler.obtainMessage(ENABLE_MESSAGE));
    }

    protected final void trigger() {
        ensureLooperThreadExists();
        handler.sendMessage(handler.obtainMessage(TRIGGER_MESSAGE));
    }

    protected final void cancel() {
        ensureLooperThreadExists();
        handler.sendMessage(handler.obtainMessage(CANCEL_MESSAGE));
    }

    protected final void disable() {
        if (handler != null) {
            handler.sendMessage(handler.obtainMessage(DISABLE_MESSAGE));
        }
    }
    
    public void start() {
        enable();
    }

    public void destroy() {
        disable();
    }

    /**
     * Called when the trigger switches from the disabled to the enabled
     * state. This is where you should setup the trigger to be activated
     * on any desired event.
     */
    protected void onEnable() {

    }

    /**
     * Called when the trigger is activated due to the associated event.
     * All Actions registered with this trigger are executed before this
     * function is called. This should be used only to reset the trigger,
     * for eg in case of recurring triggers. All actions to be performed
     * in response to this trigger must be specified via Action objects 
     * and registered with this trigger.   
     */
    protected void onTrigger() {

    }

    /**
     * Called when the trigger is cancelled while in the middle of performing
     * the registered actions. This should be used to perform any error
     * checking and reset the trigger if needed.
     */
    protected void onCancel() {

    }

    /**
     * Called when the trigger switches from the enabled state to the disabled
     * state. This is the time to cleanup and release any resources before 
     * the trigger is destroyed.
     */
    protected void onDisable() {

    }

    private volatile Looper looper;
    private volatile Handler handler;

    /**
     * Access to the trigger thread's handler.
     * 
     * @return
     */
    protected Handler getHandler() {
        return handler;
    }

    /**
     * @param msg
     * @return
     */
    protected boolean handleMessage(Message msg) {
        // For right now don't handle any messages, only runnables
        return false;
    }

    protected static final int ENABLE_MESSAGE = 1, TRIGGER_MESSAGE = 2, CANCEL_MESSAGE = 3, 
            DISABLE_MESSAGE = 4;

    private class TriggerHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case ENABLE_MESSAGE:
                state.enable(Trigger.this);
                break;
            case TRIGGER_MESSAGE:
                state.trigger(Trigger.this);
            case CANCEL_MESSAGE:
                state.cancel(Trigger.this);
            case DISABLE_MESSAGE:
                state.disable(Trigger.this);
                break;
            default:
                return Trigger.this.handleMessage(msg);
            }
            return true; // Message was handled
        }

    }

    /**********************************
     * Custom serialization
     ********************************/

    /**
     * Used to override the serialiazation technique for multiple types.
     * You can override this method to have getGson() return a Gson object that includes your TypeAdapterFactory.
     * 
     * @return
     */
    protected TypeAdapterFactory getSerializationFactory() {
        return null;
    }
}
