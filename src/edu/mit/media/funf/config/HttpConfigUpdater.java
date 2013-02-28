package edu.mit.media.funf.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.util.IOUtil;

/**
 * ConfigUpdater which does an Http get to the given url.
 *
 */
public class HttpConfigUpdater extends ConfigUpdater {

  @Configurable
  private String url;
  
  @Override
  protected JsonObject getConfig() {
    String content = IOUtil.httpGet(url, null);
    return new JsonParser().parse(content).getAsJsonObject();
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
  

}
