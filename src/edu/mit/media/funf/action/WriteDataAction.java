package edu.mit.media.funf.action;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.JsonElement;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.util.LogUtil;

public class WriteDataAction extends Action implements DataListener {

    @Configurable
    private SQLiteOpenHelper dbHelper = null;
        
    WriteDataAction() {
    }
    
    public WriteDataAction(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    protected void execute(String key, IJsonObject data) {
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
        final String key = probeConfig.get(RuntimeTypeAdapterFactory.TYPE).toString();
        final IJsonObject finalData = data;
        ensureHandlerExists();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                execute(key, finalData);
            }
        });
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        String key = probeConfig.get(RuntimeTypeAdapterFactory.TYPE).toString();
        Log.d(LogUtil.TAG, "finished writing probe data " + key);
        setHandler(null); // free system resources as data stream has completed.
    }
}
