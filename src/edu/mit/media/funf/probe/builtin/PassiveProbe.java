package edu.mit.media.funf.probe.builtin;

import edu.mit.media.funf.probe.Probe.Base;

public class PassiveProbe extends Base {

	@Override
	public void registerListener(DataListener... listeners) {
		registerPassiveListener(listeners);
	}

	@Override
	public void unregisterListener(DataListener... listeners) {
		unregisterPassiveListener(listeners);
	}

	
	
}
