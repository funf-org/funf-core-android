package edu.mit.media.funf.action;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.util.LogUtil;

import android.util.Log;

public class RunUpdateAction extends Action {
    
    @Configurable
    private String name = null;
    
    @Configurable
    private FunfManager manager = null;
    
    @Configurable
    protected ConfigUpdater update = null;
    
    RunUpdateAction() {
    }
    
    public RunUpdateAction(String name, FunfManager manager, ConfigUpdater update) {
        this.name = name;
        this.manager = manager;
        this.update = update;
    }
    
    protected void execute() {
        if (name != null && manager != null && update != null) {
            Log.d(LogUtil.TAG, "running update");
            update.run(name, manager);
        } else {
            Log.d(LogUtil.TAG, "update failed");
        }
        setHandler(null); // free system resources
    }
    
    protected boolean isLongRunningAction() {
        return true;
    }
}
