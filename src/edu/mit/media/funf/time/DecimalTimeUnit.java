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

/**
 * A precision lossless implementation of TimeUnit
 * @author alangardner
 *
 */
public enum DecimalTimeUnit {
	NANOSECONDS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toNanos(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return decimal(duration); }
		@Override public BigDecimal toMicros(Number duration) { return decimal(duration).scaleByPowerOfTen(NANO - MICRO); }
		@Override public BigDecimal toMillis(Number duration) { return decimal(duration).scaleByPowerOfTen(NANO - MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).scaleByPowerOfTen(NANO); }
		@Override public BigDecimal toMinutes(Number duration) { return toSeconds(duration).divide(SECONDS_IN_MINUTE); }
		@Override public BigDecimal toHours(Number duration) { return toSeconds(duration).divide(SECONDS_IN_HOUR); }
		@Override public BigDecimal toDays(Number duration) { return toSeconds(duration).divide(SECONDS_IN_DAY); }
	},
	MICROSECONDS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toMicros(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return decimal(duration).scaleByPowerOfTen(MICRO - NANO); }
		@Override public BigDecimal toMicros(Number duration) { return decimal(duration); }
		@Override public BigDecimal toMillis(Number duration) { return decimal(duration).scaleByPowerOfTen(MICRO - MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).scaleByPowerOfTen(MICRO); }
		@Override public BigDecimal toMinutes(Number duration) { return toSeconds(duration).divide(SECONDS_IN_MINUTE); }
		@Override public BigDecimal toHours(Number duration) { return toSeconds(duration).divide(SECONDS_IN_HOUR); }
		@Override public BigDecimal toDays(Number duration) { return toSeconds(duration).divide(SECONDS_IN_DAY); }
	},
	MILLISECONDS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toMillis(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return decimal(duration).scaleByPowerOfTen(MILLI - NANO); }
		@Override public BigDecimal toMicros(Number duration) { return decimal(duration).scaleByPowerOfTen(MILLI - MICRO); }
		@Override public BigDecimal toMillis(Number duration) { return decimal(duration); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).scaleByPowerOfTen(MILLI); }
		@Override public BigDecimal toMinutes(Number duration) { return toSeconds(duration).divide(SECONDS_IN_MINUTE); }
		@Override public BigDecimal toHours(Number duration) { return toSeconds(duration).divide(SECONDS_IN_HOUR); }
		@Override public BigDecimal toDays(Number duration) { return toSeconds(duration).divide(SECONDS_IN_DAY); }
	},
	SECONDS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toSeconds(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return decimal(duration).scaleByPowerOfTen(-NANO); }
		@Override public BigDecimal toMicros(Number duration) { return decimal(duration).scaleByPowerOfTen(-MICRO); }
		@Override public BigDecimal toMillis(Number duration) {return decimal(duration).scaleByPowerOfTen(-MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration); }
		@Override public BigDecimal toMinutes(Number duration) { return decimal(duration).divide(SECONDS_IN_MINUTE); }
		@Override public BigDecimal toHours(Number duration) { return decimal(duration).divide(SECONDS_IN_HOUR); }
		@Override public BigDecimal toDays(Number duration) { return decimal(duration).divide(SECONDS_IN_DAY); }
	},
	MINUTES {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toMinutes(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-NANO); }
		@Override public BigDecimal toMicros(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-MICRO); }
		@Override public BigDecimal toMillis(Number duration) {return toSeconds(duration).scaleByPowerOfTen(-MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).multiply(SECONDS_IN_MINUTE); }
		@Override public BigDecimal toMinutes(Number duration) { return decimal(duration); }
		@Override public BigDecimal toHours(Number duration) { return decimal(duration).divide(MINUTES_IN_HOUR); }
		@Override public BigDecimal toDays(Number duration) { return decimal(duration).divide(MINUTES_IN_DAY); }
	},
	HOURS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toHours(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-NANO); }
		@Override public BigDecimal toMicros(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-MICRO); }
		@Override public BigDecimal toMillis(Number duration) {return toSeconds(duration).scaleByPowerOfTen(-MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).multiply(SECONDS_IN_HOUR); }
		@Override public BigDecimal toMinutes(Number duration) { return decimal(duration).multiply(MINUTES_IN_HOUR); }
		@Override public BigDecimal toHours(Number duration) { return decimal(duration); }
		@Override public BigDecimal toDays(Number duration) { return decimal(duration).divide(HOURS_IN_DAY); }
	},
	DAYS {
		@Override public BigDecimal convert(Number sourceDuration, DecimalTimeUnit u) { return u.toDays(sourceDuration); }
		@Override public BigDecimal toNanos(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-NANO); }
		@Override public BigDecimal toMicros(Number duration) { return toSeconds(duration).scaleByPowerOfTen(-MICRO); }
		@Override public BigDecimal toMillis(Number duration) {return toSeconds(duration).scaleByPowerOfTen(-MILLI); }
		@Override public BigDecimal toSeconds(Number duration) { return decimal(duration).multiply(SECONDS_IN_DAY); }
		@Override public BigDecimal toMinutes(Number duration) { return decimal(duration).multiply(MINUTES_IN_DAY); }
		@Override public BigDecimal toHours(Number duration) { return decimal(duration).multiply(HOURS_IN_DAY); }
		@Override public BigDecimal toDays(Number duration) { return decimal(duration); }
	};
	
	
	
	/**
     * Convert the given time duration in the given unit to this
     * unit.
     *
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * <tt>TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)</tt>
     *
     * @param sourceDuration the time duration in the given <tt>sourceUnit</tt>
     * @param sourceUnit the unit of the <tt>sourceDuration</tt> argument
     * @return the converted duration in this unit
     */
    public abstract BigDecimal convert(Number sourceDuration, DecimalTimeUnit sourceUnit);

    /**
     * Equivalent to <tt>NANOSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toNanos(Number duration);

    /**
     * Equivalent to <tt>MICROSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toMicros(Number duration);

    /**
     * Equivalent to <tt>MILLISECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toMillis(Number duration);

    /**
     * Equivalent to <tt>SECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toSeconds(Number duration);

    /**
     * Equivalent to <tt>MINUTES.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toMinutes(Number duration);

    /**
     * Equivalent to <tt>HOURS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toHours(Number duration);

    /**
     * Equivalent to <tt>DAYS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     */
    public abstract BigDecimal toDays(Number duration);
    
    public static final int 
    	NANO = -9,
    	MICRO = -6,
    	MILLI = -3;
    public static final BigDecimal
    	SECONDS_IN_MINUTE = new BigDecimal(60),
    	MINUTES_IN_HOUR = new BigDecimal(60),
    	HOURS_IN_DAY = new BigDecimal(24),
    	SECONDS_IN_HOUR = SECONDS_IN_MINUTE.multiply(MINUTES_IN_HOUR),
    	SECONDS_IN_DAY = SECONDS_IN_HOUR.multiply(HOURS_IN_DAY),
    	MINUTES_IN_DAY = MINUTES_IN_HOUR.multiply(HOURS_IN_DAY);
    
    public static BigDecimal decimal(Number number) {
    	return (number instanceof BigDecimal) ? (BigDecimal)number : BigDecimal.valueOf(number.doubleValue());
    }
}
