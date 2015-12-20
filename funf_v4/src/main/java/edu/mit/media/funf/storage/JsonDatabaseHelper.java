package edu.mit.media.funf.storage;

import com.google.gson.JsonObject;

import android.content.Context;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TimeZone;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;

/**
 * Created by astopczynski on 10/26/15.
 */
public class JsonDatabaseHelper implements DatabaseHelper{

    public static final String COLUMN_TIME_OFFSET = "time_offset";


    Context context;
    String databaseName;
    String tempFileName;
    static FileOutputStream fos = null;
    private FileOutputStream tempFos = null;

    public JsonDatabaseHelper(Context context, String name, int version) {
        this.context = context;
        this.databaseName = name;
        this.tempFileName = this.databaseName + "_temp";
    }

    public void init() {
        this.createDatabaseFile();
    }

    private void createDatabaseFile() {
        try {
            fos = this.context.openFileOutput(this.databaseName, Context.MODE_APPEND);

            JsonObject dataObject = new JsonObject();
            dataObject.addProperty("ANDROID_ID",
                    Settings.Secure.getString(
                            this.context.getContentResolver(), Settings.Secure.ANDROID_ID
                    ));
            dataObject.addProperty(this.COLUMN_TIME_OFFSET,
                TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60);

            try {
                dataObject.addProperty("FUNF_VERSION", FunfManager.funfManager.getVersion());
            } catch (NullPointerException e) {}

            try {
                dataObject.addProperty("APPLICATION_VERSION", FunfManager.funfManager.getApplicationVersion());
            } catch (NullPointerException e) {}

            fos.write((dataObject.toString() + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void insert(String name, double timestamp, IJsonObject value) {
        if (fos == null) {
            createDatabaseFile();
        }
        JsonObject dataObject = null;
        try {
            dataObject = new JsonObject();
            dataObject.addProperty(this.COLUMN_NAME, name);
            dataObject.addProperty(this.COLUMN_TIMESTAMP, timestamp);
            dataObject.add(this.COLUMN_VALUE, value);
            fos.write((dataObject.toString() + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            insertIntoTemp(dataObject);
        }
    }

    private void createTempFile() {
        try {
            tempFos = this.context.openFileOutput(this.tempFileName, Context.MODE_APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finalizeTempFile() {
        try {
            if (tempFos != null) tempFos.close();
            (new File(this.tempFileName)).delete();
        } catch (IOException e) {
        }

    }

    private void copyFromTempFile() {
        try {
            FileInputStream tempFis = this.context.openFileInput(this.tempFileName);
            BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(tempFis, "UTF-8"));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) break;
                fos.write((line + "\n").getBytes());
            }

        } catch (IOException e) {
        }
    }

    private void insertIntoTemp(JsonObject dataObject) {
        createTempFile();
        try {
            if (dataObject != null) {
                tempFos.write((dataObject.toString() + "\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finish() {
        try {
            if (fos != null) {
                copyFromTempFile();
                finalizeTempFile();
                fos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath() {
        return this.context.getFileStreamPath(this.databaseName).getPath();
    }
}
