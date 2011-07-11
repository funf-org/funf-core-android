package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcelable;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class VideosProbeTest extends ProbeTestCase<VideosProbe> {

	public VideosProbeTest() {
		super(VideosProbe.class);
	}
	
	public void testData() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(10);
		ArrayList<Parcelable> videos = data.getParcelableArrayList(VideosProbe.VIDEOS);
		assertNotNull(videos);
		assertTrue(videos.size() > 0);
		
		// Running again should return an empty result
		startProbe(params);
		data = getData(10);
		videos = data.getParcelableArrayList(VideosProbe.VIDEOS);
		assertNotNull(videos);
		assertTrue(videos.isEmpty());
	}

}
