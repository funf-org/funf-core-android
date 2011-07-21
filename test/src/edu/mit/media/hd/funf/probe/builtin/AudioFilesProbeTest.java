package edu.mit.media.hd.funf.probe.builtin;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore.Audio;
import edu.mit.media.hd.funf.probe.Probe.SystemParameter;

public class AudioFilesProbeTest extends ProbeTestCase<AudioFilesProbe> {

	public AudioFilesProbeTest() {
		super(AudioFilesProbe.class);
	}
	
	public void testData() {
		Bundle params = new Bundle();
		params.putLong(SystemParameter.PERIOD.name, 0L);
		startProbe(params);
		Bundle data = getData(10);
		ArrayList<Parcelable> audioFiles = data.getParcelableArrayList(AudioFilesProbe.AUDIO_FILES);
		assertNotNull(audioFiles);
		assertTrue(audioFiles.size() > 0);
		
		// Running again should return an empty result
		startProbe(params);
		data = getData(10);
		audioFiles = data.getParcelableArrayList(AudioFilesProbe.AUDIO_FILES);
		assertNotNull(audioFiles);
		assertTrue(audioFiles.isEmpty());
	}

}
