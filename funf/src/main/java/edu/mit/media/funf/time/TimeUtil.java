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
package edu.mit.media.funf.time;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TimeUtil {

	private static final BigDecimal TWO = new BigDecimal(2);
	
	public static final int
		NANO = 9,
		MICRO = 6,
		MILLI = 3;
	
	/**
	 * Returns a BigDecimal timestamp in seconds with millisecond precision, using System.currentTimeMillis()
	 * @return
	 */
	public static BigDecimal getTimestamp() {
		return DecimalTimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
	}

	/**
	 * Returns a BigDecimal timestamp in seconds with microsecond precision,
	 * using System.nanoTime() and uptimeNanosToTimestamp()
	 * @return
	 */
	public static BigDecimal getTimestampWithMicroPrecision() {
		return TimeUtil.uptimeNanosToTimestamp(System.nanoTime());
	}

	public static long secondsToMillis(Number seconds) {
		return DecimalTimeUnit.SECONDS.toMillis(seconds).longValue();
	}

	/**
	 * Aligns the nano seconds to the start of a new millisecond.
	 * This should be called whenever device wakes up from sleep.
	 */
	public static void calibrateNanosConversion() {
		long originalMillis = System.currentTimeMillis();
		long updatedMillis = originalMillis;
		while(originalMillis == updatedMillis) {
			updatedMillis = System.currentTimeMillis();
		}
		TimeUtil.referenceNanos = System.nanoTime();
		TimeUtil.referenceMillis = updatedMillis;
		TimeUtil.secondsOffset = BigDecimal.valueOf(TimeUtil.referenceMillis, MILLI).subtract(BigDecimal.valueOf(TimeUtil.referenceNanos, NANO));
	}
	
	public static BigDecimal roundToMilliPrecision(BigDecimal timestamp) {
		return timestamp.setScale(MILLI, RoundingMode.HALF_EVEN);
	}

	public static BigDecimal roundToMicroPrecision(BigDecimal timestamp) {
		return timestamp.setScale(MICRO, RoundingMode.HALF_EVEN);
	}

	/**
	 * Converts uptime nanos to a real UTC timestamp in seconds.
	 * @param nanos
	 * @return
	 */
	public static BigDecimal uptimeNanosToTimestamp(long nanos) {
		if (TimeUtil.secondsOffset == null) {
			calibrateNanosConversion();
		} else {
			long currentMillis1 = System.currentTimeMillis();
			long currentNanos = System.nanoTime();
			long currentMillis2 = System.currentTimeMillis();
			BigDecimal currentTimeStamp = DecimalTimeUnit.MILLISECONDS.toSeconds(((double)(currentMillis1 + currentMillis2))/2.0);
			if (TimeUtil._uptimeNanosToTimestamp(currentNanos).subtract(currentTimeStamp).abs().doubleValue() > TimeUtil.CLOCK_OFFSET_TOLERANCE) {
				calibrateNanosConversion();
			}
		}
		return TimeUtil._uptimeNanosToTimestamp(nanos);
	}

	// Round to microseconds, because of the inaccuracy associated with our method of syncing the clocks
	public static BigDecimal _uptimeNanosToTimestamp(long nanos) {
		return roundToMicroPrecision(BigDecimal.valueOf(nanos, NANO).add(TimeUtil.secondsOffset));
	}

	public static final double CLOCK_OFFSET_TOLERANCE = 0.002;
	public static BigDecimal secondsOffset;
	public static long referenceMillis;
	public static long referenceNanos;

}
