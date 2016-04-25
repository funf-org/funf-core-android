package edu.mit.media.funf.config;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

/**
 * ConfigUpdater which does an Http get to the given url.
 *
 */
public class HttpConfigUpdater extends ConfigUpdater {

  @Configurable
  private String url;
  
  @Override
  public JsonObject getConfig() throws ConfigUpdateException {
    try {
      String content = null;
      String currentUrl = IOUtil.formatServerUrl(url, "");
      Log.i(LogUtil.TAG, "Config url: " + currentUrl);
      if (IOUtil.isValidUrl(currentUrl)) {
        content = IOUtil.httpGet(currentUrl, null, BasicPipeline.ACTION_UPDATE, FunfManager.funfManager.getAuthToken(url));
      }
      if (content == null) {
        throw new ConfigUpdateException("Unable to download configuration.");
      }
      return new JsonParser().parse(content).getAsJsonObject();
    } catch (JsonSyntaxException e) {
      throw new ConfigUpdateException("Bad json in configuration.", e);
    } catch (IllegalStateException e) {
      throw new ConfigUpdateException("Bad json in configuration.", e);
    }
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
  

}
