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
package edu.mit.media.funf.probe.builtin;

import android.net.Uri;

public class ProbeKeys {
	
	public static interface StatusKeys {
		public static final String 
		ENABLED = "ENABLED",
		RUNNING = "RUNNING",
		NEXT_RUN = "NEXT_RUN",
		PREVIOUS_RUN = "PREVIOUS_RUN",
		NAME = "NAME",
		DISPLAY_NAME = "DISPLAY_NAME",
		REQUIRED_PERMISSIONS = "REQUIRED_PERMISSIONS",
		REQUIRED_FEATURES = "REQUIRED_FEATURES",
		PARAMETERS = "PARAMETERS";
	}
	
	public static interface BaseProbeKeys {
		public static final String 
		PROBE = "probe",
		// TODO: add probe version
		TIMESTAMP = "timestamp";
	}
	
	public static interface SensorKeys extends BaseProbeKeys {
		public static final String 
		MAXIMUM_RANGE = "maximumRange",
		NAME = "name",
		POWER = "power",
		RESOLUTION = "resolution",
		TYPE = "type",
		VENDOR = "vendor",
		VERSION = "version",
		SENSOR = "sensor",
		EVENT_TIMESTAMP = "eventTimestamp",
		ACCURACY = "accuracy";
	}
	
	public static interface AccelerometerFeaturesKeys extends BaseProbeKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z",
		DIFF_FRAME_SECS = "diffFrameSecs",
		NUM_FRAME_SAMPLES = "numFrameSamples",
		MEAN = "mean",
		ABSOLUTE_CENTRAL_MOMENT = "absoluteCentralMoment",
		STANDARD_DEVIATION = "standardDeviation",
		MAX_DEVIATION = "maxDeviation",
		PSD_ACROSS_FREQUENCY_BANDS = "psdAcrossFrequencyBands";
	}
	
	public static interface AccelerometerSensorKeys extends SensorKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z";	
	}
	
	public static interface ActivityKeys extends BaseProbeKeys {
		public static final String 
		ACTIVITY_LEVEL = "activityLevel",
		ACTIVITY_LEVEL_NONE = "none",
		ACTIVITY_LEVEL_LOW = "low",
		ACTIVITY_LEVEL_HIGH = "high";
	}
	
	public static interface AndroidInfoKeys extends BaseProbeKeys {
		public static final String 
		FIRMWARE_VERSION = "firmwareVersion",
		BUILD_NUMBER = "buildNumber",
		SDK = "sdk";
	}
	
	public static interface ApplicationsKeys extends BaseProbeKeys {
		public static final String 
		INSTALLED_APPLICATIONS = "installedApplications",
		UNINSTALLED_APPLICATIONS = "uninstalledApplications",
		INSTALLED = "installed",
		INSTALLED_TIMESTAMP = "installedTimestamp";
	}
	
	public static interface AudioFeaturesKeys extends BaseProbeKeys {
		public static final String 
		DIFF_SECS = "diffSecs",
		L1_NORM = "l1Norm",
		L2_NORM = "l2Norm",
		LINF_NORM = "linfNorm",
		PSD_ACROSS_FREQUENCY_BANDS = "psdAcrossFrequencyBands",
		MFCCS = "mfccs";
	}
	
	public static interface AudioFilesKeys extends BaseProbeKeys, android.provider.MediaStore.Audio.AudioColumns {
		public static final String AUDIO_FILES = "audioFiles";
	}
	
	public static interface BatteryKeys extends BaseProbeKeys {
		public static final String 
		ICON_SMALL = "iconSmall",
		PRESENT = "present",
		SCALE = "scale",
		LEVEL = "level",
		TECHNOLOGY = "technology",
		STATUS = "status",
		VOLTAGE = "voltage",
		HEALTH = "health",
		TEMPERATURE = "temperature";
	}
	
	public static interface BluetoothKeys extends BaseProbeKeys {
		public static final String 
		DEVICES = "devices";
	}
	
	public static class BrowserBookmarksKeys extends android.provider.Browser.BookmarkColumns implements BaseProbeKeys {
		public static final String BOOKMARKS = "bookmarks";
	}
	
	public static class BrowserSearchesKeys extends android.provider.Browser.SearchColumns implements BaseProbeKeys {
		public static final String SEARCHES = "searches";
	}
	
	public static class CallLogKeys extends android.provider.CallLog.Calls implements BaseProbeKeys {
		public static final String CALLS = "calls";
	}
	
	public static interface CellKeys extends BaseProbeKeys {
		public static final String
		TYPE = "type",
		PSC = "psc",
		CID = "cid",
		LAC = "lac";
		// TODO: fill with cdma data keys
		// baseStationId
		// baseStationLatitude
		// baseStationLongitude
		// networkId
		// systemId
	}
	
	public static interface ContactKeys extends BaseProbeKeys {
		public static final String CONTACT_DATA = "contactData";
		// Cannot extend android final class android.provider.ContactsContract.Data, or inherit from protected interfaces
	}
	
	public static interface GravitySensorKeys extends SensorKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z";	
	}
	
	public static interface GyroscopeSensorKeys extends SensorKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z";	
	}
	
	public static interface HardwareInfoKeys extends BaseProbeKeys {
		public static final String 
		WIFI_MAC = "wifiMac", 
		BLUETOOTH_MAC = "bluetoothMac", 
		ANDROID_ID = "androidId",
		BRAND = "brand", 
		MODEL = "model", 
		DEVICE_ID = "deviceId";	
	}
		
	public static interface ImagesKeys extends BaseProbeKeys, android.provider.MediaStore.Images.ImageColumns {
		public static final String 
		IMAGES = "images";	
	}
	
	public static interface LightSensorKeys extends SensorKeys {
		public static final String 
		LUX = "lux";	
	}
	
	public static interface LinearAccelerationSensorKeys extends SensorKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z";	
	}
	
	public static interface LocationKeys extends BaseProbeKeys {
		public static final String 
		ACCURACY = "mAccuracy",
		LATITUDE = "mLatitude",
		LONGITUDE= "mLongitude";
	}
	
	public static interface MagneticFieldSensorKeys extends SensorKeys {
		public static final String 
		X = "x", 
		Y = "y", 
		Z = "z";
	}
	
	public static interface OrientationSensorKeys extends SensorKeys {
		public static final String 
		AZIMUTH = "azimuth", 
		PITCH = "pitch", 
		ROLL = "roll";
	}
	
	public static interface PressureSensorKeys extends SensorKeys {
		public static final String 
		PRESSURE = "pressure";
	}
	
	public static interface ProximitySensorKeys extends SensorKeys {
		public static final String 
		DISTANCE = "distance";
	}
	
	public static interface RotationVectorSensorKeys extends SensorKeys {
		public static final String 
		X_SIN_THETA_OVER_2 = "xSinThetaOver2", 
		Y_SIN_THETA_OVER_2 = "ySinThetaOver2", 
		Z_SIN_THETA_OVER_2 = "zSinThetaOver2",
		COS_THETA_OVER_2 = "cosThetaOver2";
	}
	
	public static interface RunningApplicationsKeys extends BaseProbeKeys {
		public static final String 
		RUNNING_TASKS = "runningTasks",
		DURATION = "duration",
		TASK_INFO = "taskInfo";
	}
	
	public static interface ScreenKeys extends BaseProbeKeys {
		public static final String 
		SCREEN_ON = "screenOn";
	}
	
	public static interface SmsKeys extends BaseProbeKeys, AndroidInternal.TextBasedSmsColumns {
		// public static final String 
	}
	
	public static interface TelephonyKeys extends BaseProbeKeys {
		public static final String 
		CALL_STATE = "callState",
		DEVICE_ID = "deviceId",
		DEVICE_SOFTWARE_VERSION = "deviceSoftwareVersion",
		LINE_1_NUMBER = "line1Number",
		NETWORK_COUNTRY_ISO = "networkCountryIso",
		NETWORK_OPERATOR = "networkOperator",
		NETWORK_OPERATOR_NAME = "networkOperatorName",
		NETWORK_TYPE = "networkType",
		PHONE_TYPE = "phoneType",
		SIM_COUNTRY_ISO = "simCountryIso",
		SIM_OPERATOR = "simOperator",
		SIM_OPERATOR_NAME = "simOperatorName",
		SIM_SERIAL_NUMBER = "simSerialNumber",
		SIM_STATE = "simState",
		SUBSCRIBER_ID = "subscriberId",
		VOICEMAIL_ALPHA_TAG = "voicemailAlphaTag",
		VOICEMAIL_NUMBER = "voicemailNumber",
		HAS_ICC_CARD = "hassIccCard";
	}
	
	public static interface TemperatureSensorKeys extends SensorKeys {
		public static final String 
		TEMPERATURE = "temperature";
	}
	
	public static interface TimeOffsetKeys extends BaseProbeKeys {
		public static final String 
		LOCAL_TIME_OFFSET = "localTimeOffset",
		ROUND_TRIP_DELAY = "roundTripDelay";
	}
	
	public static interface VideosKeys extends BaseProbeKeys, android.provider.MediaStore.Video.VideoColumns {
		public static final String 
		VIDEOS = "videos";	
	}
	
	public static interface WifiKeys extends BaseProbeKeys {
		public static final String 
		SCAN_RESULTS = "scanResults";
	}
	
	public static interface ServicesKeys extends BaseProbeKeys {
		//public static final String 
	}
	
	public static interface AccountsKeys extends BaseProbeKeys {
		public static final String 
		ACCOUNTS = "accounts",
		NAME = "name",
		TYPE = "type";
	}
	
	public interface AndroidInternal {
	
		///////////////////////////////////////////
		// COPIED FROM NON-PUBLIC ANDROID API
		// http://www.google.com/codesearch/p?hl=en#fxuXIzvA0aY/core/java/android/provider/Telephony.java&q=package:android%20%22core/java/android/provider/Telephony.java%22&sa=N&cd=1&ct=rc&l=186
	
		public interface Sms extends TextBasedSmsColumns {
			public static final Uri CONTENT_URI = Uri.parse("content://sms");
			public static final String MESSAGES = "messages";
			/* NOTE: other fields and methods not copied */
		}
		
		/**
	     * Base columns for tables that contain text based SMSs.
	     */
	    public interface TextBasedSmsColumns {
	        /**
	         * The type of the message
	         * <P>Type: INTEGER</P>
	         */
	        public static final String TYPE = "type";
	
	        public static final int MESSAGE_TYPE_ALL    = 0;
	        public static final int MESSAGE_TYPE_INBOX  = 1;
	        public static final int MESSAGE_TYPE_SENT   = 2;
	        public static final int MESSAGE_TYPE_DRAFT  = 3;
	        public static final int MESSAGE_TYPE_OUTBOX = 4;
	        public static final int MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
	        public static final int MESSAGE_TYPE_QUEUED = 6; // for messages to send later
	
	
	        /**
	         * The thread ID of the message
	         * <P>Type: INTEGER</P>
	         */
	        public static final String THREAD_ID = "thread_id";
	
	        /**
	         * The address of the other party
	         * <P>Type: TEXT</P>
	         */
	        public static final String ADDRESS = "address";
	
	        /**
	         * The person ID of the sender
	         * <P>Type: INTEGER (long)</P>
	         */
	        public static final String PERSON_ID = "person";
	
	        /**
	         * The date the message was sent
	         * <P>Type: INTEGER (long)</P>
	         */
	        public static final String DATE = "date";
	
	        /**
	         * Has the message been read
	         * <P>Type: INTEGER (boolean)</P>
	         */
	        public static final String READ = "read";
	
	        /**
	         * Indicates whether this message has been seen by the user. The "seen" flag will be
	         * used to figure out whether we need to throw up a statusbar notification or not.
	         */
	        public static final String SEEN = "seen";
	
	        /**
	         * The TP-Status value for the message, or -1 if no status has
	         * been received
	         */
	        public static final String STATUS = "status";
	
	        public static final int STATUS_NONE = -1;
	        public static final int STATUS_COMPLETE = 0;
	        public static final int STATUS_PENDING = 32;
	        public static final int STATUS_FAILED = 64;
	
	        /**
	         * The subject of the message, if present
	         * <P>Type: TEXT</P>
	         */
	        public static final String SUBJECT = "subject";
	
	        /**
	         * The body of the message
	         * <P>Type: TEXT</P>
	         */
	        public static final String BODY = "body";
	
	        /**
	         * The id of the sender of the conversation, if present
	         * <P>Type: INTEGER (reference to item in content://contacts/people)</P>
	         */
	        public static final String PERSON = "person";
	
	        /**
	         * The protocol identifier code
	         * <P>Type: INTEGER</P>
	         */
	        public static final String PROTOCOL = "protocol";
	
	        /**
	         * Whether the <code>TP-Reply-Path</code> bit was set on this message
	         * <P>Type: BOOLEAN</P>
	         */
	        public static final String REPLY_PATH_PRESENT = "reply_path_present";
	
	        /**
	         * The service center (SC) through which to send the message, if present
	         * <P>Type: TEXT</P>
	         */
	        public static final String SERVICE_CENTER = "service_center";
	
	        /**
	         * Has the message been locked?
	         * <P>Type: INTEGER (boolean)</P>
	         */
	        public static final String LOCKED = "locked";
	
	        /**
	         * Error code associated with sending or receiving this message
	         * <P>Type: INTEGER</P>
	         */
	        public static final String ERROR_CODE = "error_code";
	
	        /**
	         * Meta data used externally.
	         * <P>Type: TEXT</P>
	         */
	        public static final String META_DATA = "meta_data";
	    }
	}
}
