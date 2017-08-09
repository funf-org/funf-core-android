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
package edu.mit.media.funf.probe.builtin;

import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

import com.google.gson.Gson;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ServicesKeys;

public class ServicesProbe extends Base implements ServicesKeys {

	/**
	 * The array of packages from which service info will be emitted. 
	 * If this parameter is null, will return every service.
	 */
	@Configurable
	private String[] packages = null;  
	
	@Override
	protected void onStart() {
		super.onStart();
		Gson gson = getGson();
		ActivityManager am = (ActivityManager)getContext().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<String> packageList = packages == null ? null : Arrays.asList(packages);
		for (RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
			String packageName = info.service.getPackageName();
			if (packageList == null || packageList.contains(packageName)) {
				sendData(gson.toJsonTree(info).getAsJsonObject());
			}
		}
		stop();
	}
	
}
