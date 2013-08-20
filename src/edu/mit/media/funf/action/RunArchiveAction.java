package edu.mit.media.funf.action;

import java.io.File;

import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.util.LogUtil;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class RunArchiveAction extends Action {

    private Looper looper = null;
    private Handler myHandler = null;
    
    private FileArchive archive = null;
    private SQLiteOpenHelper dbHelper = null;
    
    public RunArchiveAction(ActionGraph graph, FileArchive archive, SQLiteOpenHelper dbHelper) {
        super(graph);
        this.archive = archive;
        this.dbHelper = dbHelper;
    }
    
    protected void execute() {
        Log.d(LogUtil.TAG, "running archive");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // TODO: add check to make sure this is not empty
        File dbFile = new File(db.getPath());
        db.close();
        if (archive.add(dbFile)) {
          dbFile.delete();
        }
        dbHelper.getWritableDatabase(); // Build new database
        Log.d(LogUtil.TAG, "archived!");
        exitMyHandler(); // free system resources
    }
    
    @Override
    public void runInHandler() {
        ensureMyHandlerExists(); // run data archive on a dedicated thread
        super.runInHandler();
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
