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
import android.os.Binder;
import android.os.IBinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.DefaultSchedule;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.time.TimeUtil;

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
		ACTION_ENABLE_PASSIVE_PROBE = PREFIX + ".ENABLE",
		ACTION_START_PROBE = PREFIX + ".START",
		ACTION_STOP_PROBE = PREFIX + ".STOP",
		ACTION_DISABLE_PASSIVE_PROBE = PREFIX + ".DISABLE";
	
	private ProbeFactory cacheFactory;
	private Map<Probe.DataListener, Set<ProbeDataRequest>> requests;
	private Map<Uri,Map<Probe.DataListener,Double>> requestSatisfiedTimestamps; // Map used in place of set only for WeakRefs
	private AlarmManager manager;

	/**
	 * Binder interface to the probe
	 */
	public class LocalBinder extends Binder {
		public ProbeManager getService() {
			return ProbeManager.this;
		}
	}

	private final IBinder binder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
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
		super.onDestroy();
		Set<Uri> activeProbeUris = new HashSet<Uri>();
		for (Set<ProbeDataRequest> requestEntries : requests.values()) {
			for (ProbeDataRequest request : requestEntries) {
				activeProbeUris.add(request.getProbeUri());
			}
		}
		for (Uri probeUri : activeProbeUris) {
			cancelAlarm(probeUri);
			getProbe(probeUri).destroy();
		}
	}
	
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action == null) {
			
		} else if (ACTION_ENABLE_PASSIVE_PROBE.equals(action)
				|| ACTION_DISABLE_PASSIVE_PROBE.equals(action)
				|| ACTION_START_PROBE.equals(action)
				|| ACTION_STOP_PROBE.equals(action)) {
			Probe probe = getProbe(intent.getData());
			if (probe != null) {
				Map<Probe.DataListener,Double> timestamps = requestSatisfiedTimestamps.get(intent.getData());
				
				Set<DataListener> listeners = (timestamps == null) ? new HashSet<Probe.DataListener>() : timestamps.keySet();
				//listeners.add(this);  // TODO: enable this to be a listener, for triggers
				
				if (ACTION_ENABLE_PASSIVE_PROBE.equals(action) && probe instanceof PassiveProbe) {
					// TODO: clean this up, it is way to complicated
					Set<DataListener> opportunisticListeners = new HashSet<DataListener>();
					for (DataListener listener : listeners) {
						 Set<ProbeDataRequest> dataRequests = requests.get(listener);
						 boolean opportunistic = false;
						 for (ProbeDataRequest dataRequest : dataRequests) {
							 BasicSchedule schedule = new BasicSchedule(Probe.Base.getProbeClass(Probe.PROBE_URI.getName(intent.getData())), dataRequest.getSchedule());
							 opportunistic = opportunistic || schedule.isOpportunistic();
						 }
						 if (opportunistic) {
							 opportunisticListeners.add(listener);
						 }
					}
					DataListener[] opportunisticListenersArray = new DataListener[opportunisticListeners.size()];
					opportunisticListeners.toArray(opportunisticListenersArray);
					((PassiveProbe)probe).registerPassiveListener(opportunisticListenersArray);
				} else if (ACTION_DISABLE_PASSIVE_PROBE.equals(action) && probe instanceof PassiveProbe) {
					DataListener[] listenerArray = new DataListener[listeners.size()];
					listeners.toArray(listenerArray);
					((PassiveProbe)probe).unregisterPassiveListener(listenerArray);
				} else if (ACTION_START_PROBE.equals(action)) {
					DataListener[] listenerArray = new DataListener[listeners.size()];
					listeners.toArray(listenerArray);
					probe.registerListener(listenerArray);
					if (probe instanceof ContinuousProbe) {
						scheduleStop(intent.getData());
					}
				} else if (ACTION_STOP_PROBE.equals(action)  && probe instanceof ContinuousProbe) {
					DataListener[] listenerArray = new DataListener[listeners.size()];
					listeners.toArray(listenerArray);
					((ContinuousProbe)probe).unregisterListener(listenerArray);
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
	public <T extends Probe> T getProbe(Class<? extends T> probeClass, JsonObject config) {
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
			if (values != null) {
				JsonUtils.deepCopyOnto(values, schedule, true);
			}
			
			try {period = schedule.get("period").getAsDouble(); } catch (ClassCastException e) {} catch (IllegalStateException e) {} catch (NullPointerException e) {}
			try {duration = schedule.get("duration").getAsDouble(); } catch (ClassCastException e) {} catch (IllegalStateException e) {} catch (NullPointerException e) {}
			try {opportunistic = schedule.get("opportunistic").getAsBoolean(); } catch (ClassCastException e) {} catch (IllegalStateException e) {} catch (NullPointerException e) {}
			try {strict = schedule.get("strict").getAsBoolean(); } catch (ClassCastException e) {} catch (IllegalStateException e) {} catch (NullPointerException e) {}
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
	
	private void cancelAlarm(Uri probeUri) {
		for (String action : new String[] {ACTION_START_PROBE, ACTION_STOP_PROBE, ACTION_DISABLE_PASSIVE_PROBE, ACTION_ENABLE_PASSIVE_PROBE}) {
			Intent i = new Intent(action, probeUri);
			i.setClass(this, getClass());
			PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE);
			if (pi != null) {
				manager.cancel(pi);
			}
		}
	}
	
	private void schedule(Uri probeUri) {
		Class<? extends Probe> probeClass = Probe.Base.getProbeClass(Probe.PROBE_URI.getName(probeUri));
		
		// Figure out when the next time this probe needs to be run is
		Double nextRunTime = null;
		Double minPeriod = null;
		boolean strict = false;
		for (Map.Entry<DataListener,Double> listenerSatisfied : requestSatisfiedTimestamps.get(probeUri).entrySet()) {
			DataListener listener = listenerSatisfied.getKey();
			Double lastSatisfied = listenerSatisfied.getValue();
			if (lastSatisfied == null) {
				nextRunTime = TimeUtil.getTimestamp().doubleValue();
				break;
			} else {
				for (ProbeDataRequest request : requests.get(listener)) {
					BasicSchedule schedule = new BasicSchedule(Probe.Base.getProbeClass(Probe.PROBE_URI.getName(probeUri)), request.getSchedule());
					Double period = schedule.getPeriod();
					if (period != null && period != 0) {
						double requestNextRunTime = lastSatisfied + period;
						nextRunTime = (nextRunTime == null) ? requestNextRunTime : Math.min(nextRunTime, requestNextRunTime);
						minPeriod = (minPeriod == null) ? period :  Math.min(minPeriod, period);
						strict = strict || schedule.isStrict();
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
			long triggerAtTimeMillis = TimeUtil.secondsToMillis(nextRunTime);
			if (minPeriod == null) {
				manager.set(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, pi);
			} else {
				long minPeriodMillis = TimeUtil.secondsToMillis(minPeriod);
				if (strict) {
					manager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, minPeriodMillis, pi);
				} else {
					manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, minPeriodMillis, pi);
				}
			}
			
		}
	}
	
	private void scheduleStop(Uri probeUri) {
		Class<? extends Probe> probeClass = Probe.Base.getProbeClass(Probe.PROBE_URI.getName(probeUri));
		// Only continuous probes can be stopped
		if (ContinuousProbe.class.isAssignableFrom(probeClass)) {
			Double maxDuration = null;
			Map<DataListener,Double> timestamps = requestSatisfiedTimestamps.get(probeUri);
			if (timestamps == null) {
				timestamps = new HashMap<Probe.DataListener, Double>();
			}
			Map<DataListener,Double> listenerSatisfieds = requestSatisfiedTimestamps.get(probeUri);
			if (listenerSatisfieds != null) {
				for (Map.Entry<DataListener,Double> listenerSatisfied : listenerSatisfieds.entrySet()) {
					DataListener listener = listenerSatisfied.getKey();
					for (ProbeDataRequest request : requests.get(listener)) {
						BasicSchedule schedule = new BasicSchedule(probeClass, request.getSchedule());
						Double duration = schedule.getDuration();
						if (duration != null) {
							maxDuration = (maxDuration == null) ? duration : Math.max(maxDuration, duration);
						}
					}
				}
			}
			if (maxDuration != null) {
				Intent i = new Intent(ACTION_STOP_PROBE, probeUri);
				i.setClass(this, getClass());
				PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
				long triggerAtTimeMillis = TimeUtil.secondsToMillis(maxDuration);
				manager.set(AlarmManager.RTC_WAKEUP, triggerAtTimeMillis, pi);
			}
		}
	}


}
