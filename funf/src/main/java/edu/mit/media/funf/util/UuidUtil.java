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
package edu.mit.media.funf.util;

import static edu.mit.media.funf.util.AsyncSharedPrefs.async;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;

public class UuidUtil {

	public static String getInstallationId(Context context) {
		if (UuidUtil.uuid == null) {
			SharedPreferences prefs = async(context.getSharedPreferences(UuidUtil.FUNF_UTILS_PREFS, Context.MODE_PRIVATE));
			UuidUtil.uuid = prefs.getString(UuidUtil.INSTALLATION_UUID_KEY, null);
			if (UuidUtil.uuid == null) {
				UuidUtil.uuid = UUID.randomUUID().toString();
				prefs.edit().putString(UuidUtil.INSTALLATION_UUID_KEY, UuidUtil.uuid).commit();
			}
		}
		return UuidUtil.uuid;
	}

	public static String uuid = null;
	public static final String INSTALLATION_UUID_KEY = "INSTALLATION_UUID";
	public static final String FUNF_UTILS_PREFS = "edu.mit.media.funf.Utils";

}
