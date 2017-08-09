/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
