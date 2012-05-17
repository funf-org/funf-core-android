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
package edu.mit.media.funf;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;

import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.ContextInjectorTypeAdapaterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultScheduleSerializer;
import edu.mit.media.funf.config.SingletonTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.time.TimeUtil;

public class FunfManager extends Service {
	
	public static final String 
	ACTION_KEEP_ALIVE = "funf.keepalive",
	ACTION_INTERNAL = "funf.internal";
	
	private static final String 
	PROBE_TYPE = "funf/probe",
	PIPELINE_TYPE = "funf/pipeline";
	
	private static final String 
	PROBE_ACTION_REGISTER = "register",
	PROBE_ACTION_UNREGISTER = "unregister",
	PROBE_ACTION_REGISTER_PASSIVE = "register-passive",
	PROBE_ACTION_UNREGISTER_PASSIVE = "unregister-passive";
	
	private Handler handler;
	private JsonParser parser;
	private Map<String,Pipeline> pipelines;
	private Map<IJsonObject,List<DataRequestInfo>> dataRequests; 	
	private class DataRequestInfo {
		private DataListener listener;
		private Schedule schedule;
		private BigDecimal lastSatisfied;
	}
	
	// TODO: triggers
	
	// Maybe instances of probes are different from other, and are created in manager
	
	private Scheduler scheduler;

	@Override
	public void onCreate() {
		super.onCreate();
		this.parser = new JsonParser();
		this.scheduler = new Scheduler();
		this.handler = new Handler();
		getGson(); // Sets gson
		this.dataRequests = new HashMap<IJsonObject, List<DataRequestInfo>>();
		this.pipelines = new HashMap<String, Pipeline>();
		// TODO: load data requests from disk?  or just do pipelines
		
		// Load stored pipeline config


		// TODO: bootstrap from meta parameters if pipelines don't exist
		Bundle metadata = getMetadata();
		for (String keyName : metadata.keySet()) {
			// Determine if resource or value, If resource get value
			// Parse value into JsonElement
			// If JsonString create uri and (down)load file and parse for pipleine config
			// If JsonObject use as a pipeline instance config
		}
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// TODO: call onDestroy on all pipelines
		for (Pipeline pipeline : pipelines.values()) {
			pipeline.onDestroy();
		}
		
		// TODO: save outstanding requests
		// TODO: remove all remaining Alarms

		// TODO: make sure to destroy all probes
		for (Object probeObject : getProbeFactory().getCached()) {
			String componentString = JsonUtils.immutable(gson.toJsonTree(probeObject)).toString();
			cancelProbe(componentString);
			((Probe)probeObject).destroy();
		}
		getProbeFactory().clearCache();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
			// Does nothing, but wakes up FunfManager
		} else if (ACTION_INTERNAL.equals(action)) {
			String type = intent.getType();
			Uri componentUri = intent.getData();
			if (PROBE_TYPE.equals(type)) {
				// Handle probe action
				IJsonObject probeConfig = (IJsonObject)JsonUtils.immutable(parser.parse(getComponentName(componentUri)));
				String probeAction = getAction(componentUri);
				
				BigDecimal now = TimeUtil.getTimestamp();
				final Probe probe = getGson().fromJson(probeConfig, Probe.class); 
				List<DataRequestInfo> requests = dataRequests.get(probeConfig);
				
				// TODO: Need to allow for some listeners to be registered and unregistered on different schedules
				if (probe != null) {
	 				if (PROBE_ACTION_REGISTER.equals(probeAction)) {
	 					if (requests != null) {
		 					List<DataListener> listenersThatNeedData = new ArrayList<Probe.DataListener>();
		 					List<DataRequestInfo> infoForListenersThatNeedData = new ArrayList<FunfManager.DataRequestInfo>();
	 						for (DataRequestInfo requestInfo : requests) {
	 							BigDecimal interval = requestInfo.schedule.getInterval();
	 							// Compare date last satisfied to schedule interval
	 							if (requestInfo.lastSatisfied == null || now.subtract(requestInfo.lastSatisfied).compareTo(interval) >= 0) {
	 								listenersThatNeedData.add(requestInfo.listener);
	 								infoForListenersThatNeedData.add(requestInfo);
	 							}
	 						}
	 						final DataListener[] listenerArray = new DataListener[listenersThatNeedData.size()];
	 						listenersThatNeedData.toArray(listenerArray);
		 					if (listenerArray.length > 0) {
		 						probe.registerListener(listenerArray);
		 					}
		 					
		 					// Schedule unregister if continuous
		 					// TODO: do different durations for each schedule
		 					if (probe instanceof ContinuousProbe) {
		 						Schedule mergedSchedule = getMergedSchedule(infoForListenersThatNeedData);
		 						if (mergedSchedule != null) {
		 							handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											((ContinuousProbe) probe).unregisterListener(listenerArray);
										}
									}, TimeUtil.secondsToMillis(mergedSchedule.getDuration()));
		 						}
		 					}
	 					}
					} else if (PROBE_ACTION_UNREGISTER.equals(probeAction) && probe instanceof ContinuousProbe) {
						for (DataRequestInfo requestInfo : requests) {
							((ContinuousProbe)probe).unregisterListener(requestInfo.listener);
						}
					} else if (PROBE_ACTION_REGISTER_PASSIVE.equals(probeAction) && probe instanceof PassiveProbe) {
	 					if (requests != null) {
	 						for (DataRequestInfo requestInfo : requests) {
	 							if (requestInfo.schedule.isOpportunistic()) {
	 								((PassiveProbe)probe).registerPassiveListener(requestInfo.listener);
	 							}
	 						}
	 					}
					} else if (PROBE_ACTION_UNREGISTER_PASSIVE.equals(probeAction) && probe instanceof PassiveProbe) {
	 					if (requests != null) {
	 						for (DataRequestInfo requestInfo : requests) {
	 							((PassiveProbe)probe).unregisterPassiveListener(requestInfo.listener);
	 						}
	 					}
					}
				}

				// TODO: Calculate new schedule for probe
			} else if (PIPELINE_TYPE.equals(type)) {
				// Handle pipeline action
				String pipelineName = getComponentName(componentUri);
				String pipelineAction = getAction(componentUri);
				Pipeline pipeline = pipelines.get(pipelineName);
				if (pipeline != null) {
					pipeline.onRun(pipelineAction, null);
				}
			}

		}
		return Service.START_FLAG_RETRY; // TODO: may want the last intent always redelivered to make sure system starts up
	}

	private Bundle getMetadata() {
		try {
			Bundle metadata = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA).metaData;
			return metadata == null ? new Bundle() : metadata;
		} catch (NameNotFoundException e) {
			throw new RuntimeException("Unable to get metadata for the FunfManager service.");
		}
	}
	
	/**
	 * Get a gson builder with the probe factory built in
	 * @return
	 */
	public GsonBuilder getGsonBuilder() {
		return getGsonBuilder(this);
	}
	
	/**
	 * Get a gson builder with the probe factory built in
	 * @return
	 */
	public static GsonBuilder getGsonBuilder(Context context) {
		return new GsonBuilder()
		.registerTypeAdapterFactory(getProbeFactory(context))
		.registerTypeAdapterFactory(getPipelineFactory(context))
		.registerTypeAdapterFactory(new DefaultRuntimeTypeAdapterFactory<Schedule>(context, Schedule.class, BasicSchedule.class))
		.registerTypeAdapter(DefaultSchedule.class, new DefaultScheduleSerializer())
		.registerTypeAdapter(Class.class, new JsonSerializer<Class<?>>() {

			@Override
			public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
				return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.getName());
			}
		});
	}
	
	private Gson gson;
	/**
	 * Get a Gson instance which includes the SingletonProbeFactory
	 * @return
	 */
	public Gson getGson() {
		if (gson == null) {
			gson = getGsonBuilder().create();
		}
		return gson;
	}
	
	public TypeAdapterFactory getPipelineFactory() {
		return getPipelineFactory(this);
	}
	
	private static PipelineFactory PIPELINE_FACTORY;
	public static PipelineFactory getPipelineFactory(Context context) {
		if (PIPELINE_FACTORY == null) {
			PIPELINE_FACTORY = new PipelineFactory(context);
		}
		return PIPELINE_FACTORY;
	}
	
	public SingletonTypeAdapterFactory getProbeFactory() {
		return getProbeFactory(this);
	}
	
	private static SingletonTypeAdapterFactory PROBE_FACTORY;
	public static SingletonTypeAdapterFactory getProbeFactory(Context context) {
		if (PROBE_FACTORY == null) {
			PROBE_FACTORY = new SingletonTypeAdapterFactory(
					new DefaultRuntimeTypeAdapterFactory<Probe>(
							context, 
							Probe.class, 
							null, 
							new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
		}
		return PROBE_FACTORY;
	}
	
	// triggers
	// FunfManager should allow you to register for triggers
	// Scheduler will register for triggers
	
	public void registerPipeline(String name, Pipeline pipeline) {
		synchronized (pipelines) {
			unregisterPipeline(name);
			pipelines.put(name, pipeline);
			pipeline.onCreate(this);
		}
	}
	
	public Pipeline getRegisteredPipeline(String name) {
		return pipelines.get(name);
	}
	
	public void unregisterPipeline(String name) {
		Pipeline existingPipeline = pipelines.get(name);
		if (existingPipeline != null) {
			existingPipeline.onDestroy();
		}
	}
	
	public void requestData(DataListener listener, JsonElement probeConfig) {
		requestData(listener, probeConfig, null);
	}
	
	public void requestData(DataListener listener, JsonElement probeConfig, Schedule schedule) {
		if (probeConfig == null) {
			throw new IllegalArgumentException("Probe config cannot be null");
		}
		// Use schedule in probeConfig @schedule annotation
		Probe probe = gson.fromJson(probeConfig, Probe.class);
		if (schedule == null) {
			DefaultSchedule defaultSchedule = probe.getClass().getAnnotation(DefaultSchedule.class);
			JsonObject scheduleObject = defaultSchedule == null ? new JsonObject() : gson.toJsonTree(defaultSchedule, DefaultSchedule.class).getAsJsonObject();
			if (probeConfig.isJsonObject() && probeConfig.getAsJsonObject().has(PipelineFactory.SCHEDULE)) {
				JsonUtils.deepCopyOnto(probeConfig.getAsJsonObject().get(PipelineFactory.SCHEDULE).getAsJsonObject(), scheduleObject, true);
			}
			schedule = gson.fromJson(scheduleObject, Schedule.class);
		}
		IJsonObject completeProbeConfig = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));  // Make sure probe config is complete and consistent
		requestData(listener, completeProbeConfig, schedule);
	}
	
	private void requestData(DataListener listener, IJsonObject completeProbeConfig, Schedule schedule) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener cannot be null");
		}
		if (completeProbeConfig == null) {
			throw new IllegalArgumentException("Probe config cannot be null");
		}
		DataRequestInfo newDataRequest = new DataRequestInfo();
		newDataRequest.lastSatisfied = null;
		newDataRequest.listener = listener;
		newDataRequest.schedule = schedule;
		synchronized (dataRequests) {
			List<DataRequestInfo> requests = dataRequests.get(completeProbeConfig);
			if (requests == null) {
				requests = new ArrayList<FunfManager.DataRequestInfo>();
				dataRequests.put(completeProbeConfig, requests);
			}
			unrequestData(listener, completeProbeConfig);
			requests.add(newDataRequest);
		}
		rescheduleProbe(completeProbeConfig);
	}
	
	public void unrequestData(DataListener listener, JsonElement probeConfig) {
		Probe probe = gson.fromJson(probeConfig, Probe.class);
		IJsonObject completeProbeConfig = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));  // Make sure probe config is complete and consistent
		unrequestData(listener, completeProbeConfig);
		rescheduleProbe(completeProbeConfig);
	}
	
	/**
	 * This version does not reschedule.
	 * @param listener
	 * @param completeProbeConfig
	 */
	private void unrequestData(DataListener listener, IJsonObject completeProbeConfig) {
		synchronized (dataRequests) {
			List<DataRequestInfo> requests = dataRequests.get(completeProbeConfig);
			Probe probe = gson.fromJson(completeProbeConfig, Probe.class);
			for (int i = 0; i < requests.size(); i++) {
				if (requests.get(i).listener == listener) {
					requests.remove(i);
					if (probe instanceof ContinuousProbe) {
						((ContinuousProbe)probe).unregisterListener(listener);
					}
					if (probe instanceof PassiveProbe) {
						((PassiveProbe)probe).unregisterPassiveListener(listener);
					}
					break; // Should only have one request for this listener and probe
				}
			}
		}
	}
	
	private Schedule getMergedSchedule(List<DataRequestInfo> requests) {
		BasicSchedule mergedSchedule = null;
		for (DataRequestInfo request: requests) {
			if (mergedSchedule == null) {
				mergedSchedule = new BasicSchedule(request.schedule);
			} else {
				// Min interval
				mergedSchedule.setInterval(mergedSchedule.getInterval().min(request.schedule.getInterval()));
				// Max duration
				mergedSchedule.setDuration(mergedSchedule.getDuration().max(request.schedule.getDuration()));
				// Strict if one is strict
				mergedSchedule.setStrict(mergedSchedule.isStrict() || request.schedule.isStrict());
			}
		}
		return mergedSchedule;
	}
	
	private void rescheduleProbe(IJsonObject completeProbeConfig) {
		synchronized (dataRequests) {
			// Simple schedule merge for now
			// TODO: make this more efficient
			String componentString = completeProbeConfig.toString();
			List<DataRequestInfo> requests = dataRequests.get(completeProbeConfig);
			if (requests.isEmpty()) {
				cancelProbe(componentString);
			} else {
				Schedule mergedSchedule = getMergedSchedule(requests);
				for (DataRequestInfo request: requests) {
					// Schedule passive listening if opportunistic
					if (request.schedule.isOpportunistic()) {
						Probe probe = gson.fromJson(completeProbeConfig, Probe.class);
						if (probe instanceof PassiveProbe) {
							((PassiveProbe)probe).registerPassiveListener(request.listener);
						}
					}
				}
				scheduler.set(PROBE_TYPE, getComponenentUri(componentString, PROBE_ACTION_REGISTER), mergedSchedule);
			}
		}
	}
	
	public class LocalBinder extends Binder {
		public FunfManager getManager() {
			return FunfManager.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}
	
	/////////////////////////////////////////////
	// Reserve action for later inter-funf communication
	// Use type to differentiate between probe/pipeline
	// funf:<componenent_name>#<action>
	
	public static final String 
		FUNF_SCHEME = "funf";

	public static Uri getComponenentUri(String component, String action) {
		return new Uri.Builder()
		.scheme(FUNF_SCHEME)
		.path(component) // Automatically prepends slash
		.fragment(action)
		.build();
	}
	
	public static String getComponentName(Uri componentUri) {
		return componentUri.getPath().substring(1); // Remove automatically prepended slash from beginning
	}
	
	public static String getAction(Uri componentUri) {
		return componentUri.getFragment();
	}
	
	public static Intent getFunfIntent(Context context, String type, String component, String action) {
		return getFunfIntent(context, type, getComponenentUri(component, action));
	}
	
	public static Intent getFunfIntent(Context context, String type, Uri componentUri) {
		Intent intent = new Intent();
		intent.setClass(context, FunfManager.class);
		intent.setPackage(context.getPackageName());
		intent.setAction(ACTION_INTERNAL);
		intent.setDataAndType(componentUri, type);
		return intent;
	}
	

	private void cancelProbe(String probeConfig) {
		scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_REGISTER));
		scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_UNREGISTER));
		scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_REGISTER_PASSIVE));
		scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_UNREGISTER_PASSIVE));
	}
	
	////////////////////////////////////////////////////


	public Scheduler getScheduler() {
		return scheduler;
	}
	
	public class Scheduler {
	
		private AlarmManager alarmManager;
		private Context context;
		
		// private Map<Pipeline,Config,Schedule>
		// Need to be able to merge schedules for common types quickly, across pipelines
		// private Map<Config,<Schedule,Pipeline>>
		
		// Use factory to build data listeners from gson?
		// Or just grab data listener from pipeline
		// What about running other operations?  Should they all just have a run/start and maybe stop?
		
		// IDEA:
		// Send config back to pipeline, let it decide how to handle it.
		
		
		public Scheduler() {
			this.alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			this.context = FunfManager.this;
		}
		
		
		public void cancel(String type, Uri componentAndAction) {
			Intent intent = getFunfIntent(context, type, componentAndAction);
			PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
			if (operation != null) {
				operation.cancel();
			}
		}
		
		public void cancel(String type, String component, String action) {
			cancel(type, getComponenentUri(component, action));
		}
		
		public void set(String type, String component, String action, Schedule schedule) {
			set(type, getComponenentUri(component, action), schedule);
		}
		
		public void set(String type, Uri componentAndAction, Schedule schedule) {
			
			// Creates pending intents that will call back into FunfManager
			// Uses alarm manager to time them
			Intent intent = getFunfIntent(context, type, componentAndAction);
			PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			Number previousTime = null;
			
			// TODO: add random start for initial
			// startTimeMillis += random;
			

			BigDecimal startTime = schedule.getNextTime(previousTime);
			if (startTime != null) {
				long startTimeMillis = TimeUtil.secondsToMillis(startTime);
				if (schedule.getInterval() == null) {
					alarmManager.set(AlarmManager.RTC_WAKEUP, startTimeMillis, operation);
				} else {
					long intervalMillis = TimeUtil.secondsToMillis(schedule.getInterval());
					if (schedule.isStrict()) {
						alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, intervalMillis, operation);
					} else {
						alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, intervalMillis, operation);
					}
				}
			}
						
		}
		
		// TODO: Feature to wait a certain amount of seconds after boot to begin
		// TODO: Feature to prevent too many things from running at once, w/ random backoff times
		
		
		// All are in timestamp seconds
		// PARAMS
		// time (time to run, may not need this for now)
		// strict (do we need wakeup as a separate parameter, or are all wakeup?)
		// interval (period)
		// duration (start to stop time)
		// opportunistic (For probes, means use other probe being run as an excuse to run this one, not sure what it means for others, its possible this won't be part of scheduling)
		
		
		
		// Schedule runnables
		// Data request would be able to contain
		
		// Pipeline could provide data listener for probes
		// Pipeline could provide mechanism for creating objects to RUN!!!
		
		// Configuration, is loaded into pipeline which creates the object to run
		// This allows separate probes if required
		// Allows creation of database services to be determined by pipeline
	}
}
