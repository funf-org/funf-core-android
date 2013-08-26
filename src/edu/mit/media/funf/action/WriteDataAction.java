package edu.mit.media.funf.action;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.util.LogUtil;

public class WriteDataAction extends Action implements DataListener {

    private SQLiteOpenHelper dbHelper = null;
    
    private String key = null;
    private IJsonObject data = null;
    
    private Looper looper = null;
    private Handler myHandler = null; // run this action on a dedicated thread
    
    public WriteDataAction(ActionGraph graph, SQLiteOpenHelper dbHelper) {
        super(graph);
        this.dbHelper = dbHelper;
    }
    
    protected void execute() {
        if (key == null || data == null)
            return;
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final double timestamp = data.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsDouble();
        final String value = data.toString();
        if (timestamp == 0L || key == null || value == null) {
            Log.e(LogUtil.TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + key + " - " + value);
            throw new SQLException("Not all required fields specified.");
        }
        ContentValues cv = new ContentValues();
        cv.put(NameValueDatabaseHelper.COLUMN_NAME, key);
        cv.put(NameValueDatabaseHelper.COLUMN_VALUE, value);
        cv.put(NameValueDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
        db.insertOrThrow(NameValueDatabaseHelper.DATA_TABLE.name, "", cv);
    }
    
    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        this.key = probeConfig.get(RuntimeTypeAdapterFactory.TYPE).toString();
        this.data = data;
        queueInHandler();
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        Log.d(LogUtil.TAG, "finished writing probe data " + key);
        exitMyHandler(); // free system resources as data stream has completed.
    }
    
    @Override
    public void queueInHandler() {
        ensureMyHandlerExists(); // run data write on a dedicated thread
        super.queueInHandler();
    }
    
    private void ensureMyHandlerExists() {
        if (looper == null) {
            synchronized (this) {
                if (looper == null) {
                    HandlerThread thread = new HandlerThread("Action[" + getClass().getName() + "]");
                    thread.start();
                    looper = thread.getLooper();
                    myHandler = new Handler(looper);
                    setHandler(myHandler);
                }
            }
        }
    }
    
    private void exitMyHandler() {
        if (looper != null) {
            synchronized (this) {
                if (looper != null) {
                    looper.quit();
                    looper = null;
                    myHandler = null;                    
                }
            }
        }   
    }

}
