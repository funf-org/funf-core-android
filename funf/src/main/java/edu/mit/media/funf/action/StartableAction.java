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

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.Startable;
import edu.mit.media.funf.datasource.Startable.TriggerAction;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import android.util.Log;

public class StartableAction extends Action implements TriggerAction {
        
    @Configurable
    private Double duration = null;
    
    @Configurable
    private Startable target = null;
    
    StartableAction() {
    }

    public void setTarget(Startable target) {
        this.target = target;
    }
    
    protected void execute() {
        if (target == null) 
            return;
        Log.d(LogUtil.TAG, "running action start");
        Log.d(LogUtil.TAG, target.getClass().getName());
        target.start();
        if (duration != null && duration > 0.0) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(LogUtil.TAG, "running action stop");
                    target.stop();
                }
            }, TimeUtil.secondsToMillis(duration));   
        }
    }
    
    protected boolean isLongRunningAction() {
        return true;
    }
}
