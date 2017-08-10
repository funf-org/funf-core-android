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
