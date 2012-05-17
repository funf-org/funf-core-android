package edu.mit.media.funf;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
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
		
		private BigDecimal interval = null;
		private BigDecimal duration = null;
		private boolean opportunistic = true;
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
