package edu.mit.media.funf.action;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource.SchedulingAction;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

public class RegisterDurationAction extends Action implements SchedulingAction {
        
    @Configurable
    private int duration = 0;
    
    @Configurable
    private StartableDataSource delegate = null;
    
    RegisterDurationAction() {
    }

    public void setDelegate(StartableDataSource delegate) {
        this.delegate = delegate;
    }
    
    protected void execute() {
        if (delegate == null) 
            return;
        Log.d(LogUtil.TAG, "running probe action start");
        delegate.start();
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(LogUtil.TAG, "running probe action stop");
                delegate.stop();
            }
        }, TimeUtil.secondsToMillis(duration));
    }
}
