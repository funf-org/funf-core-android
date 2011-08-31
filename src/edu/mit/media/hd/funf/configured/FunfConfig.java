package edu.mit.media.hd.funf.configured;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.hd.funf.Utils;
import edu.mit.media.hd.funf.probe.ProbeExceptions.UnstorableTypeException;

/**
 * Immutable class representing configuration of the funf system.
 *
 */
public class FunfConfig {
	public static final String VERSION_KEY = "version";
	public static final String CONFIG_URL_KEY = "configUrl";
	public static final String UPDATE_PERIOD_KEY = "configUpdatePeriod";
	public static final String ARCHIVE_PERIOD_KEY = "archivePeriod";
	public static final String REMOTE_ARCHIVE_PERIOD_KEY = "remoteArchivePeriod";
	public static final String DATABASES_KEY = "databases";
	public static final String DATA_REQUESTS_KEY = "dataRequests";
	

	public static final long DEFAULT_ARCHIVE_PERIOD = 3 * 60 * 60;  // 3 hours
	public static final long DEFAULT_REMOTE_ARCHIVE_PERIOD = 6 * 60 * 60;  // 6 hours
	static final int DEFAULT_UPDATE_PERIOD = 1 * 60 * 60;
	// TODO: should we add an application name to the config, so that multiple apps can co-exist?
	// private final String appName;
	private final int version;
	private final String configUrl;
	private final long updatePeriod, archivePeriod, remoteArchivePeriod;
	private final Map<String, ProbeDatabaseConfig> databases;
	private final Map<String,Bundle[]> dataRequests;
	
	/**
	 * @param version
	 * @param configDownloadUrl
	 * @param configCheckPeriod in seconds
	 * @param archivePeriod in seconds
	 * @param remoteArchivePeriod in seconds
	 * @param databases
	 * @param dataRequests
	 */
	public FunfConfig(int version, String configDownloadUrl, long configCheckPeriod, long archivePeriod, long remoteArchivePeriod, Map<String, ProbeDatabaseConfig> databases, Map<String,Bundle[]> dataRequests) {
		this.version = version;
		this.configUrl = configDownloadUrl;
		this.updatePeriod = configCheckPeriod;
		this.archivePeriod = archivePeriod;
		this.remoteArchivePeriod = remoteArchivePeriod;
		this.databases = new HashMap<String, ProbeDatabaseConfig>(databases);
		this.dataRequests = new HashMap<String, Bundle[]>(dataRequests);
	}
	
	@SuppressWarnings("unchecked")
	public FunfConfig(String jsonString) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonString);
		version = jsonObject.getInt(VERSION_KEY);
		configUrl = jsonObject.optString(CONFIG_URL_KEY, null);
		updatePeriod = jsonObject.optLong(UPDATE_PERIOD_KEY, 0L);
		archivePeriod = jsonObject.optLong(ARCHIVE_PERIOD_KEY, 0L);
		remoteArchivePeriod = jsonObject.optLong(REMOTE_ARCHIVE_PERIOD_KEY, 0L);
		
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
	
	private static FunfConfig cachedConfig = null;
	
	public static boolean setFunfConfig(Context context, FunfConfig funfConfig) {
		return setFunfConfig(context, context.getPackageName(), funfConfig);
	}
	
	static boolean setFunfConfig(Context context, String appPackage, FunfConfig funfConfig) {
		try {
			Log.d(FunfConfig.class.getName(),"Config set");
			context.getSharedPreferences("FUNF_CONFIG", Context.MODE_PRIVATE).edit().putString(appPackage, funfConfig.toJson()).commit();
			cachedConfig = null;
			return true;
		} catch (JSONException e) {
			Log.e(FunfConfig.class.getName(),"Malformed configuration!!", e);
			return false;
		}
	}
	
	public static FunfConfig getFunfConfig(Context context) {
		return getFunfConfig(context, context.getPackageName());
	}
	
	static FunfConfig getFunfConfig(Context context, String appPackage) {
		if (cachedConfig == null) {
			String configJson = context.getSharedPreferences("FUNF_CONFIG", Context.MODE_PRIVATE).getString(appPackage, null);
			try {
				if (configJson == null) Log.i(FunfConfig.class.getName(),"Config does not exist");
				cachedConfig = configJson == null ? null : new FunfConfig(configJson);
			} catch (JSONException e) {
				Log.e(FunfConfig.class.getName(),"Malformed configuration!!", e);
				return null;
			}
		}
		return cachedConfig;
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

	/**
	 * Period in which configuration should be update, in seconds.
	 * @return
	 */
	public long getConfigUpdatePeriod() {
		return updatePeriod == 0L ? DEFAULT_UPDATE_PERIOD : updatePeriod;
	}
	
	/**
	 * Period in which databases are archived, in seconds;
	 * @return
	 */
	public long getArchivePeriod() {
		return archivePeriod == 0L ? DEFAULT_ARCHIVE_PERIOD : archivePeriod;
	}
	
	/**
	 * Period in which databases are remotely archived, in seconds;
	 * @return
	 */
	public long getRemoteArchivePeriod() {
		return remoteArchivePeriod == 0L ? DEFAULT_REMOTE_ARCHIVE_PERIOD : remoteArchivePeriod;
	}

	public Map<String, ProbeDatabaseConfig> getDatabases() {
		return new HashMap<String, ProbeDatabaseConfig>(databases);
	}

	public Map<String, Bundle[]> getDataRequests() {
		return new HashMap<String, Bundle[]>(dataRequests);
	}

	public String toJson() throws JSONException {
		return toJson(false);
	}
	
	public String toJson(boolean prettyPrint) throws JSONException {
		JSONObject jsonObject = toJsonObject();
		return prettyPrint ? jsonObject.toString(4) : toJsonObject().toString();
	}
	
	JSONObject toJsonObject() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(VERSION_KEY, version);
		jsonObject.put(CONFIG_URL_KEY, configUrl);
		if (updatePeriod != 0L) {
			jsonObject.put(UPDATE_PERIOD_KEY, updatePeriod);
		}
		if (archivePeriod != 0L) {
			jsonObject.put(ARCHIVE_PERIOD_KEY, archivePeriod);
		}
		if (remoteArchivePeriod != 0L) {
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
