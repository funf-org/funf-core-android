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
	

	protected static final int EXTERNAL_MESSAGE = 0;
	protected static final int INTERNAL_MESSAGE = 1;
	protected static final int MESSAGE_QUIT = 2;
	protected static final int MESSAGE_PAUSE = 3;

    private static final long DEFAULT_MILLIS_TO_WAIT = 5000L;
    
    // Times since bootup to controll order at front of que, 
    // implemented as a hack because putting message at front of queue does not work as expected
    private static final long 
    PRIORITY_BEFORE_FRONT = 1,
    PRIORITY_FRONT = 2,
    PRIORITY_AFTER_FRONT = 3;
    
    
	private int startId = 0;
	private volatile HandlerThread thread;
	private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;

    private Intent intentToWaitFor;

    
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
    		switch (msg.what) {
			case MESSAGE_QUIT:
        		mServiceLooper.quit();
				break;
			case MESSAGE_PAUSE:
				Long millisToWait = (Long)msg.obj;
				if (millisToWait == null) {
					millisToWait = DEFAULT_MILLIS_TO_WAIT;
				}
				try {
					synchronized (this) {
						this.wait(millisToWait);
					}
				} catch (InterruptedException e) {
					Log.w(TAG, "Service handler thread interrupted!");
				}
				break;
			default:
            	Log.d(TAG, "Handling msg " + msg.arg1);
        		Log.d(TAG, "Handling message @ " + System.currentTimeMillis() +": " + msg.obj);
	            onHandleIntent((Intent)msg.obj);
	            
	            if (!hasMessages()) {
	            	onEndOfQueue();
	            }
				break;
			}
        }
        

        private boolean hasMessages() {
        	// TODO: this may be to intensive to run on every message, consider a better implementation
        	//startId == msg.arg1  
        	return mServiceHandler.hasMessages(EXTERNAL_MESSAGE) 
        	|| mServiceHandler.hasMessages(INTERNAL_MESSAGE) 
        	||  mServiceHandler.hasMessages(MESSAGE_PAUSE) 
        	|| mServiceHandler.hasMessages(MESSAGE_QUIT);
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
    
    
    protected void pauseQueueUntilIntentReceived(Intent intent, Long timeout) {
    	if (!mServiceHandler.hasMessages(MESSAGE_QUIT)) {
	    	intentToWaitFor = (intent == null) ? new Intent() : intent;
			Message msg = mServiceHandler.obtainMessage();
			msg.what = MESSAGE_PAUSE;
			mServiceHandler.sendMessageAtTime(msg, PRIORITY_BEFORE_FRONT);  // Very front of queue
    	}
    }
    
    protected boolean queueIntent(Intent intent) {
    	return queueIntent(intent, false);
    }
    
    protected boolean queueIntent(Intent intent, boolean atFront) {
    	Message msg = mServiceHandler.obtainMessage();
    	msg.what = INTERNAL_MESSAGE;
        msg.arg1 = new Random().nextInt();
        msg.obj = intent;
        Log.d(TAG, "Internal Queue Message: "+ ((intent == null) ? "<quit>" : (intent.getComponent() + " " + intent.getAction())));
        if (atFront) {
        	return mServiceHandler.sendMessageAtTime(msg, PRIORITY_FRONT);  // HACK: because of the implementation of postMessageAtFrontOfQueue = sendMessageAtTime(msg, 0L)
        } else {
        	this.startId = msg.arg1; // TODO: figure out how to create priority queue, so we know how to specify start id
        	return mServiceHandler.sendMessage(msg);
        }
    }

    private boolean isIntentThisIsWaitingFor(Intent intent) {
    	return intentToWaitFor != null
    			&& ((intentToWaitFor.getComponent() == null && intentToWaitFor.getAction() == null)
    					|| intentToWaitFor.filterEquals(intent));
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.obj = intent;
        msg.what = EXTERNAL_MESSAGE;
        msg.arg1 = startId;
        
        // Awaken handler thread if this is the intent we are waiting for
    	if (isIntentThisIsWaitingFor(intent)) {
    		Log.d(TAG, "GOT intent we were waiting for: " + intent.getComponent() + " " + intent.getAction());
			intentToWaitFor = null;
    		boolean success = mServiceHandler.sendMessageAtTime(msg, PRIORITY_BEFORE_FRONT);
    		Log.d(TAG, "Successfully queued at front the intent we were waiting for. " + success);
			mServiceHandler.removeMessages(MESSAGE_PAUSE);
	    	synchronized (mServiceHandler) {
				mServiceHandler.notify();
			}
    	} else {
            this.startId = msg.arg1;
        	mServiceHandler.sendMessage(msg);
    	}

        Log.d(TAG, "onStart Message: " + intent.getComponent() + " " + intent.getAction());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return getRedeliveryType(intent);
    }

    @Override
    public void onDestroy() {
    	Log.d(TAG, "Destroying service " + getClass().getName());
    	
    	// Pause long enough to allow subclasses to queue up things before QUIT message
    	pauseQueueUntilIntentReceived(new Intent("NON_EXISTENT_INTENT"), 100L);
    	
    	// Send quit message at front of queue
		Message msg = mServiceHandler.obtainMessage();
		msg.what = MESSAGE_QUIT;
		mServiceHandler.sendMessageAtTime(msg, PRIORITY_AFTER_FRONT); // So that it occurs after all messages "At front of queue"
		
		onBeforeDestroy();
		
		mServiceHandler.removeMessages(MESSAGE_PAUSE);
    	synchronized (mServiceHandler) {
			mServiceHandler.notify();
		}
		
    	// Wait for queue to finish
    	try {
			thread.join(2000);
		} catch (InterruptedException e) {
		}
		if (thread.isAlive()) {
			Log.d(TAG, "Message thread did not die in time: " + getClass().getName());
			mServiceLooper.quit();
		}
    }
    
    /**
     * Allows the subclass to queue up messages on message thread before the service is destroyed.
     * Useful for must have cleanup options.
     */
    public void onBeforeDestroy() {
    	// Default implementation does nothing
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    protected boolean isIntentHandlerThread() {
    	Looper looper = Looper.myLooper();
    	return looper != null && looper.equals(mServiceLooper);
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
	  * Called when service has reached the end of the queue.  The default implementation calls stopSelf().
	  * Subclasses can override this to prevent class from stopping, or to perform cleanup on handler thread before onDestory.
	  * @return
	  */
    protected void onEndOfQueue() {
    	stopSelf();
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
