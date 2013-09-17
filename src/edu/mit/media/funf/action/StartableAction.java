package edu.mit.media.funf.action;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.Startable;
import edu.mit.media.funf.datasource.Startable.TriggerAction;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

public class StartableAction extends Action implements TriggerAction {
        
    @Configurable
    private Double duration = null;
    
    @Configurable
    private Startable target = null;
    
    StartableAction() {
    }

    public void setDelegate(Startable target) {
        this.target = target;
    }
    
    protected void execute() {
        if (target == null) 
            return;
        Log.d(LogUtil.TAG, "running action start");
        target.start();
        if (duration != null && duration > 0.0) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(LogUtil.TAG, "running action stop");
                    target.stop();
                }
            }, TimeUtil.secondsToMillis(duration));   
        }
    }
    
    protected boolean isLongRunningAction() {
        return true;
    }
}
