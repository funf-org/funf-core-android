package edu.mit.media.funf.config;

import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.util.EqualsUtil;
import edu.mit.media.funf.util.LogUtil;

public abstract class ConfigUpdater {

  
  public void  run(String name, FunfManager mgr) {
    JsonObject oldConfig = mgr.getPipelineConfig(name);
    try {
      JsonObject newConfig = getConfig();
      if (!EqualsUtil.areEqual(oldConfig, newConfig)) {
        mgr.saveAndReload(name, newConfig);
      }
    } catch (ConfigUpdateException e) {
      Log.w(LogUtil.TAG, "Unable to get config", e);
    }
  }
  
  abstract protected JsonObject getConfig() throws ConfigUpdateException;
  
  
  
  

  public class ConfigUpdateException extends Exception {

    private static final long serialVersionUID = 7595505577357891121L;

    public ConfigUpdateException() {
      super();
    }

    public ConfigUpdateException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }

    public ConfigUpdateException(String detailMessage) {
      super(detailMessage);
    }

    public ConfigUpdateException(Throwable throwable) {
      super(throwable);
    }
    
  }
}
