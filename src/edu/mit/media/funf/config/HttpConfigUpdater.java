package edu.mit.media.funf.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.util.IOUtil;

public class HttpConfigUpdater extends ConfigUpdater {

  @Configurable
  private String url;
  
  @Override
  protected JsonObject getConfig() {
    String content = IOUtil.httpGet(url, null);
    return new JsonParser().parse(content).getAsJsonObject();
  }

}
