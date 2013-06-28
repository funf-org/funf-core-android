/**
 * 
 * Funf: Open Sensing Framework Copyright (C) 2013 Alan Gardner
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Funf. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.pipeline;

import java.io.File;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;

public class HighBandwidthPipeline extends BasicPipeline implements Pipeline, DataListener {

  protected final int LARGE = 4;

  private Looper largeLooper;
  private Handler largeHandler;
  private Handler.Callback largeCallback = new Handler.Callback() {

    @Override
    public boolean handleMessage(Message msg) {
      onBeforeRun(msg.what, (JsonObject)msg.obj);
      switch (msg.what) {
        case LARGE:
          writeLargeData((String)msg.obj);
          break;
        default:
          break;
      }
      onAfterRun(msg.what, (JsonObject)msg.obj);
      return false;
    }
  };

  protected void writeLargeData(String filename) {
    File largeFile = new File(filename);
    if (archive != null && archive.add(largeFile)) {
      largeFile.delete();
    }
  }

  @Override
  public void onCreate(FunfManager manager) {
    super.onCreate(manager);
    HandlerThread thread = new HandlerThread(getClass().getName());
    thread.start();
    this.largeLooper = thread.getLooper();
    this.largeHandler = new Handler(largeLooper, largeCallback);
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    largeLooper.quit();
  }
  
  @Override
  public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
    super.onDataReceived(probeConfig, data);

    // check for reference to high bandwidth data
    if (data.get("filename") != null) {
      String filename = data.get("filename").getAsString();
      largeHandler.obtainMessage(LARGE, filename);
    }   
  }
}
