package edu.mit.media.hd.funf.probe.builtin;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import edu.mit.media.hd.funf.probe.Probe;

/**
 * TODO: Figure out purpose for this probe
 * Record funf alarms?
 *
 */
public class AlarmProbe extends Probe {

	public static final String FUNF_ALARM_ACTION = "funf.intent.action.LOG";
	
	public static final String ALARM_ID = "ID";
	public static final String ALARM_POP_TIME = "POPTIME";
	public static final String ALARM_EVENT_TIME = "EVENTTIME";
	public static final String ALARM_ACTION = "ACTION";
	public static final String ALARM_REPEATING = "REPEATING";

	public static final int NO_ALARM_ID = -1;
	
	private AlarmReceiver receiver;
	private boolean hasData;
	private int alarmId;
	private long popTime;
	private long eventTime;
	private String action;
	private int repeating;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return null;
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}

	@Override
	protected void onEnable() {
		hasData = false;
		receiver = new AlarmReceiver();
		registerReceiver(receiver, new IntentFilter(FUNF_ALARM_ACTION));
	}
	
	@Override
	protected void onDisable() {
		unregisterReceiver(receiver);
	}

	@Override
	protected void onRun(Bundle params) {
		sendProbeData();
		stop();
	}

	@Override
	protected void onStop() {
		
	}

	@Override
	public void sendProbeData() {
		if (hasData) {
			Bundle data = new Bundle();
			data.putInt(ALARM_ID, alarmId);
			data.putLong(ALARM_POP_TIME, popTime);
			data.putLong(ALARM_EVENT_TIME, eventTime);
			data.putString(ALARM_ACTION, action);
			data.putInt(ALARM_REPEATING, repeating);
			sendProbeData(eventTime, new Bundle(), data);
		}
	}
	

	class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (FUNF_ALARM_ACTION.equals(intent.getAction())) {
				alarmId = intent.getIntExtra("_id", -1);
				popTime = intent.getLongExtra("poptime", -1);
				eventTime = intent.getLongExtra("eventtime", -1);
				action = intent.getStringExtra("action");
				repeating = getDaysOfWeek(context, alarmId);
				hasData = true;
				sendProbeData();
			}
		}
		
		private int getDaysOfWeek(Context context, int id) {
			ContentResolver resolver = context.getContentResolver();
			String[] projection = {"_id", "daysofweek"};
			Cursor cursor = resolver.query(Uri.parse("content://com.hlidskialf.android.alarmclock/alarm"), projection, null, null, "_id ASC");
			cursor.moveToFirst();
			for (int i = 0; i < cursor.getCount(); i++) {
				if (cursor.getInt(cursor.getColumnIndex("_id")) == id) {
					return cursor.getInt(cursor.getColumnIndex("daysofweek"));
				} else continue;
			}
			return -1;
		}
	}
}
