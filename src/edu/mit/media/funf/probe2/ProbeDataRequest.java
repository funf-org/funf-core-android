package edu.mit.media.funf.probe2;

import android.net.Uri;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.EqualsUtil;
import edu.mit.media.funf.HashCodeUtil;
import edu.mit.media.funf.JsonUtils;

/**
 * Immutable class for describing a data request from a probe.
 *
 */
public class ProbeDataRequest {
	public static final String 
		PROBE_NAME = "probeName",
		CONFIG = "config",
		SCHEDULE = "schedule";
	
	private final String probeName;
	private final Class<? extends Probe> probeClass;
	private final String config;
	private final String schedule;
	
	public ProbeDataRequest(String probeName, JsonObject config,  JsonObject schedule) {
		if (probeName == null) {
			throw new RuntimeException("Probe data request must specify a probe name or class.");
		}
		this.probeName = probeName;
		this.probeClass = Probe.Base.getProbeClass(probeName);
		this.config = config == null ? null : JsonUtils.deepSort(config).toString();
		this.schedule = schedule == null ? null : JsonUtils.deepSort(schedule).toString();
	}
	
	public ProbeDataRequest(Class<? extends Probe> probeClass, JsonObject config, JsonObject schedule) {
		if (probeClass == null) {
			throw new RuntimeException("Probe data request must specify a probe name or class.");
		}
		this.probeName = probeClass.getName();
		this.probeClass = probeClass;
		this.config = config == null ? null : JsonUtils.deepSort(config).toString();
		this.schedule = schedule == null ? null : JsonUtils.deepSort(schedule).toString();
	}
	
	public ProbeDataRequest(JsonObject requestJsonObject) {
		JsonElement nameEl = requestJsonObject.get(PROBE_NAME);
		if (JsonNull.INSTANCE == nameEl) {
			throw new RuntimeException("Probe data request json structure must specify a 'probeName'.");
		}
		this.probeName = nameEl.getAsString();
		this.probeClass = Probe.Base.getProbeClass(probeName);
		JsonElement configEl = requestJsonObject.get(CONFIG);
		if (JsonNull.INSTANCE == configEl) {
			this.config = null;
		} else {
			this.config = JsonUtils.deepSort(configEl.getAsJsonObject()).toString();
		}
		JsonElement scheduleEl = requestJsonObject.get(SCHEDULE);
		if (JsonNull.INSTANCE == scheduleEl) {
			this.schedule = null;
		} else {
			this.schedule = JsonUtils.deepSort(scheduleEl.getAsJsonObject()).toString();
		}
	}

	public String getProbeName() {
		return probeName;
	}

	public Class<? extends Probe> getProbeClass() {
		return probeClass;
	}

	public JsonObject getConfig() {
		// Ensures objects are copies so they can be mutated without affecting this request
		return (JsonObject) (config == null ? null : new JsonParser().parse(config));
	}

	public JsonObject getSchedule() {
		// Ensures objects are copies so they can be mutated without affecting this request
		return (JsonObject) (schedule == null ? null : new JsonParser().parse(schedule));
	}
	
	/**
	 * A consistent uri for the probe class and config pair.  This can be used as an identifier for the probe instance.
	 * @return
	 */
	public Uri getProbeUri() {
		return Probe.Identifier.getProbeUri(probeClass == null ? probeName : probeClass.getName(), config);
	}
	
	public JsonObject getAsJsonObject() {
		JsonObject o = new JsonObject();
		o.addProperty(PROBE_NAME, probeName);
		o.add(CONFIG, getConfig());
		o.add(SCHEDULE, getSchedule());
		return o;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof ProbeDataRequest) {
			ProbeDataRequest oRequest = (ProbeDataRequest) o;
			return EqualsUtil.areEqual(this.probeName, oRequest.probeName) 
					&& EqualsUtil.areEqual(this.config, oRequest.config)
					&& EqualsUtil.areEqual(this.schedule, oRequest.schedule);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = HashCodeUtil.SEED;
		hash = HashCodeUtil.hash(hash, probeName);
		hash = HashCodeUtil.hash(hash, config);
		hash = HashCodeUtil.hash(hash, schedule);
		return hash;
	}

	@Override
	public String toString() {
		return getAsJsonObject().toString();
	}
}
