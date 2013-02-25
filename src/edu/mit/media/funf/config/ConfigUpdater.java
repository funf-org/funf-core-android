package edu.mit.media.funf.config;

import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.util.EqualsUtil;

public abstract class ConfigUpdater {

  
  
  public void  run(String name, FunfManager mgr) {
    JsonObject oldConfig = mgr.getPipelineConfig(name);
    JsonObject newConfig = getConfig();
    if (!EqualsUtil.areEqual(oldConfig, newConfig)) {
      mgr.saveAndReload(name, newConfig);
    }
  }
  
  abstract protected JsonObject getConfig();
}
