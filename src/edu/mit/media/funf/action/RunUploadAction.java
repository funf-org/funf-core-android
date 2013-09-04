package edu.mit.media.funf.action;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.util.LogUtil;

import android.util.Log;

public class RunUploadAction extends Action {

    @Configurable
    private FileArchive archive = null;

    @Configurable
    private RemoteFileArchive upload = null;

    private UploadService uploader;

    RunUploadAction() {
    }

    public RunUploadAction(FileArchive archive, RemoteFileArchive upload, UploadService uploader) {
        this.archive = archive;
        this.upload = upload;
        this.uploader = uploader;
    }

    protected void execute() {
        if (archive != null && upload != null && uploader != null) {
            Log.d(LogUtil.TAG, "running upload");
            uploader.run(archive, upload);
        } else {
            Log.d(LogUtil.TAG, "upload failed");
        }
        setHandler(null); // free system resources
    }
    
    protected boolean isLongRunningAction() {
        return true;
    }
}