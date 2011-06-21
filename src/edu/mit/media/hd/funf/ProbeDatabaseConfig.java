package edu.mit.media.hd.funf;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Immutable class representing configuration of a database of probe data, 
 * including data requests for probes.
 *
 */
public class ProbeDatabaseConfig {
	public static final String UPLOAD_URL_KEY = "uploadUrl";
	public static final String ENCRYPTION_KEY_KEY = "encryptionKey";
	public static final String PROBES_TO_RECORD_KEY = "probesToRecord";
	
	private final String[] probesToRecord;
	private final String uploadUrl;
	private final String encryptionKey;
	
	
	public ProbeDatabaseConfig(String[] probesToRecord, String dataUploadUrl, String encryptionKey) {
		this.probesToRecord = copyStringArray(probesToRecord);
		this.uploadUrl = dataUploadUrl;
		this.encryptionKey = encryptionKey;
	}
	
	public ProbeDatabaseConfig(JSONObject jsonObject) throws JSONException {
		JSONArray probesJsonArray = jsonObject.getJSONArray(PROBES_TO_RECORD_KEY);
		this.probesToRecord = new String[probesJsonArray.length()];
		for (int i = 0; i < probesJsonArray.length(); i++) {
			this.probesToRecord[i] = probesJsonArray.optString(i);
			// TODO: consider removing null or uncoerceable items
		}
		this.uploadUrl = jsonObject.optString(UPLOAD_URL_KEY, null);
		this.encryptionKey = jsonObject.optString(ENCRYPTION_KEY_KEY, null);
		
	}
	
	ProbeDatabaseConfig(String jsonString) throws JSONException {
		this(new JSONObject(jsonString));
	}

	public String getUploadUrl() {
		return uploadUrl;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public String[] getProbesToRecord() {
		return copyStringArray(probesToRecord);
	}
	
	public String toJson() {
		return toJsonObject().toString();
	}
	
	JSONObject toJsonObject() {
		JSONArray probesJsonArray = new JSONArray(Arrays.asList(probesToRecord));
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(PROBES_TO_RECORD_KEY, probesJsonArray);
			if (uploadUrl != null) {
				jsonObject.put(UPLOAD_URL_KEY, uploadUrl);
			}
			if (encryptionKey != null) {
				jsonObject.put(ENCRYPTION_KEY_KEY, encryptionKey);
			}
		} catch (JSONException e) {
			throw new RuntimeException("Unable to serialize json");
		}
		return jsonObject;
	}

	private static String[] copyStringArray(String[] strings) {
		String[] copy = new String[strings.length];
		System.arraycopy(strings, 0, copy, 0, strings.length);
		return copy;
	}
		
}
