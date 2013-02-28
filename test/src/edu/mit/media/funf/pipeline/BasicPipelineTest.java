package edu.mit.media.funf.pipeline;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.AndroidTestCase;
import android.util.Log;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.tests.R;

public class BasicPipelineTest extends AndroidTestCase {

  private FunfManager mgr;
  private ServiceConnection conn = new ServiceConnection() {
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mgr = ((FunfManager.LocalBinder)service).getManager();
      synchronized (s) {
        s.notify();
      }
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO Auto-generated method stub
      
    }
    
  };
  
  private Object s = new Object();
  
  public void setUp() throws Exception {
    super.setUp();
    getContext().bindService(new Intent(getContext(), FunfManager.class), conn, Context.BIND_AUTO_CREATE);
    synchronized (s) {
      s.wait(5000L);
    }
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    getContext().unbindService(conn);
  }


  public void testAsdf() {
    String testPipelineConfig = getContext().getResources().getString(R.string.default_pipeline);
    Pipeline pipeline = mgr.getGson().fromJson(testPipelineConfig, Pipeline.class);
    Log.d("FunfTest", "PIPELINE: " + mgr.getGson().toJson(pipeline));
    pipeline.onCreate(mgr);
  }
}
