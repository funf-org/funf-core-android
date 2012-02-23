package edu.mit.media.funf.probe2;

import edu.mit.media.funf.probe2.Probe.Base;
import edu.mit.media.funf.probe2.Probe.ContinuousProbe;
import edu.mit.media.funf.probe2.Probe.DefaultConfig;
import edu.mit.media.funf.probe2.Probe.DefaultSchedule;
import edu.mit.media.funf.probe2.Probe.Description;
import edu.mit.media.funf.probe2.Probe.DisplayName;
import edu.mit.media.funf.probe2.Probe.RequiredFeatures;
import edu.mit.media.funf.probe2.Probe.RequiredPermissions;

@DisplayName("Cool awesome probe")
@Description("This probe collects some awesome stuff.  You should definitely enable it.")
@RequiredFeatures({"android.hardware.bluetooth"})
@RequiredPermissions({android.Manifest.permission.ACCESS_CHECKIN_PROPERTIES})
@DefaultSchedule("{\"PERIOD\": 100}")
@DefaultConfig("{}")
public class TestProbe extends Base implements ContinuousProbe {

}
