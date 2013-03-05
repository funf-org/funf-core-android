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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.time.TimeUtil;

public interface Schedule {
	
	public boolean isOpportunistic();
	public boolean isStrict();

	public BigDecimal getInterval();
	
	/**
	 * Use the previous time to figure out the next time to run.
	 * @param previousTime
	 * @return
	 */
	
	public BigDecimal getDuration();
	

	public BigDecimal getNextTime(Number previousTime);
	
	
	public class BasicSchedule implements Schedule {
		
	    @Configurable
		private BigDecimal interval = null;
	    
	    @Configurable
		private BigDecimal duration = null;
	    
	    @Configurable
		private boolean opportunistic = true;
	    
	    @Configurable
		private boolean strict = false;
		
		public BasicSchedule() {
		}
		
		public BasicSchedule(Schedule schedule) {
			interval = schedule.getInterval();
			duration = schedule.getDuration();
			opportunistic = schedule.isOpportunistic();
			strict = schedule.isStrict();
		}
		
		public BasicSchedule(BigDecimal interval, BigDecimal duration, boolean opportunistic, boolean strict) {
			this.interval = interval;
			this.duration = duration;
			this.opportunistic = opportunistic;
			this.strict = strict;
		}
		
		public BigDecimal getNextTime(Number previousTime) {
			if (interval == null) {
				return null;
			} else if (previousTime == null) {
				return TimeUtil.getTimestamp();
			} else {
				return DecimalTimeUnit.decimal(previousTime).add(interval);
			}
		}
		
		
		
		public void setInterval(BigDecimal interval) {
			this.interval = interval;
		}

		public void setDuration(BigDecimal duration) {
			this.duration = duration;
		}

		public void setOpportunistic(boolean opportunistic) {
			this.opportunistic = opportunistic;
		}

		public void setStrict(boolean strict) {
			this.strict = strict;
		}

		public BigDecimal getInterval() {
			return interval;
		}
		
		public BigDecimal getDuration() {
			return duration;
		}

		public boolean isOpportunistic() {
			return opportunistic;
		}

		public boolean isStrict() {
			return strict;
		}
		
	}

	@Documented
	@Retention(RUNTIME)
	@Target({TYPE,FIELD})
	@Inherited
	public @interface DefaultSchedule {
		Class<?> type() default BasicSchedule.class;
		
		String value() default "";
		
		double interval() default Probe.DEFAULT_PERIOD;
	
		double duration() default Probe.ContinuousProbe.DEFAULT_DURATION;
	
		boolean opportunistic() default Probe.DEFAULT_OPPORTUNISTIC;
	
		boolean strict() default Probe.DEFAULT_STRICT;
	}
}
