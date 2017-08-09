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

import java.lang.Runnable;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class Action implements Runnable {
    
    private Looper looper = null;
    private Handler handler = null;
    
    Action() {
    }
        
    protected Handler getHandler() {
        return handler;
    }
    
    public void setHandler(Handler handler) {
        if (this.handler != null) {
            exitHandler();
        }
        this.handler = handler;
    }
    
    @Override
    public final void run() {
        if (isLongRunningAction()) {
            ensureHandlerExists();
            if (Looper.myLooper() != getHandler().getLooper()) {
                getHandler().post(this);
                return;
            }
        }
        execute();
    }
    
    /**
     * Override this function to include the action-specific code.
     */
    protected void execute() {
        // Perform action here
    }
    
    protected boolean isLongRunningAction() {
        return false;
    }
    
    protected void ensureHandlerExists() {
        if (handler == null) {
            synchronized (this) {
                if (looper == null) {
                    HandlerThread thread = new HandlerThread("Action[" + getClass().getName() + "]");
                    thread.start();
                    looper = thread.getLooper();
                    handler = new Handler(looper);
                }
            }
        }
    }
    
    protected void exitHandler() {
        if (handler != null) {
            synchronized (this) {
                if (looper != null) {
                    looper.quit();
                    looper = null;
                    handler = null;                    
                }
            }
        }   
    }
    
}
