package edu.mit.media.hd.funf.probe.builtin;

import android.os.Bundle;

public class ImagesProbeTest extends ProbeTestCase<ImagesProbe> {

	public ImagesProbeTest() {
		super(ImagesProbe.class);
	}
	
	public void testData() {
		Bundle params = new Bundle();
		startProbe(params);
		Bundle data = getData(10);
		assertNotNull(data.get(ImagesProbe.IMAGES));
	}

}
