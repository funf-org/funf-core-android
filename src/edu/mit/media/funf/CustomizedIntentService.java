package edu.mit.media.funf;

import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public abstract class CustomizedIntentService extends Service {
	
	private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            if (shouldStop()) {
            	stopSelf(msg.arg1);
            }
        }
    }

    public CustomizedIntentService() {
    	this(null);
    }
    
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public CustomizedIntentService(String name) {
        super();
        mName = (name == null) ? getClass().getName() : name;
    }

    @Override
    public void onCreate() {
    	super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    
    protected void queueIntent(Intent intent) {
    	Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = new Random().nextInt();
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return getRedeliveryType(intent);
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    protected boolean isIntentHandlerThread() {
    	return Looper.myLooper().equals(mServiceLooper);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);
    
    /**
     * Hook to determine if service should attempt to be stopped after this message is completed.  
     * This will not stop the service if there are still more messages in the queue to be processed.
     * Default is true;
     * @return
     */
    protected boolean shouldStop() {
    	return true;
    }
    
    /**
     * Returns the redelivery type based on the intent passed in.  Subclasses can use this method to determine which intents get redilvered, and which do not.
     * Default is redelivery.
     * @param intent
     * @return
     */
    protected int getRedeliveryType(Intent intent) {
    	return START_REDELIVER_INTENT;
    }

}
