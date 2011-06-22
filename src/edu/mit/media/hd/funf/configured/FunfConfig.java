package edu.mit.media.hd.funf.configured;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Utils;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

/**
 * Immutable class representing configuration of the funf system.
 *
 */
public class FunfConfig {
	public static final String VERSION_KEY = "version";
	public static final String CONFIG_URL_KEY = "configUrl";
	public static final String UPDATE_PERIOD_KEY = "updatePeriod";
	public static final String ARCHIVE_PERIOD_KEY = "archivePeriod";
	public static final String REMOTE_ARCHIVE_PERIOD_KEY = "remoteArchivePeriod";
	public static final String DATABASES_KEY = "databases";
	public static final String DATA_REQUESTS_KEY = "dataRequests";
	

	public static final long DEFAULT_ARCHIVE_PERIOD = 3 * 60 * 60 * 1000;  // 3 hours
	public static final long DEFAULT_REMOTE_ARCHIVE_PERIOD = 6 * 60 * 60 * 1000;  // 6 hours
	static final int DEFAULT_UPDATE_PERIOD = 1000 * 60 * 60;
	// TODO: should we add an application name to the config, so that multiple apps can co-exist?
	// private final String appName;
	private final int version;
	private final String configUrl;
	private final long updatePeriod, archivePeriod, remoteArchivePeriod;
	private final Map<String, ProbeDatabaseConfig> databases;
	private final Map<String,Bundle[]> dataRequests;
	
	public FunfConfig(int version, String configDownloadUrl, int configCheckPeriod, long archivePeriod, long remoteArchivePeriod, Map<String, ProbeDatabaseConfig> databases, Map<String,Bundle[]> dataRequests) {
		this.version = version;
		this.configUrl = configDownloadUrl;
		this.updatePeriod = configCheckPeriod == 0 ? DEFAULT_UPDATE_PERIOD : configCheckPeriod;
		this.archivePeriod = archivePeriod == 0 ? DEFAULT_ARCHIVE_PERIOD : archivePeriod;
		this.remoteArchivePeriod = remoteArchivePeriod == 0 ? DEFAULT_REMOTE_ARCHIVE_PERIOD : remoteArchivePeriod;
		this.databases = new HashMap<String, ProbeDatabaseConfig>(databases);
		this.dataRequests = new HashMap<String, Bundle[]>(dataRequests);
	}
	
	@SuppressWarnings("unchecked")
	public FunfConfig(String jsonString) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonString);
		version = jsonObject.getInt(VERSION_KEY);
		configUrl = jsonObject.optString(CONFIG_URL_KEY, null);
		updatePeriod = jsonObject.optLong(UPDATE_PERIOD_KEY, DEFAULT_UPDATE_PERIOD);
		this.archivePeriod = jsonObject.optLong(ARCHIVE_PERIOD_KEY, DEFAULT_ARCHIVE_PERIOD);
		this.remoteArchivePeriod = jsonObject.optLong(REMOTE_ARCHIVE_PERIOD_KEY, DEFAULT_REMOTE_ARCHIVE_PERIOD);
		
		databases = new HashMap<String, ProbeDatabaseConfig>();
		JSONObject databasesJsonObject = jsonObject.getJSONObject(DATABASES_KEY);
		Iterator<String> databaseNames = databasesJsonObject.keys();
		while (databaseNames.hasNext()) {
			String databaseName = databaseNames.next();
			databases.put(databaseName, 
					new ProbeDatabaseConfig(databasesJsonObject.getJSONObject(databaseName)));
		}
		
		dataRequests = new HashMap<String, Bundle[]>();
		JSONObject dataRequestsObject = jsonObject.getJSONObject(DATA_REQUESTS_KEY);
		Iterator<String> probeNames = dataRequestsObject.keys();
		while (probeNames.hasNext()) {
			String probeName = probeNames.next();
			JSONArray requestsJsonArray = dataRequestsObject.getJSONArray(probeName);
			dataRequests.put(probeName, getBundleArray(requestsJsonArray));
		}
	}
	
	public static boolean setFunfConfig(Context context, FunfConfig funfConfig) {
		return setFunfConfig(context, context.getPackageName(), funfConfig);
	}
	
	static boolean setFunfConfig(Context context, String appPackage, FunfConfig funfConfig) {
		try {
			context.getSharedPreferences("FUNF_CONFIG", Context.MODE_PRIVATE).edit().putString(appPackage, funfConfig.toJson()).commit();
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	public static FunfConfig getFunfConfig(Context context) {
		return getFunfConfig(context, context.getPackageName());
	}
	
	static FunfConfig getFunfConfig(Context context, String appPackage) {
		// TODO: queue up update if necessary
		String configJson = context.getSharedPreferences("FUNF_CONFIG", Context.MODE_PRIVATE).getString(appPackage, null);
		try {
			return configJson == null ? null : new FunfConfig(configJson);
		} catch (JSONException e) {
			return null;
		}
	}
	
	private static Bundle[] getBundleArray(JSONArray jsonArray) throws JSONException {
		Bundle[] request = new Bundle[jsonArray.length()];
		for (int i = 0; i < jsonArray.length(); i++) {
			request[i] = getBundle(jsonArray.getJSONObject(i));
		}
		return request;
	}
	
	private static JSONArray toJSONArray(Bundle[] bundles) {
		JSONArray jsonArray = new JSONArray();
		for (Bundle bundle : bundles) {
			jsonArray.put(toJSONObject(bundle));
		}
		return jsonArray;
	}
	
	@SuppressWarnings("unchecked")
	private static Bundle getBundle(JSONObject jsonObject) throws JSONException {
		Bundle requestPart = new Bundle();
		Iterator<String> paramNames = jsonObject.keys();
		while (paramNames.hasNext()) {
			String paramName = paramNames.next();
			try  {
			Utils.putInBundle(requestPart, paramName, jsonObject.get(paramName));
			} catch (UnstorableTypeException e) {
				throw new JSONException(e.getLocalizedMessage());
			}
		}
		return requestPart;
	}
	
	private static JSONObject toJSONObject(Bundle bundle) {
		return new JSONObject(Utils.getValues(bundle));
	}

	
	public int getVersion() {
		return version;
	}

	public String getConfigUrl() {
		return configUrl;
	}

	public long getUpdatePeriod() {
		return updatePeriod;
	}
	
	public long getArchivePeriod() {
		return archivePeriod;
	}

	public long getRemoteArchivePeriod() {
		return remoteArchivePeriod;
	}

	public Map<String, ProbeDatabaseConfig> getDatabases() {
		return new HashMap<String, ProbeDatabaseConfig>(databases);
	}

	public Map<String, Bundle[]> getDataRequests() {
		return new HashMap<String, Bundle[]>(dataRequests);
	}

	public String toJson() throws JSONException {
		return toJsonObject().toString();
	}
	JSONObject toJsonObject() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(VERSION_KEY, version);
		jsonObject.put(CONFIG_URL_KEY, configUrl);
		if (updatePeriod != DEFAULT_UPDATE_PERIOD) {
			jsonObject.put(UPDATE_PERIOD_KEY, updatePeriod);
		}
		if (archivePeriod != DEFAULT_ARCHIVE_PERIOD) {
			jsonObject.put(ARCHIVE_PERIOD_KEY, archivePeriod);
		}
		if (remoteArchivePeriod != DEFAULT_REMOTE_ARCHIVE_PERIOD) {
			jsonObject.put(REMOTE_ARCHIVE_PERIOD_KEY, remoteArchivePeriod);
		}
		
		JSONObject databasesJsonObject = new JSONObject();
		for (Map.Entry<String, ProbeDatabaseConfig> databaseConfig : databases.entrySet()) {
			databasesJsonObject.put(databaseConfig.getKey(), databaseConfig.getValue().toJsonObject());
		}
		jsonObject.put(DATABASES_KEY, databasesJsonObject);
		
		JSONObject dataRequestsJson = new JSONObject();
		for (Map.Entry<String, Bundle[]> dataReqest : dataRequests.entrySet()) {
			dataRequestsJson.put(dataReqest.getKey(), toJSONArray(dataReqest.getValue()));
		}
		jsonObject.put(DATA_REQUESTS_KEY, dataRequestsJson);
		
		return jsonObject;
	}
}
