package edu.mit.media.funf.tests;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.test.AndroidTestCase;

public class SensorTest extends AndroidTestCase {

	private long lastNanos = 0L;
	private long lastNanosSum = 0L;
	private long count = 0L;
	
	public void testSensorSpeed() throws InterruptedException {
		for (int i = 0; i< 1000; i++) {
			// calibrateNanosConversion();
			System.out.println(millisToSeconds(System.currentTimeMillis()) - uptimeNanosToTimestamp(System.nanoTime()));
		}
		System.out.println("---------------------------");
		
		double lastNano = 0;
		for (int i = 0; i< 20; i++) {
			long nano = System.nanoTime();
			double diff = millisToSeconds(System.currentTimeMillis()) - uptimeNanosToTimestamp(nano);
			System.out.println(diff);
			System.out.println(uptimeNanosToTimestamp(nano) - uptimeNanosToTimestamp(lastNanos));
			lastNanos = nano;
			System.out.println(millisToSeconds(System.currentTimeMillis()) + " " + uptimeNanosToTimestamp(System.nanoTime()));
		}
		SensorManager manager =  (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		calibrateNanosConversion();
		SensorEventListener sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				System.out.println("--------------------");
				long mili = System.currentTimeMillis();
				long nano = System.nanoTime();
				if (lastNanos == 0L) {
					lastNanos = event.timestamp;
				}
				//calibrateNanosConversion();
				System.out.println("Event Seconds: " + uptimeNanosToTimestamp(event.timestamp));
				System.out.println("Nano  Seconds: " + uptimeNanosToTimestamp(nano));
				System.out.println("Diff  Seconds: " + (uptimeNanosToTimestamp(nano) - uptimeNanosToTimestamp(event.timestamp)));
				System.out.println("Acc    Period: " + (uptimeNanosToTimestamp(nano) - uptimeNanosToTimestamp(lastNanos)));
				System.out.println("Mili  Seconds: " + millisToSeconds(mili));
				lastNanosSum += nano - lastNanos;
				count++;
				System.out.println("Avg diff:" + (lastNanosSum / count));
				lastNanos = nano;
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		manager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		Thread.sleep(30);
	}
	
	public static double millisToSeconds(long millis) {
		return ((double)millis)/1000;
	}
	
	public static long secondsToMillis(double seconds) {
		return (long)(seconds*1000);
	}
	
	public static final long NANOS_IN_SECOND = 1000000000; // 10^9
	private static long referenceNanos;
	private static long referenceMillis;
	private static double secondsOffset;
	
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
		referenceNanos = System.nanoTime();
		referenceMillis = updatedMillis;
		secondsOffset = millisToSeconds(referenceMillis) - (double)referenceNanos / (double)NANOS_IN_SECOND;
	}
	
	public static double uptimeNanosToTimestamp(long nanos) {
		long currentMillisAccordingToNanos = secondsToMillis(_uptimeNanosToTimestamp(System.nanoTime()));
		if (Math.abs(currentMillisAccordingToNanos - System.currentTimeMillis()) > 1) {
			calibrateNanosConversion();
		}
		return _uptimeNanosToTimestamp(nanos);
	}
	
	private static double _uptimeNanosToTimestamp(long nanos) {
		return ((double)nanos / (double)NANOS_IN_SECOND) + secondsOffset;
	}
	
}

