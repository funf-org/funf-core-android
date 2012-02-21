package edu.mit.media.funf.probe2;

public interface ProbeRunner {
	
	public void enable(ProbeRunnable probe);
	public void start(ProbeRunnable probe);
	public void stop(ProbeRunnable probe);
	public void disable(ProbeRunnable probe);
	
}
