package edu.mit.media.funf.probe2;
/**
 * Defines the required methods necessary for any probe.  All probes must implement this interface to 
 * be used by the Funf system.
 * 
 * Most probes will extend Probe, which implements these methods, but some special case probes may 
 * need to define their own threading structure and state machine.
 */
public interface ProbeRunnable {

	public void addDataListener(Probe.DataListener listener);
	public void removeDataListener(Probe.DataListener listener);
	public void addStatusListener(Probe.StatusListener listener);
	public void removeStatusListener(Probe.StatusListener listener);
	
	public void enable();
	public void start();
	public void stop();
	public void disable();
	
}
