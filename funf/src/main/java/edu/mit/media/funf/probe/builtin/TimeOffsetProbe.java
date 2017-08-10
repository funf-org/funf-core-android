/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.media.funf.probe.builtin;



import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TimeOffsetKeys;
import edu.mit.media.funf.time.NtpMessage;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

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
			socket.setSoTimeout((int)TimeUtil.secondsToMillis(timeout == null ? BigDecimal.TEN : timeout));
			
			
			InetAddress address = InetAddress.getByName(host);
			
			byte[] buf = new NtpMessage().toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
			
			// Get timestamp right before sending
			BigDecimal timestamp = TimeUtil.getTimestamp();
			NtpMessage.encodeTimestamp(sendPacket.getData(), 40, SECONDS_1900_TO_1970.add(timestamp).doubleValue());
			socket.send(sendPacket);
			
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
			socket.receive(receivePacket);
			
			// Immediately record the incoming timestamp
			double destinationTimestamp = SECONDS_1900_TO_1970.add(TimeUtil.getTimestamp()).doubleValue();
			
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
			data.addProperty(ROUND_TRIP_DELAY, TimeUtil.roundToMilliPrecision(new BigDecimal(roundTripDelay)));
			data.addProperty(LOCAL_TIME_OFFSET, TimeUtil.roundToMilliPrecision(new BigDecimal(localClockOffset)));
			sendData(data);
		} catch (UnknownHostException e) {
			Log.e(LogUtil.TAG, "TimeOffsetProbe: Unknown host", e);
		} catch (IOException e) {
			Log.e(LogUtil.TAG, "TimeOffsetProbe: IOError", e);
		}
		
		stop();
	}
	
	
}
