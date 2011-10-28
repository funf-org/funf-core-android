package edu.mit.media.funf;

import static edu.mit.media.funf.Utils.TAG;
import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class CustomizedIntentService extends Service {
	
	private int startId = 0;
	private volatile HandlerThread thread;
	private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        	Log.d(TAG, "Handling msg " + msg.arg1);
        	if (msg.obj == null) {
        		mServiceLooper.quit();
        	} else {
        		Log.d(TAG, "Handling message @ " + System.currentTimeMillis() +": " + msg.obj);
            	mServiceHandler.removeMessages(WAIT_MESSAGE);
	            onHandleIntent((Intent)msg.obj);
	            if (shouldStop() && startId == msg.arg1) {
	            	stopSelf();
	            }
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
        thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    

    public static final int WAIT_MESSAGE = 1;
    /**
     * Used to keep service alive while waiting for external intents.
     * @param delayMillis
     */
    protected void waitForIntent(long delayMillis) {
    	Log.d(TAG, "Waiting until..." + System.currentTimeMillis() + delayMillis);
		Message msg = mServiceHandler.obtainMessage();
		msg.what = WAIT_MESSAGE;
		mServiceHandler.sendMessageDelayed(msg, delayMillis);
	}
    protected void waitForIntent() {
		waitForIntent(1000L);
	}
    
    public static final int INTERNAL_MESSAGE = 0;
    protected void queueIntent(Intent intent) {
    	Message msg = mServiceHandler.obtainMessage();
    	msg.what = INTERNAL_MESSAGE;
        this.startId = msg.arg1 = new Random().nextInt();
        msg.obj = intent;
        boolean success = mServiceHandler.sendMessage(msg);
        Log.d(TAG, "Message: "+ ((intent == null) ? "<quit>" : (intent.getComponent() + " " + intent.getAction())));
        Log.d(TAG, "Queued message "  + msg.arg1 + "? " + success);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        this.startId = msg.arg1 = startId;
        msg.obj = intent;
        boolean success = mServiceHandler.sendMessage(msg);
        Log.d(TAG, "onStart queued message "  + msg.arg1 + "? " + success);
        Log.d(TAG, "Message: " + intent.getComponent() + " " + intent.getAction());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return getRedeliveryType(intent);
    }

    @Override
    public void onDestroy() {
    	Log.d(TAG, "Destroying service " + getClass().getName());
    	queueIntent(null); // Send quit message at end of current queue

    	// Wait for queue to finish
    	try {
			thread.join(1000);
		} catch (InterruptedException e) {
		}
		if (thread.isAlive()) {
			Log.d(TAG, "Message thread did not die in time: " + getClass().getName());
			mServiceLooper.quit();
		}
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
