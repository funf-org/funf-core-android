/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
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
