package edu.mit.media.funf.storage;

import android.content.ContentValues;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimeZone;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created by astopczynski on 10/26/15.
 */
public class JsonDatabaseHelper implements DatabaseHelper{

    public static final String COLUMN_TIME_OFFSET = "time_offset";


    Context context;
    String databaseName;
    static FileOutputStream fos = null;

    public JsonDatabaseHelper(Context context, String name, int version) {
        this.context = context;
        this.databaseName = name;
    }

    public void init() {
        this.createDatabaseFile();
    }

    private void createDatabaseFile() {
        try {
            fos = this.context.openFileOutput(this.databaseName, Context.MODE_WORLD_READABLE);
            JsonObject dataObject = new JsonObject();
            dataObject.addProperty("ANDROID_ID",
                    Settings.Secure.getString(
                            this.context.getContentResolver(), Settings.Secure.ANDROID_ID
                    ));
            dataObject.addProperty(this.COLUMN_TIME_OFFSET,
                    TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000/60);

            fos.write((dataObject.toString() + "\n").getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void insert(String name, double timestamp, IJsonObject value) {
        if (fos == null) {
            createDatabaseFile();
        }
        try {
            JsonObject dataObject = new JsonObject();
            dataObject.addProperty(this.COLUMN_NAME, name);
            dataObject.addProperty(this.COLUMN_TIMESTAMP, timestamp);
            dataObject.add(this.COLUMN_VALUE, value);
            fos.write((dataObject.toString() + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finish() {
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath() {
        return this.context.getFileStreamPath(this.databaseName).getPath();
    }
}
