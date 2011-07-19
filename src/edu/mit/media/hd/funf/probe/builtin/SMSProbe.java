package edu.mit.media.hd.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import edu.mit.media.hd.funf.probe.CursorCell;
import edu.mit.media.hd.funf.probe.DatedContentProviderProbe;

// TODO: May need to send text messages individually because bundle size may get too big for full scan
public class SMSProbe extends DatedContentProviderProbe {

	public static final String MESSAGES = "MESSAGES";
	
	@Override
	protected Uri getContentProviderUri() {
		return Sms.CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Sms.DATE;
	}

	@Override
	protected String getDataName() {
		return MESSAGES;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Sms.TYPE, intCell());
		projectionMap.put(Sms.THREAD_ID, intCell());
		projectionMap.put(Sms.ADDRESS, hashedStringCell()); // TODO: figure out if we have to normalize this first (maybe phone number)
		projectionMap.put(Sms.PERSON_ID, longCell());
		projectionMap.put(Sms.DATE, longCell());
		projectionMap.put(Sms.READ, booleanCell());
		projectionMap.put(Sms.SEEN, booleanCell());
		projectionMap.put(Sms.STATUS, intCell());
		projectionMap.put(Sms.SUBJECT, hashedStringCell());
		projectionMap.put(Sms.BODY, hashedStringCell());
		projectionMap.put(Sms.PERSON, hashedStringCell());
		projectionMap.put(Sms.PROTOCOL, intCell());
		projectionMap.put(Sms.REPLY_PATH_PRESENT, booleanCell());
		projectionMap.put(Sms.SERVICE_CENTER, stringCell());
		projectionMap.put(Sms.LOCKED, booleanCell());
		projectionMap.put(Sms.ERROR_CODE, intCell());  //Not Supported on all devices
		//projectionMap.put(Sms.META_DATA, hashedStringCell());  Doesn't exist for some reason
		return projectionMap;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {android.Manifest.permission.READ_SMS};
	}

	///////////////////////////////////////////
	// COPIED FROM NON-PUBLIC ANDROID API
	// http://www.google.com/codesearch/p?hl=en#fxuXIzvA0aY/core/java/android/provider/Telephony.java&q=package:android%20%22core/java/android/provider/Telephony.java%22&sa=N&cd=1&ct=rc&l=186

	public interface Sms extends TextBasedSmsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://sms");
		
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
