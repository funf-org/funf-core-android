package edu.mit.media.funf.action;

import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

public class ProbeAction extends Action {
    
    public static String REGISTER_LISTENER = "register",
            UNREGISTER_LISTENER = "unregister";
    
    private String listenerLabel;
    private String probeConfig;
    private String operation;
    
    public ProbeAction(ActionGraph graph, String probeConfig, String listenerLabel, String operation) {
        super(graph);
        this.listenerLabel = listenerLabel;
        this.probeConfig = probeConfig;
        this.operation = operation;
    }
    
    protected void execute() {
        Log.d(LogUtil.TAG, "running probe action " + operation);
        if (REGISTER_LISTENER.equals(operation)) {
            getGraph().registerProbeListener(listenerLabel, probeConfig);
        } else if (UNREGISTER_LISTENER.equals(operation)) {
            getGraph().unregisterProbeListener(listenerLabel, probeConfig);
        }
    }

}
