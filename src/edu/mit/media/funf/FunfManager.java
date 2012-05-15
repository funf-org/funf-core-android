package edu.mit.media.funf;

import java.math.BigDecimal;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapterFactory;

import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.SingletonTypeAdapterFactory;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
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
	
	
	// Entry point for scheduling probes and other configurable funf services
	/*
	public Gson getFactoryGson() {
		// Want singletons across pipelines
	}
	
	public Scheduler getScheduler() {
		
	}
	*/
	
	// TODO: opportunistic
	// TODO: triggers
	
	// Maybe instances of probes are different from other, and are created in manager
	
	private Scheduler scheduler;

	@Override
	public void onCreate() {
		super.onCreate();
		this.scheduler = new Scheduler();
		// TODO: bootstrap from meta parameters
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// TODO: call onDestroy on all pipelines
		// TODO: remove all remaining Alarms
		// TODO: make sure to destroy all probes
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
			// Does nothing, but wakes up FunfManager
		} else if (ACTION_INTERNAL.equals(action)) {
			// TODO: handle internal actions, pipelines and probes
		}
		return Service.START_FLAG_RETRY; // TODO: may want the last intent always redilvered to make sure system starts up
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
		.registerTypeAdapterFactory(new DefaultRuntimeTypeAdapterFactory<Schedule>(context, Schedule.class, BasicSchedule.class));
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
							new ConfigurableTypeAdapterFactory()));
		}
		return PROBE_FACTORY;
	}
	
	// triggers
	// FunfManager should allow you to register for triggers
	// Scheduler will register for triggers
	
	public void registerPipeline(Pipeline pipeline) {
		
	}
	
	public void unregisterPipeline(Pipeline pipeline) {
		
	}
	
	public void requestData(DataListener listener, JsonElement probeConfig) {
		
	}
	
	public void unrequestData(DataListener listener, JsonElement probeConfig) {
		
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
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
		.path(component)
		.fragment(action)
		.build();
	}
	
	public static String getComponentName(Uri componentUri) {
		return componentUri.getPath();
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
		intent.setType(type);
		intent.setData(componentUri);
		return intent;
	}
	////////////////////////////////////////////////////


	public Scheduler getScheduler() {
		return scheduler;
	}
	
	public class Scheduler {
		
		// TODO: need place to merge schedules before registering alarms
	
		private AlarmManager alarmManager;
		private Context context;
		
		// This may belong somewhere else
		private Map<Uri,Number> componentActionToLastSatisfied;
		
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
			// If schedule is null
			// TODO: Will use "@schedule" param on json config, filling in missing parts w/ DefaultSchedule annotation of type
			// If none exists will use DefaultSchedule annotation on 
			// if none exists will run once
			
			// Creates pending intents that will call back into FunfManager
			// Uses alarm manager to time them
			Intent intent = getFunfIntent(context, type, componentAndAction);
			PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			Number previousTime = null;
			boolean scheduleStop = false;
			
	
			
			// TODO: schedule stop
			
			// TODO: add random start for initial
			// startTimeMillis += random;
			

			BigDecimal startTime = schedule.getNextTime(previousTime);
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
