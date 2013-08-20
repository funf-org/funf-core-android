package edu.mit.media.funf.action;

import android.os.Handler;
import edu.mit.media.funf.probe.Probe.DataListener;

public interface ActionGraph {
    
    public void addListenerByLabel(String label, DataListener listener);
    public void removeListenerByLabel(String label);
    
    public void registerProbeListener(String listenerLabel, String probe);
    public void unregisterProbeListener(String listenerLabel, String probe);

    public Handler getHandler();
}
