package edu.mit.media.funf.probe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.JsonUtils;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.StartableProbe;

/**
 * This service coordinates satisfying data requests by scheduling and running probes.
 * 
 * @author alangardner
 *
 */
public class ProbeManager extends Service implements ProbeFactory {
	
	public static final String PREFIX = "edu.mit.media.funf.probe";
	public static final String
		ACTION_SCHEDULE = PREFIX + ".SCHEDULE",
		ACTION_ENABLE_PROBE = PREFIX + ".ENABLE",
		ACTION_START_PROBE = PREFIX + ".START",
		ACTION_STOP_PROBE = PREFIX + ".STOP",
		ACTION_DISABLE_PROBE = PREFIX + ".DISABLE";
	
	private ProbeFactory cacheFactory;
	private Map<Probe.DataListener, Set<ProbeDataRequest>> requests;
	private Map<Uri,Map<Probe.DataListener,Double>> requestSatisfiedTimestamps; // Map used in place of set only for WeakRefs
	private AlarmManager manager;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		cacheFactory = ProbeFactory.CachingProbeFactory.getInstance(this);
		requests = new WeakHashMap<Probe.DataListener, Set<ProbeDataRequest>>();
		requestSatisfiedTimestamps = new HashMap<Uri, Map<DataListener,Double>>();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action == null) {
			
		} else if (ACTION_ENABLE_PROBE.equals(action)
				|| ACTION_DISABLE_PROBE.equals(action)
				|| ACTION_START_PROBE.equals(action)
				|| ACTION_STOP_PROBE.equals(action)) {
			Probe probe = getProbe(intent.getData());
			if (probe != null) {
				Set<DataListener> listeners = requestSatisfiedTimestamps.get(intent.getData()).keySet();
				//listeners.add(this);  // TODO: enable this to be a listener, for triggers
				
				if (ACTION_ENABLE_PROBE.equals(action)) {
					// TODO: clean this up, it is way to complicated
					Set<DataListener> opportunisticListeners = new HashSet<DataListener>();
					for (DataListener listener : listeners) {
						 Set<ProbeDataRequest> dataRequests = requests.get(listener);
						 boolean opportunistic = false;
						 for (ProbeDataRequest dataRequest : dataRequests) {
							 BasicSchedule schedule = new BasicSchedule(Probe.Base.getProbeClass(Probe.Identifier.getProbeName(intent.getData())), dataRequest.getSchedule());
							 opportunistic = opportunistic || schedule.isOpportunistic();
						 }
						 if (opportunistic) {
							 opportunisticListeners.add(listener);
						 }
					}
					probe.unregisterPassiveListener((DataListener[])opportunisticListeners.toArray());
				} else if (ACTION_DISABLE_PROBE.equals(action)) {
					probe.unregisterPassiveListener((DataListener[])listeners.toArray());
				} else if (ACTION_START_PROBE.equals(action) && probe instanceof StartableProbe) {
					probe.registerListener((DataListener[])listeners.toArray());
					if (probe instanceof ContinuousProbe) {
						scheduleStop(intent.getData());
					}
				} else if (ACTION_STOP_PROBE.equals(action)  && probe instanceof ContinuousProbe) {
					probe.unregisterListener((DataListener[])listeners.toArray());
				} 
			}
		} else if (ACTION_SCHEDULE.equals(action)) {
			if (intent.getData() == null) {
				scheduleAll();
			} else {
				schedule(intent.getData());
			}
		}
		
		return START_REDELIVER_INTENT;
	}

	public Set<ProbeDataRequest> getDataRequests(Probe.DataListener listener) {
		Set<ProbeDataRequest> requestSet = new HashSet<ProbeDataRequest>();
		if (requests.containsKey(listener)) {
			requestSet.addAll(requests.get(listener));
		}
		return requestSet;
	}

	public synchronized void requestData(Probe.DataListener listener, ProbeDataRequest request) {
		Set<ProbeDataRequest> dataRequests = requests.get(listener);
		if (dataRequests == null) {
			dataRequests = new HashSet<ProbeDataRequest>();
			requests.put(listener, dataRequests);
		}
		dataRequests.add(request);
		
		Uri probeUri = request.getProbeUri();
		Map<Probe.DataListener,Double> listeners = requestSatisfiedTimestamps.get(probeUri);
		if (listeners == null) {
			listeners = new WeakHashMap<Probe.DataListener,Double>();
			requestSatisfiedTimestamps.put(probeUri, listeners);
		}
		listeners.put(listener, null);
		schedule(request.getProbeUri());
	}
	
	public synchronized void unrequestData(Probe.DataListener listener, ProbeDataRequest request) {
		Set<ProbeDataRequest> dataRequests = requests.get(listener);
		if (dataRequests != null) {
			dataRequests.remove(request);
			if (dataRequests.isEmpty()) {
				requests.remove(listener);
			}
		}
		
		Uri probeUri = request.getProbeUri();
		Map<Probe.DataListener,Double> listeners = requestSatisfiedTimestamps.get(probeUri);
		if (listeners != null) {
			listeners.remove(probeUri);
			if (listeners.isEmpty()) {
				requestSatisfiedTimestamps.remove(probeUri);
			}
		}
		schedule(request.getProbeUri());
	}
	
	public synchronized void unrequestAllData(Probe.DataListener listener) {
		Set<ProbeDataRequest> dataRequests = requests.get(listener);
		if (dataRequests != null) {
			for (ProbeDataRequest dataRequest : dataRequests) {
				unrequestData(listener, dataRequest);
			}
		}
		requests.remove(listener);
		scheduleAll();
	}

	@Override
	public Probe getProbe(Uri probeUri) {
		if (probeUri == null) {
			return null;
		}
		return cacheFactory.getProbe(probeUri);
	}

	@Override
	public Probe getProbe(String name, JsonObject config) {
		return cacheFactory.getProbe(name, config);
	}

	@Override
	public Probe getProbe(Class<? extends Probe> probeClass, JsonObject config) {
		return cacheFactory.getProbe(probeClass, config);
	}


	/**************************************
	 * Simple scheduling implementation
	 * 
	 * For each probe:
	 * Min period
	 * Max duration (for continuous probes)
	 * 
	 **************************************/
	

	
	private class BasicSchedule {
		
		private Double period = null;
		private Double duration = null;
		private boolean opportunistic = true;
		private boolean strict = false;
		
		private BasicSchedule(Class<? extends Probe> probeClass, JsonObject values) {
			DefaultSchedule annotation = probeClass.getAnnotation(DefaultSchedule.class);
			String defaultScheduleString = annotation == null ? null : annotation.value();
			JsonObject defaultSchedule = null;
			try {
				defaultSchedule = new JsonParser().parse(defaultScheduleString).getAsJsonObject();
			} catch (IllegalStateException e) {
				defaultSchedule = new JsonObject();
			}
			
			JsonObject schedule = defaultSchedule;
			JsonUtils.deepCopyOnto(values, schedule, true);
			
			try {period = schedule.get("period").getAsDouble(); } catch (ClassCastException e) {} catch (IllegalStateException e) {}
			try {duration = schedule.get("duration").getAsDouble(); } catch (ClassCastException e) {} catch (IllegalStateException e) {}
			try {opportunistic = schedule.get("opportunistic").getAsBoolean(); } catch (ClassCastException e) {} catch (IllegalStateException e) {}
			try {opportunistic = schedule.get("strict").getAsBoolean(); } catch (ClassCastException e) {} catch (IllegalStateException e) {}
		}

		public Double getPeriod() {
			return period;
		}

		public Double getDuration() {
			return duration;
		}

		public boolean isOpportunistic() {
			return opportunistic;
		}

		public boolean isStrict() {
			return strict;
		}
	}

	private void scheduleAll() {
		for (Uri probeUri : requestSatisfiedTimestamps.keySet()) {
			schedule(probeUri);
		}
	}
	
	private void schedule(Uri probeUri) {
		Class<? extends Probe> probeClass = Probe.Base.getProbeClass(Probe.Identifier.getProbeName(probeUri));
		
		// Figure out when the next time this probe needs to be run is
		Double nextRunTime = null;
		Double minPeriod = null;
		boolean strict = false;
		for (Map.Entry<DataListener,Double> listenerSatisfied : requestSatisfiedTimestamps.get(probeUri).entrySet()) {
			DataListener listener = listenerSatisfied.getKey();
			Double lastSatisfied = listenerSatisfied.getValue();
			if (lastSatisfied == null) {
				nextRunTime = Utils.getTimestamp().doubleValue();
				break;
			} else {
				for (ProbeDataRequest request : requests.get(listener)) {
					BasicSchedule schedule = new BasicSchedule(Probe.Base.getProbeClass(Probe.Identifier.getProbeName(probeUri)), request.getSchedule());
					if (StartableProbe.class.isAssignableFrom(probeClass)) {
						Double period = schedule.getPeriod();
						if (period != null) {
							double requestNextRunTime = lastSatisfied + period;
							nextRunTime = (nextRunTime == null) ? requestNextRunTime : Math.min(nextRunTime, requestNextRunTime);
							minPeriod = (minPeriod == null) ? period :  Math.min(minPeriod, period);
							strict = strict || schedule.isStrict();
						}
					}
				}
			}
		}

		Intent i = new Intent(ACTION_START_PROBE, probeUri);
		i.setClass(this, getClass());
		PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		if (nextRunTime == null) {
			// Cancel alarm
			manager.cancel(pi);
		} else {
			// Set alarm
			long triggerAtTimeMillis = Utils.secondsToMillis(nextRunTime);
			if (minPeriod == null) {
				manager.set(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, pi);
			} else {
				long minPeriodMillis = Utils.secondsToMillis(minPeriod);
				if (strict) {
					manager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, minPeriodMillis, pi);
				} else {
					manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, minPeriodMillis, pi);
				}
			}
			
		}
	}
	
	private void scheduleStop(Uri probeUri) {
		Double maxDuration = null;
		for (Map.Entry<DataListener,Double> listenerSatisfied : requestSatisfiedTimestamps.get(probeUri).entrySet()) {
			DataListener listener = listenerSatisfied.getKey();
			for (ProbeDataRequest request : requests.get(listener)) {
				BasicSchedule schedule = new BasicSchedule(Probe.Base.getProbeClass(Probe.Identifier.getProbeName(probeUri)), request.getSchedule());
				Double duration = schedule.getDuration();
				if (duration != null) {
					maxDuration = (maxDuration == null) ? duration : Math.max(maxDuration, duration);
				}
			}
		}
		
		Intent i = new Intent(ACTION_STOP_PROBE, probeUri);
		i.setClass(this, getClass());
		PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		long triggerAtTimeMillis = Utils.secondsToMillis(maxDuration);
		manager.set(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, pi);
	}


}
