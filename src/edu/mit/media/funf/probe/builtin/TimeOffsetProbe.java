package edu.mit.media.funf.probe.builtin;

import static edu.mit.media.funf.Utils.TAG;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TimeOffsetKeys;
import edu.mit.media.funf.time.NtpMessage;

/**
 * Broadcasts the current system time offset from time on major NTP servers in seconds,
 * precise to the Microsecond (though not necessarily that accurate).
 * @author alangardner
 *
 */
public class TimeOffsetProbe extends Base implements TimeOffsetKeys {
	
	private static final BigDecimal SECONDS_1900_TO_1970 = new BigDecimal(2208988800L);
	
	@Configurable
	private String host = "2.north-america.pool.ntp.org";
	
	@Configurable
	private int port = 123;
	
	@Configurable
	private BigDecimal timeout = BigDecimal.TEN;
	
	@Override
	protected void onStart() {
		super.onStart();
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout((int)Utils.secondsToMillis(timeout == null ? BigDecimal.TEN : timeout));
			
			
			InetAddress address = InetAddress.getByName(host);
			
			byte[] buf = new NtpMessage().toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
			
			// Get timestamp right before sending
			BigDecimal timestamp = Utils.getTimestamp();
			NtpMessage.encodeTimestamp(sendPacket.getData(), 40, SECONDS_1900_TO_1970.add(timestamp).doubleValue());
			socket.send(sendPacket);
			
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
			socket.receive(receivePacket);
			
			// Immediately record the incoming timestamp
			double destinationTimestamp = SECONDS_1900_TO_1970.add(Utils.getTimestamp()).doubleValue();
			
			NtpMessage msg = new NtpMessage(receivePacket.getData());
			
			// Corrected, according to RFC2030 errata
			double roundTripDelay = (destinationTimestamp-msg.originateTimestamp) -
				(msg.transmitTimestamp-msg.receiveTimestamp);
				
			double localClockOffset =
				((msg.receiveTimestamp - msg.originateTimestamp) +
				(msg.transmitTimestamp - destinationTimestamp)) / 2;
			
			socket.close();

			JsonObject data = new JsonObject();
			data.addProperty(TIMESTAMP, timestamp);
			data.addProperty(ROUND_TRIP_DELAY, Utils.roundToMilliPrecision(new BigDecimal(roundTripDelay)));
			data.addProperty(LOCAL_TIME_OFFSET, Utils.roundToMilliPrecision(new BigDecimal(localClockOffset)));
			sendData(data);
		} catch (UnknownHostException e) {
			Log.e(TAG, "TimeOffsetProbe: Unknown host", e);
		} catch (IOException e) {
			Log.e(TAG, "TimeOffsetProbe: IOError", e);
		}
		
		stop();
	}
	
	
}
