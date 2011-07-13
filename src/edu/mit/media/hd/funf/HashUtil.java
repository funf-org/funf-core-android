package edu.mit.media.hd.funf;

/**
 * 
 * This file is part of the FunF Software System
 * Copyright � 2010, Massachusetts Institute of Technology  
 * Do not distribute or use without permission.
 * Contact: Nadav Aharony (nadav@mit.edu) or friendsandfamily@media.mit.edu
 * 
 * @date Jan, 2010
 * 
 */

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class HashUtil {

	private static MessageDigest md;
	private static final String TAG = "FNF";

	private HashUtil() {
	}

	public enum HashingType {
		ONE_WAY_HASH, INTERMEDIATE_HASH_ENC, RSA_ENC
	}

	private static String oneWayHashString(String msg) {
		if ((msg != null) || (msg != "")) {
			if (md == null) {
				try {
					md = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					Log.e(TAG, "HashUtil no SHA alghrithom", e);
				}
			}
			byte[] msgDigest = md.digest(msg.getBytes());
			BigInteger number = new BigInteger(1, msgDigest);
			return number.toString(16);
		} else {
			Log
					.e(TAG,
							"HashUtil:hashString: received a null or empty string!, returning empty string");
			return ("");
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
			Log.e(TAG + " oneWayHashAndRSA: json error:", e.getMessage());
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
				Log.e(TAG + " hashString: json error:", e.getMessage());
				return "JSON ERROR!";
			}
		} else if (hashingType == HashingType.INTERMEDIATE_HASH_ENC) {
			return oneWayHashAndRSA(context, msg);
		} else {
			Log.e(TAG, "hashString: unknown hashingMode!!!");
			return "unknown hashing mode!";
		}
	}

	// by Wei Pan
	public static String formatPhoneNumber(String numberString) {
		numberString = numberString.replaceAll("\\(", "");
		numberString = numberString.replaceAll("\\)", "");
		numberString = numberString.replaceAll("-", "");
		numberString = numberString.replaceAll("\\+", "");
		numberString = numberString.replaceAll("\\*", "");
		int i = numberString.length();
		if (i <= 10)
			return numberString;
		else
			return numberString.substring(i - 10); // only look at the last 10
													// numbers

	}
}
