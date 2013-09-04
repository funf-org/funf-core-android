package edu.mit.media.funf.action;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource.SchedulingAction;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

public class StopDataSourceAction extends Action implements SchedulingAction {
    
    @Configurable
    private StartableDataSource delegate = null;
    
    StopDataSourceAction() {
    }

    public void setDelegate(StartableDataSource delegate) {
        this.delegate = delegate;
    }
    
    protected void execute() {
        if (delegate == null) 
            return;
        Log.d(LogUtil.TAG, "running probe action stop");
        delegate.stop();
    }
}
