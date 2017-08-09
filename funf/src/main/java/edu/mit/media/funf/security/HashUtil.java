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
package edu.mit.media.funf.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import edu.mit.media.funf.util.LogUtil;


public class HashUtil {

	private static MessageDigest instance;

	private HashUtil() {
	}
	
	public static MessageDigest getMessageDigest() {
		if (instance == null) {
			try {
				instance = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				Log.e(LogUtil.TAG, "HashUtil no SHA alghrithom", e);
				return null;
			}
		}
		return instance;
	}

	public enum HashingType {
		ONE_WAY_HASH, INTERMEDIATE_HASH_ENC, RSA_ENC
	}

	public static String oneWayHashString(String msg) {
		MessageDigest md = getMessageDigest();
		synchronized (md) {
			if (msg == null || "".equals(msg)) {
				return "";
			} else if (md == null) {
					return "NO SHA";
			} else {
				byte[] msgDigest = md.digest(msg.getBytes());
				BigInteger number = new BigInteger(1, msgDigest);
				return number.toString(16);
			}
		}
	}

	private static String oneWayHashAndRSA(Context context, String msg) {
		// Map<String, String> encMsg = new HashMap<String, String>();
		try {
			JSONObject jsonEncMsg = new JSONObject();

			jsonEncMsg.put(HashingType.ONE_WAY_HASH.name(),
					oneWayHashString(msg));
			jsonEncMsg.put(HashingType.RSA_ENC.name(), RSAEncode
					.encodeStringRSA(context, msg));
			// Log.v(TAG, "oneWayHashAndRSA, jsonEncMsg: " + jsonEncMsg);
			return jsonEncMsg.toString();
		} catch (JSONException e) {
			Log.e(LogUtil.TAG, "oneWayHashAndRSA: json error:", e);
			return "JSON ERROR!";
		}
	}

	
	public static String hashString(Context context, String msg) {
		return hashString(context, msg, HashingType.ONE_WAY_HASH);
	}

	public static String hashString(Context context, String msg,
			HashingType hashingType) {
		if (hashingType == HashingType.ONE_WAY_HASH) {
			try {
				return (new JSONObject()).put(HashingType.ONE_WAY_HASH.name(),
						oneWayHashString(msg)).toString();
			} catch (JSONException e) {
				Log.e(LogUtil.TAG, "hashString: json error:", e);
				return "JSON ERROR!";
			}
		} else if (hashingType == HashingType.INTERMEDIATE_HASH_ENC) {
			return oneWayHashAndRSA(context, msg);
		} else {
			Log.e(LogUtil.TAG, "hashString: unknown hashingMode!!!");
			return "unknown hashing mode!";
		}
	}

	public static String formatPhoneNumber(String numberString) {
		numberString = numberString.replaceAll("[^0-9]+", "");
		int i = numberString.length();
		if (i <= 10)
			return numberString;
		else
			return numberString.substring(i - 10); // only look at the last 10
													// numbers

	}
}
