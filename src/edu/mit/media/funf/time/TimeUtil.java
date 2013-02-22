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
