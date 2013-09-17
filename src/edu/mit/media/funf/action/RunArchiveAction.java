package edu.mit.media.funf.action;

import java.io.File;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.util.LogUtil;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RunArchiveAction extends Action {
    
    @Configurable
    private FileArchive archive = null;
    
    @Configurable
    private SQLiteOpenHelper dbHelper = null;
    
    RunArchiveAction() {
    }
    
    public RunArchiveAction(FileArchive archive, SQLiteOpenHelper dbHelper) {
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
        setHandler(null); // free system resources
    }
    
    protected boolean isLongRunningAction() {
        return true;
    }
}
